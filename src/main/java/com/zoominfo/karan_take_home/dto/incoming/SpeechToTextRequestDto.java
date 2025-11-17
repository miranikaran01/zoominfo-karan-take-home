package com.zoominfo.karan_take_home.dto.incoming;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.media.Schema;

import com.zoominfo.karan_take_home.SpeechToTextRequest;

/*
 * Immutable request DTO for speech to text conversion
 * @param file The audio file to convert to text. Must not be empty.
 * @param language The language of the audio file. 
 * @param model The model to use for the conversion. 
 * @param stream Whether to stream the conversion.
 */
@Builder
@Schema(description = "Request DTO for speech to text conversion")
public record SpeechToTextRequestDto(
    @NotEmpty
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
