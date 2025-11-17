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
        // Validate Content-Type if provided, but be lenient since many clients don't set it correctly
        // Only reject if Content-Type is explicitly set to something that's clearly not audio
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("File must have a Content-Type");
        }
        if (!contentType.startsWith("audio/")
                && !contentType.equals("application/octet-stream")) {
            // Check if it's a known non-audio type
            if (contentType.startsWith("text/") ||
                    contentType.startsWith("image/") ||
                    contentType.startsWith("video/") ||
                    contentType.startsWith("application/json") ||
                    contentType.startsWith("application/xml")) {
                throw new IllegalArgumentException(
                        "File of type " + contentType + " not supported. File must be an audio file");
            }
        }
        this.file = file;
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
