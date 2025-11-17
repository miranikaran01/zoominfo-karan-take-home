package com.zoominfo.karan_take_home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;


class SpeechToTextRequestTest {

    private MockMultipartFile createMockFile() {
        return new MockMultipartFile(
            "file",
            "test-audio.wav",
            "audio/wav",
            "test audio content".getBytes()
        );
    }

    @Test
    void testBuilderWithAllFieldsProvided() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = SpeechToTextRequest.builder()
            .file(file)
            .language("es")
            .model("Systran/faster-whisper-large-v3")
            .stream(true)
            .build();

        assertNotNull(request);
        assertEquals(file, request.file());
        assertEquals("es", request.language());
        assertEquals("Systran/faster-whisper-large-v3", request.model());
        assertTrue(request.stream());
    }

    @Test
    void testBuilderWithDefaultValues() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = SpeechToTextRequest.builder()
            .file(file)
            .build();

        assertNotNull(request);
        assertEquals(file, request.file());
        assertEquals("en", request.language());
        assertEquals("Systran/faster-whisper-small", request.model());
        assertFalse(request.stream());
    }

    @Test
    void testBuilderWithNullLanguage() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = SpeechToTextRequest.builder()
            .file(file)
            .language(null)
            .build();

        assertEquals("en", request.language());
    }

    @Test
    void testBuilderWithEmptyLanguage() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = SpeechToTextRequest.builder()
            .file(file)
            .language("")
            .build();

        assertEquals("en", request.language());
    }

    @Test
    void testBuilderWithNullModel() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = SpeechToTextRequest.builder()
            .file(file)
            .model(null)
            .build();

        assertEquals("Systran/faster-whisper-small", request.model());
    }

    @Test
    void testBuilderWithEmptyModel() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = SpeechToTextRequest.builder()
            .file(file)
            .model("")
            .build();

        assertEquals("Systran/faster-whisper-small", request.model());
    }

    @Test
    void testBuilderWithNullStream() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = SpeechToTextRequest.builder()
            .file(file)
            .stream(null)
            .build();

        assertFalse(request.stream());
    }

    @Test
    void testBuilderWithStreamTrue() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = SpeechToTextRequest.builder()
            .file(file)
            .stream(true)
            .build();

        assertTrue(request.stream());
    }

    @Test
    void testBuilderWithStreamFalse() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = SpeechToTextRequest.builder()
            .file(file)
            .stream(false)
            .build();

        assertFalse(request.stream());
    }

    @Test
    void testConstructorWithAllNullOptionalFields() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = new SpeechToTextRequest(file, null, null, null);

        assertEquals(file, request.file());
        assertEquals("en", request.language());
        assertEquals("Systran/faster-whisper-small", request.model());
        assertFalse(request.stream());
    }

    @Test
    void testConstructorWithEmptyOptionalFields() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = new SpeechToTextRequest(file, "", "", null);

        assertEquals(file, request.file());
        assertEquals("en", request.language());
        assertEquals("Systran/faster-whisper-small", request.model());
        assertFalse(request.stream());
    }

    @Test
    void testConstructorWithAllFieldsProvided() {
        MockMultipartFile file = createMockFile();
        SpeechToTextRequest request = new SpeechToTextRequest(
            file,
            "fr",
            "Systran/faster-whisper-medium",
            true
        );

        assertEquals(file, request.file());
        assertEquals("fr", request.language());
        assertEquals("Systran/faster-whisper-medium", request.model());
        assertTrue(request.stream());
    }

    @Test
    void testRecordEquality() {
        MockMultipartFile file1 = createMockFile();
        MockMultipartFile file2 = createMockFile();

        SpeechToTextRequest request1 = SpeechToTextRequest.builder()
            .file(file1)
            .language("en")
            .model("Systran/faster-whisper-small")
            .stream(false)
            .build();

        SpeechToTextRequest request2 = SpeechToTextRequest.builder()
            .file(file2)
            .language("en")
            .model("Systran/faster-whisper-small")
            .stream(false)
            .build();

        // Records use structural equality, but MultipartFile equality is by reference
        // So these won't be equal even if fields match
        assertNotEquals(request1, request2);
        assertEquals(request1.language(), request2.language());
        assertEquals(request1.model(), request2.model());
        assertEquals(request1.stream(), request2.stream());
    }
}
