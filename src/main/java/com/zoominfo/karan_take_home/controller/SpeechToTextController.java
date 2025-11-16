package com.zoominfo.karan_take_home.controller;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import com.zoominfo.karan_take_home.services.SpeechToTextService;
import com.zoominfo.karan_take_home.dto.incoming.SpeechToTextRequestDto;
import com.zoominfo.karan_take_home.dto.outgoing.SpeechToTextResponseDto;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class SpeechToTextController {
    
    private final SpeechToTextService speechToTextService;
    
    @Operation(
        summary = "Convert speech to text",
        description = "Transcribes an audio file to text using the Faster Whisper model",
        requestBody = @RequestBody(
            description = "Audio file and transcription parameters",
            required = true,
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = @Schema(implementation = SpeechToTextRequestDto.class)
            )
        )
    )
    @PostMapping(
        path = "/speech-to-text", 
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<SpeechToTextResponseDto> speechToText(@ModelAttribute SpeechToTextRequestDto requestDto) {
        return speechToTextService.transcribe(requestDto.toRequest()).map(SpeechToTextResponseDto::from);
    }
}
