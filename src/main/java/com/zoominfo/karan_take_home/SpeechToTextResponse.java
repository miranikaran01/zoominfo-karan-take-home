package com.zoominfo.karan_take_home;

import jakarta.validation.constraints.NotEmpty;

public record SpeechToTextResponse(
    @NotEmpty
    String text
) {}
