package com.zoominfo.karan_take_home.services;

import org.springframework.stereotype.Service;

import com.zoominfo.karan_take_home.FasterWhisperResponse;
import com.zoominfo.karan_take_home.SpeechToTextRequest;
import com.zoominfo.karan_take_home.SpeechToTextResponse;
import com.zoominfo.karan_take_home.clients.FasterWhisperClient;
import com.zoominfo.karan_take_home.dto.incoming.FasterWhisperResponseDto;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class SpeechToTextService {
    private final FasterWhisperClient fasterWhisperClient;
    
    public Flux<SpeechToTextResponse> transcribe(SpeechToTextRequest request) {
        return fasterWhisperClient
                .transcribe(request.file(), request.language(), request.model(), request.stream())
                .filter(event -> event.data() != null)
                .map(event -> {
                    FasterWhisperResponseDto dto = event.data();
                    FasterWhisperResponse response = dto.toResponse();
                    return new SpeechToTextResponse(response.text());
                });
    }
}