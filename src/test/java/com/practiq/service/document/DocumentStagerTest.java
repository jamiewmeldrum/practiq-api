package com.practiq.service.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static utils.data.TestData.MAX_UPLOAD_CONTENT_LENGTH;

import com.practiq.exception.ContentTooLargeException;
import com.practiq.exception.EntityValidationError;
import com.practiq.exception.EntityValidationException;
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

        DocumentUploadCommand command =
                new DocumentUploadCommand(filename, "application/pdf", contentLength, sourceSpec);

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
        int contentLength = MAX_UPLOAD_CONTENT_LENGTH;

        DocumentUploadCommand command =
                new DocumentUploadCommand(filename, "application/pdf", contentLength, sourceSpec);

        StagedDocumentUpload staged = stager.stageUpload(command);

        assertTrue(staged.key().matches(RegexUtils.uuidWithExtension("pdf")), "key was " + staged.key());
        assertEquals(filename, staged.filename());
        assertEquals(sourceSpec, staged.sourceSpec());
        assertEquals(MediaType.APPLICATION_PDF_TYPE, staged.contentType());
        assertEquals(contentLength, staged.contentLength());
    }

    @Test
    void stageUploadThrowsWhenContentLengthIsOneOverTheMaximum() {
        int contentLength = MAX_UPLOAD_CONTENT_LENGTH + 1;

        DocumentUploadCommand command =
                new DocumentUploadCommand("physics-notes.pdf", "application/pdf", contentLength, "AQA GCSE Physics");

        ContentTooLargeException exception =
                assertThrows(ContentTooLargeException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("contentLength", "must not be greater than " + MAX_UPLOAD_CONTENT_LENGTH),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenContentTypeCannotBeParsed() {
        String contentType = "pdf";

        DocumentUploadCommand command =
                new DocumentUploadCommand("physics-notes.pdf", contentType, 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("contentType", "'" + contentType + "' is not a valid content type"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenContentTypeIsNotRecognised() {
        String contentType = "application/x-not-real";

        DocumentUploadCommand command =
                new DocumentUploadCommand("physics-notes.pdf", contentType, 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("contentType", "'" + contentType + "' is not a supported content type"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenContentTypeIsRecognisedButNotAccepted() {
        String contentType = "application/zip";

        DocumentUploadCommand command = new DocumentUploadCommand("archive.zip", contentType, 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("contentType", "'" + contentType + "' is not a supported content type"),
                exception.error());
    }

    @Test
    void stageUploadAcceptsAContentTypeThatOnlyMatchesOnceLowercased() {
        String filename = "diagram.PNG";

        DocumentUploadCommand command = new DocumentUploadCommand(filename, "IMAGE/PNG", 1024, "AQA GCSE Physics");

        StagedDocumentUpload staged = stager.stageUpload(command);

        assertEquals(MediaType.IMAGE_PNG_TYPE, staged.contentType());
        assertEquals(filename, staged.filename());
        assertTrue(staged.key().matches(RegexUtils.uuidWithExtension("png")), "key was " + staged.key());
    }

    @Test
    void stageUploadThrowsWhenFilenameHasNoExtensionSeparator() {
        DocumentUploadCommand command =
                new DocumentUploadCommand("physics-notes", "application/pdf", 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(new EntityValidationError("filename", "must have a file extension"), exception.error());
    }

    @Test
    void stageUploadThrowsWhenFilenameEndsWithTheExtensionSeparator() {
        DocumentUploadCommand command =
                new DocumentUploadCommand("physics-notes.", "application/pdf", 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(new EntityValidationError("filename", "must have a file extension"), exception.error());
    }

    @Test
    void stageUploadThrowsWhenNoContentTypeIsKnownForTheExtension() {
        String extension = "dat";

        DocumentUploadCommand command =
                new DocumentUploadCommand("physics-notes." + extension, "application/pdf", 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError("filename", "'." + extension + "' is not a recognised file extension"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenTheExtensionsContentTypeIsNotAccepted() {
        String extension = "zip";

        DocumentUploadCommand command =
                new DocumentUploadCommand("archive." + extension, "application/pdf", 1024, "AQA GCSE Physics");

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

        DocumentUploadCommand command =
                new DocumentUploadCommand("diagram." + extension, contentType, 1024, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(command));

        assertEquals(
                new EntityValidationError(
                        "contentType", "'" + contentType + "' does not match the '." + extension + "' file extension"),
                exception.error());
    }
}
