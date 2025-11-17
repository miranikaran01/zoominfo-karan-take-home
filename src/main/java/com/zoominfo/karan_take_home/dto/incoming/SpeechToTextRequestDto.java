package com.zoominfo.karan_take_home.dto.incoming;

import org.springframework.web.multipart.MultipartFile;

import com.zoominfo.karan_take_home.SpeechToTextRequest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/*
 * Immutable request DTO for speech to text conversion
 * @param file The audio file to convert to text. Must not be null.
 * @param language The language of the audio file. 
 * @param model The model to use for the conversion. 
 * @param stream Whether to stream the conversion.
 */
@Builder
@Schema(description = "Request DTO for speech to text conversion")
public record SpeechToTextRequestDto(
    @NotNull(message = "File is required")
    @Schema(description = "The audio file to convert to text", type = "string", format = "binary")
    MultipartFile file,

    @Schema(description = "The language of the audio file", example = "en")
    String language,

    @Schema(description = "The model to use for the conversion", example = "Systran/faster-whisper-small")
    String model,

    @Schema(description = "Whether to stream the conversion", example = "false")
    Boolean stream
) {
    public SpeechToTextRequest toRequest() {
        return SpeechToTextRequest.builder()
            .file(file)
            .language(language)
            .model(model)
            .stream(stream)
            .build();
    }
}
