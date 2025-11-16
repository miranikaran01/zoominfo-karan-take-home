package com.zoominfo.karan_take_home.interceptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Interceptor for FasterWhisperClient to log requests and responses,
 * handle errors, and measure request duration.
 */
public class FasterWhisperClientInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(FasterWhisperClientInterceptor.class);
    
    /**
     * Creates an ExchangeFilterFunction that logs request and response details
     * and measures request duration.
     * 
     * @return ExchangeFilterFunction for WebClient
     */
    public static ExchangeFilterFunction logRequestAndResponse() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            if (logger.isDebugEnabled()) {
                logRequest(request);
            }
            return Mono.just(request);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (logger.isDebugEnabled()) {
                logResponse(response);
            }
            return Mono.just(response);
        }));
    }
    
    /**
     * Creates an ExchangeFilterFunction that measures request duration
     * and logs slow requests.
     * 
     * @return ExchangeFilterFunction for WebClient
     */
    public static ExchangeFilterFunction measureRequestDuration() {
        return (request, next) -> {
            Instant start = Instant.now();
            return next.exchange(request)
                .doOnSuccess(response -> {
                    Duration duration = Duration.between(start, Instant.now());
                    if (duration.toMillis() > 5000) {
                        logger.warn("Slow request to {} took {} ms", 
                            request.url(), duration.toMillis());
                    } else if (logger.isDebugEnabled()) {
                        logger.debug("Request to {} completed in {} ms", 
                            request.url(), duration.toMillis());
                    }
                })
                .doOnError(error -> {
                    Duration duration = Duration.between(start, Instant.now());
                    logger.error("Request to {} failed after {} ms: {}", 
                        request.url(), duration.toMillis(), error.getMessage());
                });
        };
    }
    
    /**
     * Creates an ExchangeFilterFunction that handles HTTP error responses
     * and logs them appropriately.
     * 
     * @return ExchangeFilterFunction for WebClient
     */
    public static ExchangeFilterFunction handleErrorResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                String reasonPhrase = response.statusCode() instanceof HttpStatus 
                    ? ((HttpStatus) response.statusCode()).getReasonPhrase()
                    : "";
                logger.error("Error response from FasterWhisper API: {} {}", 
                    response.statusCode().value(), reasonPhrase);
            }
            return Mono.just(response);
        });
    }
    
    /**
     * Combines all interceptors into a single filter function.
     * 
     * @return Combined ExchangeFilterFunction
     */
    public static ExchangeFilterFunction all() {
        return logRequestAndResponse()
            .andThen(measureRequestDuration())
            .andThen(handleErrorResponse());
    }
    
    private static void logRequest(ClientRequest request) {
        logger.debug("Request: {} {}", request.method(), request.url());
        logger.debug("Request headers: {}", request.headers());
    }
    
    private static void logResponse(ClientResponse response) {
        String reasonPhrase = response.statusCode() instanceof HttpStatus 
            ? ((HttpStatus) response.statusCode()).getReasonPhrase()
            : "";
        logger.debug("Response status: {} {}", 
            response.statusCode().value(), reasonPhrase);
        logger.debug("Response headers: {}", response.headers().asHttpHeaders());
    }
}
