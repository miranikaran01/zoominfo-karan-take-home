package com.zoominfo.karan_take_home.dto.incoming;

import com.zoominfo.karan_take_home.FasterWhisperResponse;

import jakarta.validation.constraints.NotEmpty;

public record FasterWhisperResponseDto(
    
@NotEmpty
String text
) {
    public FasterWhisperResponse toResponse() {
        return new FasterWhisperResponse(this.text);
    }
}
