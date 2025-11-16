package com.zoominfo.karan_take_home;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotEmpty;

public record FasterWhisperRequest(
    @NotEmpty
    MultipartFile file,

    @NotEmpty
    String language,

    @NotEmpty
    String model,

    @NotEmpty
    boolean stream
) {

}
