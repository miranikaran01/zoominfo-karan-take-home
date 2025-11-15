package com.zoominfo.karan_take_home.controller;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zoominfo.karan_take_home.dto.incoming.SpeechToTextRequestDto;
import com.zoominfo.karan_take_home.dto.outgoing.SpeechToTextResponseDto;

@RestController
public class SpeechToTextController {
    @PostMapping("/speech-to-text")
    public SpeechToTextResponseDto speechToText(@ModelAttribute SpeechToTextRequestDto requestDto) {
        return new SpeechToTextResponseDto("Hello, world!");
    }
}
