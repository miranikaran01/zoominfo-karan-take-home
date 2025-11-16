package com.zoominfo.karan_take_home.services;

import com.zoominfo.karan_take_home.SpeechToTextRequest;
import com.zoominfo.karan_take_home.SpeechToTextResponse;
import com.zoominfo.karan_take_home.clients.FasterWhisperClient;
import com.zoominfo.karan_take_home.dto.incoming.FasterWhisperResponseDto;
import com.zoominfo.karan_take_home.FasterWhisperResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpeechToTextService {
    private final FasterWhisperClient fasterWhisperClient;
    
    public Flux<SpeechToTextResponse> transcribe(SpeechToTextRequest request) {
            return fasterWhisperClient.transcribe(request.file(), request.language(), request.model(), request.stream())
                                    .map(FasterWhisperResponseDto::toResponse)
                                    .map(FasterWhisperResponse::text)
                                    .map(SpeechToTextResponse::new);
    }
}
