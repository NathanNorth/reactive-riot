package io.github.nathannorth.riot4j.queues;

import io.github.nathannorth.riot4j.util.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class LimitedQueue {
    private static final AtomicInteger count = new AtomicInteger(0);
    private final Sinks.Many<Request> in = Sinks.many().multicast().onBackpressureBuffer(1024, false);

    public Mono<String> push(HttpClient.ResponseReceiver<?> r) {
        Request request = new Request(r); //create a request
        //in.tryEmitNext(request); //emit request
        return outCentral //get the first item in our out flux that matches our request id
                .filter(completed -> completed.id == request.id)
                .next()
                .map(completed -> completed.result).doOnSubscribe(sub -> in.tryEmitNext(request));
    }

    private final Flux<Completed> outCentral = out().cache(0);

    //take everything in our in sink and flatmap it into a completed request. If we get 429 we wait it out and try again
    private Flux<Completed> out() {
        return in.asFlux()
                .flatMap(request -> conversion.apply(request)
                        .onErrorResume(error -> {
                        if(error instanceof Exceptions.RateLimitedException) {
                            System.out.println("Hit rate limit... delaying: " + ((Exceptions.RateLimitedException) error).getSecs() + " seconds");
                            return Mono.delay(Duration.ofSeconds(((Exceptions.RateLimitedException) error).getSecs()))
                                    .flatMap(finished -> conversion.apply(request));
                        }
                        else return Mono.error(error);
                        }), 1);
    }

    //take a given request and make the web request. If the request 429s return a RateLimitException
    private final Function<Request, Mono<Completed>> conversion = request ->
        request.response.responseSingle(((response, byteBufMono) -> {
            if(response.status().code() / 100 == 2)
                return byteBufMono.asString();
            if(response.status().code() == 429)
                return Mono.error(new Exceptions.RateLimitedException(response,
                        Integer.parseInt(response.responseHeaders().get("Retry-After"))
                ));
            else {
                return Mono.error(new Exceptions.WebFailure("Error in web request " + response.status().code(), response));
            }
        })).map(str -> new Completed(request.id, str));

    private static class Request {
        private final HttpClient.ResponseReceiver<?> response;
        private final int id = count.getAndIncrement();
        public Request(HttpClient.ResponseReceiver<?> response) {
            this.response = response;
        }
    }
    private static class Completed {
        private final int id;
        private final String result;

        public Completed(int id, String result) {
            this.id = id;
            this.result = result;
        }
    }
}
