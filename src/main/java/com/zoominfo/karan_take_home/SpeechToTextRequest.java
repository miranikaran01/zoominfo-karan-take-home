package com.zoominfo.karan_take_home;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/*
 * Request record for speech to text conversion
 * @param file The audio file to convert to text. Must not be null.
 * @param language The language of the audio file. Default is "en".
 * @param model The model to use for the conversion. Default is "Systran/faster-whisper-small".
 * @param stream Whether to stream the conversion. Default is false.
 */
@Builder
public record SpeechToTextRequest(
    @NotNull
    MultipartFile file,
    String language,
    String model,
    Boolean stream
) {
    public SpeechToTextRequest(MultipartFile file, String language, String model, Boolean stream) {
        this.file = file;
        if (file.getContentType() == null || !file.getContentType().startsWith("audio")) {
            throw new IllegalArgumentException("File of type " + file.getContentType() + " not supported. File must be an audio file");
        }
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
        if (stream == null) {
            this.stream = false;
        } else {
            this.stream = stream;
        }
    }
}
