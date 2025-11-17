package com.zoominfo.karan_take_home.clients;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.bind.annotation.RequestPart;

import com.zoominfo.karan_take_home.dto.incoming.FasterWhisperResponseDto;

import reactor.core.publisher.Flux;

public interface FasterWhisperClient {
    
    @PostExchange(
        url = "/v1/audio/transcriptions",
        contentType = MediaType.MULTIPART_FORM_DATA_VALUE,
        accept = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    Flux<ServerSentEvent<FasterWhisperResponseDto>> transcribe(
        @RequestPart("file") MultipartFile file,
        @RequestPart("language") String language,
        @RequestPart("model") String model,
        @RequestPart("stream") boolean stream
    );
}
