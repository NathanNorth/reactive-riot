package tech.nathann.riot4j.queues.nlimiter;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.PrematureCloseException;
import tech.nathann.riot4j.exceptions.RetryableException;
import tech.nathann.riot4j.queues.FailureStrategies;

import java.nio.charset.StandardCharsets;

public class Request {
    private static final Logger log = LoggerFactory.getLogger(Request.class);

    private final HttpClient.ResponseReceiver<?> httpRequest;
    private final Sinks.One<String> callback = Sinks.one();

    public Request(HttpClient.ResponseReceiver<?> httpRequest) {
        this.httpRequest = httpRequest;
    }

    public Mono<String> getRequest() {
        return httpRequest.responseSingle(((response, byteBufMono) -> {
            if(response.status().code() / 100 == 2) {
                log.debug("Status is " + response.status().code()  + " Method rate limit count: " + response.responseHeaders().get("X-Method-Rate-Limit-Count") + " - App count: " + response.responseHeaders().get("X-App-Rate-Limit-Count"));
                return byteBufMono.asString(StandardCharsets.UTF_8);
            }
            else {
                log.warn("Status is " + response.status().code()  + " Method rate limit count: " + response.responseHeaders().get("X-Method-Rate-Limit-Count") + " - App count: " + response.responseHeaders().get("X-App-Rate-Limit-Count"));
                return byteBufMono.asString(StandardCharsets.UTF_8)
                        .switchIfEmpty(Mono.just(""))
                        .flatMap(data -> Mono.error(FailureStrategies.makeWebException(response, data)));
            }
        })).onErrorResume(error -> {
            if(
                    error instanceof PrematureCloseException ||
                    error instanceof ConnectTimeoutException ||
                    error instanceof ReadTimeoutException
            ) {
                log.warn("Converting Netty error " + error + " to empty RetryableException");
                return Mono.error(new RetryableException(error));
            }
            else return Mono.error(error);
        });
    }

    public Sinks.One<String> getCallback() {
        return callback;
    }
}
