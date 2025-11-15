package com.zoominfo.karan_take_home.dto.incoming;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

/*
 * Request DTO for speech to text conversion
 * @param file The audio file to convert to text. Must not be empty.
 * @param language The language of the audio file. Must not be empty.
 * @param model The model to use for the conversion. Default is "en".
 * @param stream Whether to stream the conversion. Default is false.
 */
public record SpeechToTextRequestDto(
    @NotEmpty
    MultipartFile file,

    @NotEmpty
    String language,

    String model,

    boolean stream
) {
}
