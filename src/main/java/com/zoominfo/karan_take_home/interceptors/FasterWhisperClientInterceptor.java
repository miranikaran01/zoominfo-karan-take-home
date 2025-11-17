package com.zoominfo.karan_take_home.interceptors;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import reactor.core.publisher.Mono;

/**
 * Interceptor for FasterWhisperClient to log requests and responses,
 * handle errors, and measure request duration.
 */
public class FasterWhisperClientInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(FasterWhisperClientInterceptor.class);
    
    /**
     * Creates an ExchangeFilterFunction that logs request and response details.
     * For streaming responses, only logs request (not response) to avoid interfering with the stream.
     * 
     * @return ExchangeFilterFunction for WebClient
     */
    public static ExchangeFilterFunction logRequestAndResponse() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            if (logger.isDebugEnabled()) {
                logRequest(request);
            }
            return Mono.just(request);
        }).andThen((request, next) -> {
            return next.exchange(request)
                .doOnNext(response -> {
                    if (logger.isDebugEnabled()) {
                        boolean isStreaming = response.headers().contentType()
                            .map(ct -> ct.toString().contains("text/event-stream") || 
                                       ct.toString().contains("application/stream"))
                            .orElse(false);
                        
                        if (!isStreaming) {
                            logResponse(response);
                        } else {
                            logger.debug("Streaming response status: {}", response.statusCode().value());
                        }
                    }
                });
        });
    }
    
    /**
     * Creates an ExchangeFilterFunction that measures request duration
     * and logs slow requests.
     * 
     * For streaming responses (SSE), this does not interfere with the stream.
     * Duration logging for streaming happens when the connection is established,
     * not when the stream completes, to avoid consuming the response body.
     * 
     * @return ExchangeFilterFunction for WebClient
     */
    public static ExchangeFilterFunction measureRequestDuration() {
        return (request, next) -> {
            Instant start = Instant.now();
            return next.exchange(request)
                .doOnNext(response -> {
                    boolean isStreaming = response.headers().contentType()
                        .map(ct -> ct.toString().contains("text/event-stream") || 
                                   ct.toString().contains("application/stream"))
                        .orElse(false);
                    
                    if (isStreaming) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Streaming connection established to {}", request.url());
                        }
                    } else {
                        Duration duration = Duration.between(start, Instant.now());
                        if (duration.toMillis() > 5000) {
                            logger.warn("Slow request to {} took {} ms", 
                                request.url(), duration.toMillis());
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("Request to {} completed in {} ms", 
                                request.url(), duration.toMillis());
                        }
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
        return (request, next) -> {
            return next.exchange(request)
                .doOnNext(response -> {
                    if (response.statusCode().isError()) {
                        String reasonPhrase = response.statusCode() instanceof HttpStatus 
                            ? ((HttpStatus) response.statusCode()).getReasonPhrase()
                            : "";
                        logger.error("Error response from FasterWhisper API: {} {}", 
                            response.statusCode().value(), reasonPhrase);
                    }
                });
        };
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
