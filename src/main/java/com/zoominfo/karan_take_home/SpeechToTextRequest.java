package com.zoominfo.karan_take_home;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

/*
 * Request record for speech to text conversion
 * @param file The audio file to convert to text. Must not be empty.
 * @param language The language of the audio file. Default is "en".
 * @param model The model to use for the conversion. Default is "Systran/faster-whisper-small".
 * @param stream Whether to stream the conversion. Default is false.
 */
@Builder
public record SpeechToTextRequest(
    @NotEmpty
    MultipartFile file,
    String language,
    String model,
    boolean stream
) {
    public SpeechToTextRequest(MultipartFile file, String language, String model, boolean stream) {
        this.file = file;
        this.stream = stream;
        if (language == null || language.isEmpty()) {
            this.language = "en";
        } else {
            this.language = language;
        }
        if (model == null || model.isEmpty()) {
            this.model = "Systran/faster-whisper-small";
        } else {
            this.model = model;
        }
    }
}
