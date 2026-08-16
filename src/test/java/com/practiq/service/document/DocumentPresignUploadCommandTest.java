package com.practiq.service.document;

import static org.junit.jupiter.api.Assertions.*;
import static utils.data.TestData.DOCUMENT_FILENAME_MAX_LENGTH;
import static utils.data.TestData.DOCUMENT_SOURCE_SPEC_MAX_LENGTH;

import com.practiq.service.document.dto.request.DocumentPresignUploadCommand;
import org.junit.jupiter.api.Test;

class DocumentPresignUploadCommandTest {

    @Test
    void aCommandHoldsTheValuesItWasBuiltWith() {
        String filename = "physics-notes.pdf";
        String contentType = "application/pdf";
        String sourceSpec = "AQA GCSE Physics";
        int contentLength = 2048;

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand(filename, contentType, contentLength, sourceSpec);

        assertEquals(filename, command.filename());
        assertEquals(contentType, command.contentType());
        assertEquals(contentLength, command.contentLength());
        assertEquals(sourceSpec, command.sourceSpec());
    }

    @Test
    void sourceSpecIsOptional() {
        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand("physics-notes.pdf", "application/pdf", 2048, null);

        assertNull(command.sourceSpec());
    }

    @Test
    void aCommandCannotBeBuiltWithoutAFilename() {
        IllegalArgumentException nullFilename = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand(null, "application/pdf", 2048, "AQA GCSE Physics"));

        assertEquals("filename must not be blank", nullFilename.getMessage());

        IllegalArgumentException emptyFilename = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand("", "application/pdf", 2048, "AQA GCSE Physics"));

        assertEquals("filename must not be blank", emptyFilename.getMessage());

        IllegalArgumentException whitespaceFilename = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand("   ", "application/pdf", 2048, "AQA GCSE Physics"));

        assertEquals("filename must not be blank", whitespaceFilename.getMessage());
    }

    @Test
    void aCommandCannotBeBuiltWithoutAContentType() {
        IllegalArgumentException nullContentType = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand("physics-notes.pdf", null, 2048, "AQA GCSE Physics"));

        assertEquals("contentType must not be blank", nullContentType.getMessage());

        IllegalArgumentException emptyContentType = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand("physics-notes.pdf", "", 2048, "AQA GCSE Physics"));

        assertEquals("contentType must not be blank", emptyContentType.getMessage());

        IllegalArgumentException whitespaceContentType = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand("physics-notes.pdf", "   ", 2048, "AQA GCSE Physics"));

        assertEquals("contentType must not be blank", whitespaceContentType.getMessage());
    }

    @Test
    void aCommandTakesAFilenameAtTheMaximumLengthButNotOneAbove() {
        String atTheMaximum = "a".repeat(DOCUMENT_FILENAME_MAX_LENGTH - ".pdf".length()) + ".pdf";

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand(atTheMaximum, "application/pdf", 2048, "AQA GCSE Physics");

        assertEquals(DOCUMENT_FILENAME_MAX_LENGTH, command.filename().length());
        assertEquals(atTheMaximum, command.filename());

        String oneAbove = "a".repeat(DOCUMENT_FILENAME_MAX_LENGTH + 1 - ".pdf".length()) + ".pdf";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand(oneAbove, "application/pdf", 2048, "AQA GCSE Physics"));

        assertEquals("filename cannot exceed max length " + DOCUMENT_FILENAME_MAX_LENGTH, exception.getMessage());
    }

    @Test
    void aCommandTakesASourceSpecAtTheMaximumLengthButNotOneAbove() {
        String atTheMaximum = "a".repeat(DOCUMENT_SOURCE_SPEC_MAX_LENGTH);

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand("physics-notes.pdf", "application/pdf", 2048, atTheMaximum);

        assertEquals(DOCUMENT_SOURCE_SPEC_MAX_LENGTH, command.sourceSpec().length());
        assertEquals(atTheMaximum, command.sourceSpec());

        String oneAbove = "a".repeat(DOCUMENT_SOURCE_SPEC_MAX_LENGTH + 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand("physics-notes.pdf", "application/pdf", 2048, oneAbove));

        assertEquals("sourceSpec cannot exceed max length " + DOCUMENT_SOURCE_SPEC_MAX_LENGTH, exception.getMessage());
    }

    @Test
    void aCommandCannotBeBuiltWithAContentLengthBelowOne() {
        IllegalArgumentException zero = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand("physics-notes.pdf", "application/pdf", 0, "AQA GCSE Physics"));

        assertEquals("contentLength must be greater than or equal to 1", zero.getMessage());

        IllegalArgumentException negative = assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentPresignUploadCommand("physics-notes.pdf", "application/pdf", -1, "AQA GCSE Physics"));

        assertEquals("contentLength must be greater than or equal to 1", negative.getMessage());
    }
}
