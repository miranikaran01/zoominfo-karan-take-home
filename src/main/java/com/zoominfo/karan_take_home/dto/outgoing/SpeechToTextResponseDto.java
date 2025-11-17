package com.zoominfo.karan_take_home.dto.outgoing;

import com.zoominfo.karan_take_home.SpeechToTextResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

/*
 * Response DTO for speech to text conversion
 * @param text The text converted from the audio file. Must not be empty.
 */
@Schema(description = "Response DTO for speech to text conversion")
public record SpeechToTextResponseDto(
    @NotEmpty
    @Schema(description = "The transcribed text from the audio file", example = "A zestful food is the hot cross bun.")
    String text
) {
    
    public static SpeechToTextResponseDto from(SpeechToTextResponse response) {
        return new SpeechToTextResponseDto(response.text());
    }
}
