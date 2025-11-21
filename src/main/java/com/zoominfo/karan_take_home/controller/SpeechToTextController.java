package com.zoominfo.karan_take_home.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zoominfo.karan_take_home.dto.incoming.SpeechToTextRequestDto;
import com.zoominfo.karan_take_home.dto.outgoing.SpeechToTextResponseDto;
import com.zoominfo.karan_take_home.services.SpeechToTextService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class SpeechToTextController {

    private final SpeechToTextService speechToTextService;

    @Operation(
        summary = "Convert speech to text",
        description = "Transcribes an audio file to text using the Faster Whisper model. Returns Server-Sent Events (SSE) stream with transcription results.",
        requestBody = @RequestBody(
            description = "Audio file and transcription parameters",
            required = true,
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = @Schema(implementation = SpeechToTextRequestDto.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful transcription. Returns SSE stream with transcription chunks.",
                content = @Content(
                    mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                    examples = {
                        @ExampleObject(
                            name = "Example SSE Response",
                            description = "Server-Sent Events stream format with transcription data",
                            value = "data:{\"text\":\"A zestful food is the hot cross bun.\"}\n\n"
                        )
                    },
                    schema = @Schema(implementation = SpeechToTextResponseDto.class)
                )
            )
        }
    )
    @PostMapping(
        path = "/speech-to-text",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<SpeechToTextResponseDto>> speechToText(
            @Valid @ModelAttribute SpeechToTextRequestDto requestDto) {
        System.out.println("Received api call" + requestDto.toString());
        return speechToTextService.transcribe(requestDto.toRequest())
                .map(response -> ServerSentEvent.<SpeechToTextResponseDto>builder()
                        .data(SpeechToTextResponseDto.from(response))
                        .build());
    }
}

