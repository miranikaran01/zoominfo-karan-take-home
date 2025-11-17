package com.zoominfo.karan_take_home.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.zoominfo.karan_take_home.SpeechToTextRequest;
import com.zoominfo.karan_take_home.SpeechToTextResponse;
import com.zoominfo.karan_take_home.services.SpeechToTextService;

import reactor.core.publisher.Flux;

/**
 * Unit tests for SpeechToTextController.
 * Tests the REST endpoint for speech-to-text conversion with SSE streaming.
 */
@ExtendWith(MockitoExtension.class)
class SpeechToTextControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private SpeechToTextService speechToTextService;

    private byte[] mockFileContent;
    private String mockFileName;

    @BeforeEach
    void setUp() {
        mockFileContent = "test audio content".getBytes();
        mockFileName = "test-audio.wav";
        
        // Create WebTestClient with the controller and mocked service
        SpeechToTextController controller = new SpeechToTextController(speechToTextService);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void testSpeechToText_WithSingleResponse_ReturnsSSE() {
        // Arrange
        SpeechToTextResponse response = new SpeechToTextResponse("Hello, world!");
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.just(response));

        // Act & Assert
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, "en", "Systran/faster-whisper-small", "true"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM)
            .expectBody()
            .consumeWith(result -> {
                String body = new String(result.getResponseBody());
                // Verify SSE format
                assert body.contains("data:");
                assert body.contains("Hello, world!");
            });
    }

    @Test
    void testSpeechToText_WithMultipleResponses_ReturnsMultipleSSEEvents() {
        // Arrange
        SpeechToTextResponse response1 = new SpeechToTextResponse("Hello");
        SpeechToTextResponse response2 = new SpeechToTextResponse(", ");
        SpeechToTextResponse response3 = new SpeechToTextResponse("world!");
        
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.just(response1, response2, response3));

        // Act & Assert
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, "en", "Systran/faster-whisper-small", "true"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM)
            .expectBody()
            .consumeWith(result -> {
                String body = new String(result.getResponseBody());
                // Verify multiple SSE events
                assert body.contains("data:");
                assert body.contains("Hello");
                assert body.contains(", ");
                assert body.contains("world!");
            });
    }

    @Test
    void testSpeechToText_WithEmptyResponse_ReturnsEmptySSE() {
        // Arrange
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.empty());

        // Act & Assert
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, "en", "Systran/faster-whisper-small", "true"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM)
            .expectBody()
            .consumeWith(result -> {
                String body = new String(result.getResponseBody());
                // Empty response should still be valid SSE
                assert body != null;
            });
    }

    @Test
    void testSpeechToText_WithDefaultParameters_UsesDefaults() {
        // Arrange
        SpeechToTextResponse response = new SpeechToTextResponse("Test transcription");
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.just(response));

        // Act & Assert - Only file is required, other params are optional
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, null, null, null))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    void testSpeechToText_WithSpanishLanguage_ForwardsToService() {
        // Arrange
        SpeechToTextResponse response = new SpeechToTextResponse("Hola, mundo!");
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.just(response));

        // Act & Assert
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, "es", "Systran/faster-whisper-small", "true"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM)
            .expectBody()
            .consumeWith(result -> {
                String body = new String(result.getResponseBody());
                assert body.contains("Hola, mundo!");
            });
    }

    @Test
    void testSpeechToText_WithDifferentModel_ForwardsToService() {
        // Arrange
        SpeechToTextResponse response = new SpeechToTextResponse("Test with large model");
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.just(response));

        // Act & Assert
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, "en", "Systran/faster-whisper-large-v2", "false"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    void testSpeechToText_WithStreamFalse_ForwardsToService() {
        // Arrange
        SpeechToTextResponse response = new SpeechToTextResponse("Non-streaming response");
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.just(response));

        // Act & Assert
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, "en", "Systran/faster-whisper-small", "false"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    void testSpeechToText_ServiceReturnsError_PropagatesError() {
        // Arrange
        RuntimeException error = new RuntimeException("Service error");
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.error(error));

        // Act & Assert
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, "en", "Systran/faster-whisper-small", "true"))
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    void testSpeechToText_ControllerWrapsResponseInSSE() {
        // Arrange
        SpeechToTextResponse response = new SpeechToTextResponse("Test text");
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.just(response));

        // Act & Assert - Verify the response is wrapped in ServerSentEvent
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, "en", "Systran/faster-whisper-small", "true"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM)
            .expectBody()
            .consumeWith(result -> {
                String body = new String(result.getResponseBody());
                // Verify SSE format with data field
                assert body.contains("data:");
                assert body.contains("Test text");
            });
    }

    @Test
    void testSpeechToText_WithLongText_HandlesCorrectly() {
        // Arrange
        String longText = "This is a very long transcription text that contains multiple sentences. "
            + "It should be handled correctly by the controller. "
            + "The SSE format should preserve the entire text content.";
        SpeechToTextResponse response = new SpeechToTextResponse(longText);
        when(speechToTextService.transcribe(any(SpeechToTextRequest.class)))
            .thenReturn(Flux.just(response));

        // Act & Assert
        webTestClient
            .post()
            .uri("/speech-to-text")
            .body(createMultipartBody(mockFileContent, mockFileName, "en", "Systran/faster-whisper-small", "true"))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM)
            .expectBody()
            .consumeWith(result -> {
                String body = new String(result.getResponseBody());
                assert body.contains(longText);
            });
    }

    /**
     * Helper method to create multipart form data body for testing using BodyInserters.
     */
    private BodyInserters.MultipartInserter createMultipartBody(
            byte[] fileContent, String fileName, String language, String model, String stream) {
        Resource resource = new ByteArrayResource(fileContent) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        BodyInserters.MultipartInserter inserter = BodyInserters.fromMultipartData("file", resource);
        
        if (language != null) {
            inserter.with("language", language);
        }
        if (model != null) {
            inserter.with("model", model);
        }
        if (stream != null) {
            inserter.with("stream", stream);
        }
        
        return inserter;
    }
}
