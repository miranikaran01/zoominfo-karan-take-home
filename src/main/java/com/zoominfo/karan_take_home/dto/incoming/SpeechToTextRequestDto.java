package com.zoominfo.karan_take_home.dto.incoming;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import com.zoominfo.karan_take_home.SpeechToTextRequest;

/*
 * Immutable request DTO for speech to text conversion
 * @param file The audio file to convert to text. Must not be empty.
 * @param language The language of the audio file. 
 * @param model The model to use for the conversion. 
 * @param stream Whether to stream the conversion.
 */
@Builder
public record SpeechToTextRequestDto(
    @NotEmpty
    MultipartFile file,

    String language,

    String model,

    boolean stream
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
