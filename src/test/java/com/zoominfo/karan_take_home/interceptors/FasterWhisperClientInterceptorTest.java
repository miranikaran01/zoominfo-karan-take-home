package com.zoominfo.karan_take_home.interceptors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for FasterWhisperClientInterceptor.
 * Tests logging, duration measurement and error handling for both streaming and non-streaming responses.
 */
class FasterWhisperClientInterceptorTest {

    private ExchangeFunction mockExchangeFunction;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        mockExchangeFunction = mock(ExchangeFunction.class);
        
        logger = (Logger) LoggerFactory.getLogger(FasterWhisperClientInterceptor.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.DEBUG);
    }

    @Test
    void testLogRequestAndResponse_NonStreaming_LogsRequestAndResponse() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .header("Content-Type", "multipart/form-data")
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"text\":\"test\"}")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.logRequestAndResponse();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        verify(mockExchangeFunction).exchange(any(ClientRequest.class));
        
        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        
        assertThat(logMessages).anyMatch(msg -> msg.contains("Request: POST http://localhost:8000/v1/audio/transcriptions"));
        assertThat(logMessages).anyMatch(msg -> msg.contains("Request headers:"));
        assertThat(logMessages).anyMatch(msg -> msg.contains("Response status: 200 OK"));
        assertThat(logMessages).anyMatch(msg -> msg.contains("Response headers:"));
    }

    @Test
    void testLogRequestAndResponse_Streaming_LogsRequestOnly() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .header("Content-Type", "multipart/form-data")
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
                .body("data: {\"text\":\"test\"}\n\n")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.logRequestAndResponse();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        verify(mockExchangeFunction).exchange(any(ClientRequest.class));
        
        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        
        assertThat(logMessages).anyMatch(msg -> msg.contains("Request: POST http://localhost:8000/v1/audio/transcriptions"));
        assertThat(logMessages).anyMatch(msg -> msg.contains("Streaming response status: 200"));
        assertThat(logMessages).noneMatch(msg -> msg.contains("Response status: 200 OK") && !msg.contains("Streaming"));
    }

    @Test
    void testLogRequestAndResponse_ApplicationStream_LogsRequestOnly() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/stream+json")
                .body("{\"text\":\"test\"}")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.logRequestAndResponse();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        
        assertThat(logMessages).anyMatch(msg -> msg.contains("Streaming response status: 200"));
    }

    @Test
    void testMeasureRequestDuration_NonStreaming_FastRequest_LogsDebug() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"text\":\"test\"}")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response).delayElement(Duration.ofMillis(100)));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.measureRequestDuration();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        
        assertThat(logMessages).anyMatch(msg -> msg.contains("Request to") && msg.contains("completed in") && msg.contains("ms"));
    }

    @Test
    void testMeasureRequestDuration_NonStreaming_SlowRequest_LogsWarn() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"text\":\"test\"}")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response).delayElement(Duration.ofMillis(6000)));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.measureRequestDuration();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        List<ILoggingEvent> warnLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .collect(Collectors.toList());
        
        assertThat(warnLogs).isNotEmpty();
        assertThat(warnLogs.get(0).getFormattedMessage())
                .contains("Slow request to")
                .contains("took")
                .contains("ms");
    }

    @Test
    void testMeasureRequestDuration_Streaming_LogsConnectionEstablished() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
                .body("data: {\"text\":\"test\"}\n\n")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.measureRequestDuration();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        
        assertThat(logMessages).anyMatch(msg -> msg.contains("Streaming connection established to"));
    }

    @Test
    void testMeasureRequestDuration_Error_LogsErrorWithDuration() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .build();
        
        RuntimeException error = new RuntimeException("Connection failed");
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.<ClientResponse>error(error).delayElement(Duration.ofMillis(200)));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.measureRequestDuration();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        
        List<ILoggingEvent> errorLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .collect(Collectors.toList());
        
        assertThat(errorLogs).isNotEmpty();
        assertThat(errorLogs.get(0).getFormattedMessage())
                .contains("Request to")
                .contains("failed after")
                .contains("ms")
                .contains("Connection failed");
    }

    @Test
    void testHandleErrorResponse_ErrorStatus_LogsError() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"error\":\"Internal Server Error\"}")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.handleErrorResponse();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        List<ILoggingEvent> errorLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .collect(Collectors.toList());
        
        assertThat(errorLogs).isNotEmpty();
        assertThat(errorLogs.get(0).getFormattedMessage())
                .contains("Error response from FasterWhisper API: 500");
    }

    @Test
    void testHandleErrorResponse_SuccessStatus_NoErrorLog() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"text\":\"test\"}")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.handleErrorResponse();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        List<ILoggingEvent> errorLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .collect(Collectors.toList());
        
        assertThat(errorLogs).isEmpty();
    }

    @Test
    void testHandleErrorResponse_NotFoundStatus_LogsError() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"error\":\"Not Found\"}")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.handleErrorResponse();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        List<ILoggingEvent> errorLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .collect(Collectors.toList());
        
        assertThat(errorLogs).isNotEmpty();
        assertThat(errorLogs.get(0).getFormattedMessage())
                .contains("Error response from FasterWhisper API: 404");
    }

    @Test
    void testAll_CombinesAllFilters() {
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost:8000/v1/audio/transcriptions"))
                .header("Content-Type", "multipart/form-data")
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"text\":\"test\"}")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response).delayElement(Duration.ofMillis(100)));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.all();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        verify(mockExchangeFunction).exchange(any(ClientRequest.class));
        
        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        
        assertThat(logMessages).anyMatch(msg -> msg.contains("Request: POST"));
        assertThat(logMessages).anyMatch(msg -> msg.contains("Response status: 200"));
        assertThat(logMessages).anyMatch(msg -> msg.contains("completed in") && msg.contains("ms"));
    }

    @Test
    void testLogRequestAndResponse_NoContentType_LogsFullResponse() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost:8000/health"))
                .build();
        
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .body("OK")
                .build();
        
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(response));
        
        ExchangeFilterFunction filter = FasterWhisperClientInterceptor.logRequestAndResponse();
        
        Mono<ClientResponse> result = filter.filter(request, mockExchangeFunction);
        
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        
        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        
        assertThat(logMessages).anyMatch(msg -> msg.contains("Response status: 200"));
    }
}
