package com.practiq.service.document;

import static org.junit.jupiter.api.Assertions.*;
import static utils.data.TestData.DOCUMENT_UPLOAD_MAX_CONTENT_LENGTH;

import com.practiq.foundation.exception.ContentTooLargeException;
import com.practiq.foundation.exception.EntityValidationError;
import com.practiq.foundation.exception.EntityValidationException;
import com.practiq.service.document.dto.request.DocumentPresignUploadCommand;
import io.micronaut.http.MediaType;
import org.junit.jupiter.api.Test;
import utils.RegexUtils;

class DocumentStagerTest {

    private final DocumentStager stager = new DocumentStager();

    @Test
    void stageUploadStagesTheUpload() {
        String filename = "physics-notes.pdf";
        String sourceSpec = "AQA GCSE Physics";
        int contentLength = 2048;

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand(filename, "application/pdf", contentLength, sourceSpec);

        StagedDocumentUpload staged = stager.stageUpload(command);

        assertTrue(staged.key().matches(RegexUtils.uuidWithExtension("pdf")), "key was " + staged.key());
        assertEquals(filename, staged.filename());
        assertEquals(sourceSpec, staged.sourceSpec());
        assertEquals(MediaType.APPLICATION_PDF_TYPE, staged.contentType());
        assertEquals(contentLength, staged.contentLength());
    }

    @Test
    void stageUploadStagesTheUploadWhenContentLengthIsAtTheMaximum() {
        String filename = "physics-notes.pdf";
        String sourceSpec = "AQA GCSE Physics";
        int contentLength = DOCUMENT_UPLOAD_MAX_CONTENT_LENGTH;

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand(filename, "application/pdf", contentLength, sourceSpec);

        StagedDocumentUpload staged = stager.stageUpload(command);

        assertTrue(staged.key().matches(RegexUtils.uuidWithExtension("pdf")), "key was " + staged.key());
        assertEquals(filename, staged.filename());
        assertEquals(sourceSpec, staged.sourceSpec());
        assertEquals(MediaType.APPLICATION_PDF_TYPE, staged.contentType());
        assertEquals(contentLength, staged.contentLength());
    }

    @Test
    void stageUploadThrowsWhenContentLengthIsOneOverTheMaximum() {
        int contentLength = DOCUMENT_UPLOAD_MAX_CONTENT_LENGTH + 1;

        DocumentPresignUploadCommand command = new DocumentPresignUploadCommand(
                "physics-notes.pdf", "application/pdf", contentLength, "AQA GCSE Physics");

        ContentTooLargeException exception =
                assertThrows(ContentTooLargeException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError(
                        "contentLength", "must not be greater than " + DOCUMENT_UPLOAD_MAX_CONTENT_LENGTH),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenContentTypeCannotBeParsed() {
        String contentType = "pdf";

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand("physics-notes.pdf", contentType, 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("contentType", "'" + contentType + "' is not a valid content type"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenContentTypeIsNotRecognised() {
        String contentType = "application/x-not-real";

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand("physics-notes.pdf", contentType, 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("contentType", "'" + contentType + "' is not a supported content type"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenContentTypeIsRecognisedButNotAccepted() {
        String contentType = "application/zip";

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand("archive.zip", contentType, 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("contentType", "'" + contentType + "' is not a supported content type"),
                exception.error());
    }

    @Test
    void stageUploadAcceptsAContentTypeThatOnlyMatchesOnceLowercased() {
        String filename = "diagram.PNG";

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand(filename, "IMAGE/PNG", 1024, "AQA GCSE Physics");

        StagedDocumentUpload staged = stager.stageUpload(command);

        assertEquals(MediaType.IMAGE_PNG_TYPE, staged.contentType());
        assertEquals(filename, staged.filename());
        assertTrue(staged.key().matches(RegexUtils.uuidWithExtension("png")), "key was " + staged.key());
    }

    @Test
    void stageUploadThrowsWhenFilenameHasNoExtensionSeparator() {
        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand("physics-notes", "application/pdf", 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(new EntityValidationError("filename", "must have a file extension"), exception.error());
    }

    @Test
    void stageUploadThrowsWhenFilenameEndsWithTheExtensionSeparator() {
        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand("physics-notes.", "application/pdf", 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(new EntityValidationError("filename", "must have a file extension"), exception.error());
    }

    @Test
    void stageUploadThrowsWhenNoContentTypeIsKnownForTheExtension() {
        String extension = "dat";

        DocumentPresignUploadCommand command = new DocumentPresignUploadCommand(
                "physics-notes." + extension, "application/pdf", 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("filename", "'." + extension + "' is not a recognised file extension"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenTheExtensionsContentTypeIsNotAccepted() {
        String extension = "zip";

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand("archive." + extension, "application/pdf", 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("filename", "'." + extension + "' files are not supported"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenTheDeclaredAndDerivedContentTypesAreBothAcceptedButDiffer() {
        String contentType = "application/pdf";
        String extension = "png";

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand("diagram." + extension, contentType, 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError(
                        "contentType", "'" + contentType + "' does not match the '." + extension + "' file extension"),
                exception.error());
    }
}
