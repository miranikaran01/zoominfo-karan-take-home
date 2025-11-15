package com.zoominfo.karan_take_home.dto.outgoing;

import jakarta.validation.constraints.NotEmpty;

/*
 * Response DTO for speech to text conversion
 * @param text The text converted from the audio file. Must not be empty.
 */
public record SpeechToTextResponseDto(
    @NotEmpty
    String text
) {
    
}
