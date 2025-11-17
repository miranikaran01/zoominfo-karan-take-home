package com.zoominfo.karan_take_home;

import jakarta.validation.constraints.NotEmpty;

/*
 * Response record for speech to text conversion
 * @param text The text converted from the audio file. Must not be empty.
 */
public record SpeechToTextResponse(
    @NotEmpty
    String text
) {}
