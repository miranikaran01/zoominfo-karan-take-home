package com.zoominfo.karan_take_home.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zoominfo.karan_take_home.dto.incoming.SpeechToTextRequestDto;
import com.zoominfo.karan_take_home.dto.outgoing.SpeechToTextResponseDto;
import com.zoominfo.karan_take_home.services.SpeechToTextService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class SpeechToTextController {

    private final SpeechToTextService speechToTextService;

    @PostMapping(
        path = "/speech-to-text",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<SpeechToTextResponseDto>> speechToText(
            @Valid @ModelAttribute SpeechToTextRequestDto requestDto) {

        return speechToTextService.transcribe(requestDto.toRequest())
                .map(response -> ServerSentEvent.<SpeechToTextResponseDto>builder()
                        .data(SpeechToTextResponseDto.from(response))
                        .build());
    }
}

