package com.zoominfo.karan_take_home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;


class FasterWhisperRequestTest {

    private MockMultipartFile createMockFile() {
        return new MockMultipartFile(
            "file",
            "test-audio.wav",
            "audio/wav",
            "test audio content".getBytes()
        );
    }

    @Test
    void testCreateWithAllFields() {
        MockMultipartFile file = createMockFile();
        FasterWhisperRequest request = new FasterWhisperRequest(
            file,
            "en",
            "Systran/faster-whisper-small",
            true
        );

        assertNotNull(request);
        assertEquals(file, request.file());
        assertEquals("en", request.language());
        assertEquals("Systran/faster-whisper-small", request.model());
        assertTrue(request.stream());
    }

    @Test
    void testCreateWithStreamFalse() {
        MockMultipartFile file = createMockFile();
        FasterWhisperRequest request = new FasterWhisperRequest(
            file,
            "es",
            "Systran/faster-whisper-large-v3",
            false
        );

        assertEquals(file, request.file());
        assertEquals("es", request.language());
        assertEquals("Systran/faster-whisper-large-v3", request.model());
        assertFalse(request.stream());
    }

    @Test
    void testCreateWithDifferentLanguages() {
        MockMultipartFile file = createMockFile();
        
        FasterWhisperRequest requestEn = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-small", false
        );
        FasterWhisperRequest requestFr = new FasterWhisperRequest(
            file, "fr", "Systran/faster-whisper-small", false
        );
        FasterWhisperRequest requestDe = new FasterWhisperRequest(
            file, "de", "Systran/faster-whisper-small", false
        );

        assertEquals("en", requestEn.language());
        assertEquals("fr", requestFr.language());
        assertEquals("de", requestDe.language());
    }

    @Test
    void testCreateWithDifferentModels() {
        MockMultipartFile file = createMockFile();
        
        FasterWhisperRequest requestSmall = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-small", false
        );
        FasterWhisperRequest requestMedium = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-medium", false
        );
        FasterWhisperRequest requestLarge = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-large-v3", false
        );

        assertEquals("Systran/faster-whisper-small", requestSmall.model());
        assertEquals("Systran/faster-whisper-medium", requestMedium.model());
        assertEquals("Systran/faster-whisper-large-v3", requestLarge.model());
    }

    @Test
    void testRecordEquality() {
        MockMultipartFile file1 = createMockFile();
        MockMultipartFile file2 = createMockFile();

        FasterWhisperRequest request1 = new FasterWhisperRequest(
            file1, "en", "Systran/faster-whisper-small", true
        );
        FasterWhisperRequest request2 = new FasterWhisperRequest(
            file2, "en", "Systran/faster-whisper-small", true
        );

        // Records use structural equality, but MultipartFile equality is by reference
        // So these won't be equal even if fields match
        assertNotEquals(request1, request2);
        assertEquals(request1.language(), request2.language());
        assertEquals(request1.model(), request2.model());
        assertEquals(request1.stream(), request2.stream());
    }

    @Test
    void testRecordEqualityWithSameFileReference() {
        MockMultipartFile file = createMockFile();

        FasterWhisperRequest request1 = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-small", true
        );
        FasterWhisperRequest request2 = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-small", true
        );

        // With the same file reference, records should be equal
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testRecordInequalityWithDifferentFields() {
        MockMultipartFile file = createMockFile();

        FasterWhisperRequest request1 = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-small", true
        );
        FasterWhisperRequest request2 = new FasterWhisperRequest(
            file, "es", "Systran/faster-whisper-small", true
        );
        FasterWhisperRequest request3 = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-medium", true
        );
        FasterWhisperRequest request4 = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-small", false
        );

        assertNotEquals(request1, request2); // Different language
        assertNotEquals(request1, request3); // Different model
        assertNotEquals(request1, request4); // Different stream
    }

    @Test
    void testToString() {
        MockMultipartFile file = createMockFile();
        FasterWhisperRequest request = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-small", true
        );

        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("FasterWhisperRequest"));
        assertTrue(toString.contains("en"));
        assertTrue(toString.contains("Systran/faster-whisper-small"));
        assertTrue(toString.contains("true"));
    }

    @Test
    void testRecordImmutability() {
        MockMultipartFile file = createMockFile();
        FasterWhisperRequest request = new FasterWhisperRequest(
            file, "en", "Systran/faster-whisper-small", true
        );

        // Records are immutable - we can't modify fields
        // This test verifies that accessors return the correct values
        assertEquals("en", request.language());
        assertEquals("Systran/faster-whisper-small", request.model());
        assertTrue(request.stream());
        assertEquals(file, request.file());
    }

    @Test
    void testWithEmptyStringFields() {
        MockMultipartFile file = createMockFile();
        
        // Note: @NotEmpty validation would typically catch these at runtime,
        // but we're testing the record structure itself
        FasterWhisperRequest request = new FasterWhisperRequest(
            file, "", "", false
        );

        assertEquals("", request.language());
        assertEquals("", request.model());
        assertFalse(request.stream());
    }

    @Test
    void testWithVariousFileTypes() {
        MockMultipartFile wavFile = new MockMultipartFile(
            "file", "audio.wav", "audio/wav", "wav content".getBytes()
        );
        MockMultipartFile mp3File = new MockMultipartFile(
            "file", "audio.mp3", "audio/mpeg", "mp3 content".getBytes()
        );
        MockMultipartFile m4aFile = new MockMultipartFile(
            "file", "audio.m4a", "audio/mp4", "m4a content".getBytes()
        );

        FasterWhisperRequest requestWav = new FasterWhisperRequest(
            wavFile, "en", "Systran/faster-whisper-small", false
        );
        FasterWhisperRequest requestMp3 = new FasterWhisperRequest(
            mp3File, "en", "Systran/faster-whisper-small", false
        );
        FasterWhisperRequest requestM4a = new FasterWhisperRequest(
            m4aFile, "en", "Systran/faster-whisper-small", false
        );

        assertEquals("audio.wav", requestWav.file().getOriginalFilename());
        assertEquals("audio.mp3", requestMp3.file().getOriginalFilename());
        assertEquals("audio.m4a", requestM4a.file().getOriginalFilename());
    }
}
