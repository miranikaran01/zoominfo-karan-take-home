package com.zoominfo.karan_take_home.services;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockMultipartFile;

import com.zoominfo.karan_take_home.SpeechToTextRequest;
import com.zoominfo.karan_take_home.clients.FasterWhisperClient;
import com.zoominfo.karan_take_home.dto.incoming.FasterWhisperResponseDto;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SpeechToTextServiceTest {

    @Mock
    private FasterWhisperClient fasterWhisperClient;

    @InjectMocks
    private SpeechToTextService speechToTextService;

    private MockMultipartFile mockFile;
    private SpeechToTextRequest request;

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile(
            "file",
            "test-audio.wav",
            "audio/wav",
            "test audio content".getBytes()
        );
        request = SpeechToTextRequest.builder()
            .file(mockFile)
            .language("en")
            .model("Systran/faster-whisper-small")
            .stream(true)
            .build();
    }

    @Test
    void testTranscribeWithSingleResponse() {
        // Arrange
        FasterWhisperResponseDto dto = new FasterWhisperResponseDto("Hello, world!");
        ServerSentEvent<FasterWhisperResponseDto> event = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(true)
        )).thenReturn(Flux.just(event));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(request))
            .expectNextMatches(response -> 
                response.text().equals("Hello, world!")
            )
            .verifyComplete();

        verify(fasterWhisperClient, times(1)).transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(true)
        );
    }

    @Test
    void testTranscribeWithMultipleResponses() {
        // Arrange
        FasterWhisperResponseDto dto1 = new FasterWhisperResponseDto("Hello");
        FasterWhisperResponseDto dto2 = new FasterWhisperResponseDto(", ");
        FasterWhisperResponseDto dto3 = new FasterWhisperResponseDto("world!");

        ServerSentEvent<FasterWhisperResponseDto> event1 = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto1)
            .build();
        ServerSentEvent<FasterWhisperResponseDto> event2 = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto2)
            .build();
        ServerSentEvent<FasterWhisperResponseDto> event3 = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto3)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(true)
        )).thenReturn(Flux.just(event1, event2, event3));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(request))
            .expectNextMatches(response -> response.text().equals("Hello"))
            .expectNextMatches(response -> response.text().equals(", "))
            .expectNextMatches(response -> response.text().equals("world!"))
            .verifyComplete();
    }

    @Test
    void testTranscribeFiltersNullDataEvents() {
        // Arrange
        FasterWhisperResponseDto dto1 = new FasterWhisperResponseDto("Hello");
        ServerSentEvent<FasterWhisperResponseDto> event1 = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto1)
            .build();
        ServerSentEvent<FasterWhisperResponseDto> event2 = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(null) // Null data should be filtered
            .build();
        FasterWhisperResponseDto dto3 = new FasterWhisperResponseDto("world!");
        ServerSentEvent<FasterWhisperResponseDto> event3 = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto3)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(true)
        )).thenReturn(Flux.just(event1, event2, event3));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(request))
            .expectNextMatches(response -> response.text().equals("Hello"))
            .expectNextMatches(response -> response.text().equals("world!"))
            .verifyComplete();
    }

    @Test
    void testTranscribeWithEmptyResponse() {
        // Arrange
        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(true)
        )).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(request))
            .verifyComplete();
    }

    @Test
    void testTranscribeWithAllNullDataEvents() {
        // Arrange
        ServerSentEvent<FasterWhisperResponseDto> event1 = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(null)
            .build();
        ServerSentEvent<FasterWhisperResponseDto> event2 = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(null)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(true)
        )).thenReturn(Flux.just(event1, event2));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(request))
            .verifyComplete();
    }

    @Test
    void testTranscribeWithDefaultRequestValues() {
        // Arrange
        SpeechToTextRequest defaultRequest = SpeechToTextRequest.builder()
            .file(mockFile)
            .build(); // Uses defaults: language="en", model="Systran/faster-whisper-small", stream=false

        FasterWhisperResponseDto dto = new FasterWhisperResponseDto("Test transcription");
        ServerSentEvent<FasterWhisperResponseDto> event = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(false)
        )).thenReturn(Flux.just(event));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(defaultRequest))
            .expectNextMatches(response -> response.text().equals("Test transcription"))
            .verifyComplete();

        verify(fasterWhisperClient, times(1)).transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(false)
        );
    }

    @Test
    void testTranscribeWithDifferentLanguage() {
        // Arrange
        SpeechToTextRequest spanishRequest = SpeechToTextRequest.builder()
            .file(mockFile)
            .language("es")
            .model("Systran/faster-whisper-small")
            .stream(false)
            .build();

        FasterWhisperResponseDto dto = new FasterWhisperResponseDto("Hola, mundo!");
        ServerSentEvent<FasterWhisperResponseDto> event = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("es"),
            eq("Systran/faster-whisper-small"),
            eq(false)
        )).thenReturn(Flux.just(event));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(spanishRequest))
            .expectNextMatches(response -> response.text().equals("Hola, mundo!"))
            .verifyComplete();

        verify(fasterWhisperClient, times(1)).transcribe(
            eq(mockFile),
            eq("es"),
            eq("Systran/faster-whisper-small"),
            eq(false)
        );
    }

    @Test
    void testTranscribeWithDifferentModel() {
        // Arrange
        SpeechToTextRequest largeModelRequest = SpeechToTextRequest.builder()
            .file(mockFile)
            .language("en")
            .model("Systran/faster-whisper-large-v3")
            .stream(true)
            .build();

        FasterWhisperResponseDto dto = new FasterWhisperResponseDto("High quality transcription");
        ServerSentEvent<FasterWhisperResponseDto> event = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-large-v3"),
            eq(true)
        )).thenReturn(Flux.just(event));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(largeModelRequest))
            .expectNextMatches(response -> response.text().equals("High quality transcription"))
            .verifyComplete();

        verify(fasterWhisperClient, times(1)).transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-large-v3"),
            eq(true)
        );
    }

    @Test
    void testTranscribeWithLongText() {
        // Arrange
        String longText = "This is a very long transcription text that contains multiple sentences. " +
            "It should be properly handled by the service. " +
            "The text can span multiple lines and contain various punctuation marks! " +
            "Does it work correctly? Yes, it does.";

        FasterWhisperResponseDto dto = new FasterWhisperResponseDto(longText);
        ServerSentEvent<FasterWhisperResponseDto> event = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(true)
        )).thenReturn(Flux.just(event));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(request))
            .expectNextMatches(response -> response.text().equals(longText))
            .verifyComplete();
    }

    @Test
    void testTranscribeWithSpecialCharacters() {
        // Arrange
        String textWithSpecialChars = "Hello! @#$%^&*()_+-=[]{}|;':\",./<>?";
        FasterWhisperResponseDto dto = new FasterWhisperResponseDto(textWithSpecialChars);
        ServerSentEvent<FasterWhisperResponseDto> event = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(true)
        )).thenReturn(Flux.just(event));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(request))
            .expectNextMatches(response -> response.text().equals(textWithSpecialChars))
            .verifyComplete();
    }

    @Test
    void testTranscribeWithUnicodeCharacters() {
        // Arrange
        String unicodeText = "Hello 世界! Здравствуй мир! مرحبا بالعالم";
        FasterWhisperResponseDto dto = new FasterWhisperResponseDto(unicodeText);
        ServerSentEvent<FasterWhisperResponseDto> event = ServerSentEvent.<FasterWhisperResponseDto>builder()
            .data(dto)
            .build();

        when(fasterWhisperClient.transcribe(
            eq(mockFile),
            eq("en"),
            eq("Systran/faster-whisper-small"),
            eq(true)
        )).thenReturn(Flux.just(event));

        // Act & Assert
        StepVerifier.create(speechToTextService.transcribe(request))
            .expectNextMatches(response -> response.text().equals(unicodeText))
            .verifyComplete();
    }
}
