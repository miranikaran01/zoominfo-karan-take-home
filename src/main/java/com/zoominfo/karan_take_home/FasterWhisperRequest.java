package com.zoominfo.karan_take_home;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotEmpty;

/*
 * Request record for faster whisper request
 * @param file The audio file to convert to text. Must not be empty.
 * @param language The language of the audio file. Must not be empty.
 * @param model The model to use for the conversion. Must not be empty.
 * @param stream Whether to stream the conversion. Must not be empty.
 */
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
