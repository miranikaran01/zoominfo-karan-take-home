package com.zoominfo.karan_take_home.controller;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import com.zoominfo.karan_take_home.services.SpeechToTextService;
import com.zoominfo.karan_take_home.dto.incoming.SpeechToTextRequestDto;
import com.zoominfo.karan_take_home.dto.outgoing.SpeechToTextResponseDto;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class SpeechToTextController {
    
    private final SpeechToTextService speechToTextService;
    
    @PostMapping(path = "/speech-to-text", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<SpeechToTextResponseDto> speechToText(@ModelAttribute SpeechToTextRequestDto requestDto) {
        return speechToTextService.transcribe(requestDto.toRequest()).map(SpeechToTextResponseDto::from);
    }
}
