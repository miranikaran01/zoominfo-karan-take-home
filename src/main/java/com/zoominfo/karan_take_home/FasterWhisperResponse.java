package com.zoominfo.karan_take_home;


import jakarta.validation.constraints.NotEmpty;
/*
 * Response record for faster whisper response
 * @param text The text converted from the audio file. Must not be empty.
 */
public record FasterWhisperResponse(
    @NotEmpty
    String text
) {
}
