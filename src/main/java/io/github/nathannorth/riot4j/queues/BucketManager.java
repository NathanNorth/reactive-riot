package io.github.nathannorth.riot4j.queues;

import io.github.nathannorth.riot4j.exceptions.RateLimitedException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;

/**
 * A bucket manager handles all the actual web requests of its owned buckets, and provides a convenience map so outside methods can push to buckets
 */
public class BucketManager {
    //map using enum for organization
    private final HashMap<RateLimits, Bucket> buckets = new HashMap<>();
    private final Sinks.Many<Retryable> in = Sinks.many().multicast().onBackpressureBuffer(1024, true);

    public BucketManager() {
        //use tries till the day we die. We attach two emissions to successful tries
        in.asFlux()
                .flatMap(retryable -> useATry(retryable)
                                //whenever a try makes it out of the method, we emit the result to the output mono and also tell the queue that it can start sending stuff again
                                .doOnNext(result -> retryable.getResultHandle().emitValue(result, Sinks.EmitFailureHandler.FAIL_FAST))
                                .doOnNext(result -> retryable.getBucketHandle().emitValue(true, Sinks.EmitFailureHandler.FAIL_FAST))
                        , 1)
                .subscribe();
    }

    //recursive function to try making http requests
    private Mono<String> useATry(Retryable retryable) {
        return retryable.getTry() //try http request
                .onErrorResume(error -> {
                    //catch rate limits
                    if(error instanceof RateLimitedException) {
                        RateLimitedException rateLimitedError = (RateLimitedException) error;

                        //global rate limit
                        if(!rateLimitedError.isMethod()) {
                            System.out.println("Hit GLOBAL rate limit! Delaying " + rateLimitedError.getSecs() + " seconds...");
                            return Mono.delay(Duration.ofSeconds(rateLimitedError.getSecs()))
                                    .flatMap(finished -> useATry(retryable)); //try again after failure
                        }
                        //method rate limit
                        else {
                            System.out.println("Hit METHOD rate limit! Telling bucket to wait!");
                            retryable.getBucketHandle().emitError(rateLimitedError, Sinks.EmitFailureHandler.FAIL_FAST);
                            return Mono.empty(); //go on with our lives
                        }
                    }
                    return Mono.error(error); //some other error
                });
    }

    //when a bucket pushes to the manager we need to reset the handle it waits on, and then emit to our queue
    protected Mono<Boolean> push(Retryable r) {
        r.setBucketHandle(Sinks.one()); //reset bucket handle todo can sinks have race conditions / backpressure?
        in.emitNext(r, Sinks.EmitFailureHandler.FAIL_FAST);
        return r.getBucketHandle().asMono();
    }

    //public method to access teh manager. Allows a user to push to a bucket given a key enum. Creates buckets as necessary.
    public Mono<String> pushToBucket(RateLimits limit, HttpClient.ResponseReceiver<?> input) {
        Bucket bucket = buckets.computeIfAbsent(limit,
                key -> new Bucket(this, key)
        );
        return bucket.push(input);
    }
}
