package com.practiq.service.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DocumentUploadCommandTest {

    @Test
    void aCommandHoldsTheValuesItWasBuiltWith() {
        String filename = "physics-notes.pdf";
        String contentType = "application/pdf";
        String sourceSpec = "AQA GCSE Physics";
        int contentLength = 2048;

        DocumentUploadCommand command = new DocumentUploadCommand(filename, contentType, contentLength, sourceSpec);

        assertEquals(filename, command.filename());
        assertEquals(contentType, command.contentType());
        assertEquals(contentLength, command.contentLength());
        assertEquals(sourceSpec, command.sourceSpec());
    }

    @Test
    void sourceSpecIsOptional() {
        DocumentUploadCommand command = new DocumentUploadCommand("physics-notes.pdf", "application/pdf", 2048, null);

        assertNull(command.sourceSpec());
    }

    @Test
    void aCommandCannotBeBuiltWithoutAFilename() {
        IllegalArgumentException nullFilename = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentUploadCommand(null, "application/pdf", 2048, "AQA GCSE Physics"));

        assertEquals("filename must not be blank", nullFilename.getMessage());

        IllegalArgumentException emptyFilename = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentUploadCommand("", "application/pdf", 2048, "AQA GCSE Physics"));

        assertEquals("filename must not be blank", emptyFilename.getMessage());

        IllegalArgumentException whitespaceFilename = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentUploadCommand("   ", "application/pdf", 2048, "AQA GCSE Physics"));

        assertEquals("filename must not be blank", whitespaceFilename.getMessage());
    }

    @Test
    void aCommandCannotBeBuiltWithoutAContentType() {
        IllegalArgumentException nullContentType = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentUploadCommand("physics-notes.pdf", null, 2048, "AQA GCSE Physics"));

        assertEquals("contentType must not be blank", nullContentType.getMessage());

        IllegalArgumentException emptyContentType = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentUploadCommand("physics-notes.pdf", "", 2048, "AQA GCSE Physics"));

        assertEquals("contentType must not be blank", emptyContentType.getMessage());

        IllegalArgumentException whitespaceContentType = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentUploadCommand("physics-notes.pdf", "   ", 2048, "AQA GCSE Physics"));

        assertEquals("contentType must not be blank", whitespaceContentType.getMessage());
    }

    @Test
    void aCommandCannotBeBuiltWithoutAContentLength() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentUploadCommand("physics-notes.pdf", "application/pdf", null, "AQA GCSE Physics"));

        assertEquals("contentLength must not be null", exception.getMessage());
    }

    @Test
    void aCommandCannotBeBuiltWithAContentLengthBelowOne() {
        IllegalArgumentException zero = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentUploadCommand("physics-notes.pdf", "application/pdf", 0, "AQA GCSE Physics"));

        assertEquals("contentLength must be greater than or equal to 1", zero.getMessage());

        IllegalArgumentException negative = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentUploadCommand("physics-notes.pdf", "application/pdf", -1, "AQA GCSE Physics"));

        assertEquals("contentLength must be greater than or equal to 1", negative.getMessage());
    }
}
