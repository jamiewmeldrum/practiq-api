package com.practiq.service.document;

import static com.practiq.service.document.DocumentStager.MAX_CONTENT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.practiq.dto.request.PostDocumentRequest;
import com.practiq.exception.EntityValidationError;
import com.practiq.exception.EntityValidationException;
import io.micronaut.http.MediaType;
import io.micronaut.http.exceptions.ContentLengthExceededException;
import org.junit.jupiter.api.Test;
import utils.RegexUtils;

class DocumentStagerTest {

    private final DocumentStager stager = new DocumentStager();

    @Test
    void stageUploadStagesTheUpload() {
        String filename = "physics-notes.pdf";
        int contentLength = 2048;
        String sourceSpec = "AQA GCSE Physics";
        PostDocumentRequest request = new PostDocumentRequest(filename, contentLength, "application/pdf", sourceSpec);

        StagedDocumentUpload staged = stager.stageUpload(request);

        assertTrue(staged.key().matches(RegexUtils.uuidWithExtension("pdf")), "key was " + staged.key());
        assertEquals(filename, staged.filename());
        assertEquals(sourceSpec, staged.sourceSpec());
        assertEquals(MediaType.APPLICATION_PDF_TYPE, staged.contentType());
        assertEquals(contentLength, staged.contentLength());
    }

    @Test
    void stageUploadStagesTheUploadWhenContentLengthIsAtTheMaximum() {
        String filename = "physics-notes.pdf";
        int contentLength = MAX_CONTENT_LENGTH;
        String sourceSpec = "AQA GCSE Physics";
        PostDocumentRequest request = new PostDocumentRequest(filename, contentLength, "application/pdf", sourceSpec);

        StagedDocumentUpload staged = stager.stageUpload(request);

        assertTrue(staged.key().matches(RegexUtils.uuidWithExtension("pdf")), "key was " + staged.key());
        assertEquals(filename, staged.filename());
        assertEquals(sourceSpec, staged.sourceSpec());
        assertEquals(MediaType.APPLICATION_PDF_TYPE, staged.contentType());
        assertEquals(contentLength, staged.contentLength());
    }

    @Test
    void stageUploadThrowsWhenContentLengthIsOneOverTheMaximum() {
        int contentLength = MAX_CONTENT_LENGTH + 1;
        PostDocumentRequest request =
                new PostDocumentRequest("physics-notes.pdf", contentLength, "application/pdf", "AQA GCSE Physics");

        ContentLengthExceededException exception =
                assertThrows(ContentLengthExceededException.class, () -> stager.stageUpload(request));

        assertEquals(
                "Length of content (" + contentLength + ") is greater than " + MAX_CONTENT_LENGTH,
                exception.getMessage());
    }

    @Test
    void stageUploadThrowsWhenContentTypeIsMissing() {
        PostDocumentRequest nullContentType =
                new PostDocumentRequest("physics-notes.pdf", 1024, null, "AQA GCSE Physics");
        EntityValidationException nullFailure =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(nullContentType));
        assertEquals(new EntityValidationError("contentType", "must not be blank"), nullFailure.error());

        PostDocumentRequest emptyContentType =
                new PostDocumentRequest("physics-notes.pdf", 1024, "", "AQA GCSE Physics");
        EntityValidationException emptyFailure =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(emptyContentType));
        assertEquals(new EntityValidationError("contentType", "must not be blank"), emptyFailure.error());

        PostDocumentRequest whitespaceContentType =
                new PostDocumentRequest("physics-notes.pdf", 1024, "   ", "AQA GCSE Physics");
        EntityValidationException whitespaceFailure =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(whitespaceContentType));
        assertEquals(new EntityValidationError("contentType", "must not be blank"), whitespaceFailure.error());
    }

    @Test
    void stageUploadThrowsWhenContentTypeCannotBeParsed() {
        String contentType = "pdf";
        PostDocumentRequest request =
                new PostDocumentRequest("physics-notes.pdf", 1024, contentType, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(request));

        assertEquals(
                new EntityValidationError("contentType", "'" + contentType + "' is not a valid content type"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenContentTypeIsNotRecognised() {
        String contentType = "application/x-not-real";
        PostDocumentRequest request =
                new PostDocumentRequest("physics-notes.pdf", 1024, contentType, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(request));

        assertEquals(
                new EntityValidationError("contentType", "'" + contentType + "' is not a supported content type"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenContentTypeIsRecognisedButNotAccepted() {
        String contentType = "application/zip";
        PostDocumentRequest request = new PostDocumentRequest("archive.zip", 1024, contentType, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(request));

        assertEquals(
                new EntityValidationError("contentType", "'" + contentType + "' is not a supported content type"),
                exception.error());
    }

    @Test
    void stageUploadAcceptsAContentTypeThatOnlyMatchesOnceLowercased() {
        String filename = "diagram.PNG";
        PostDocumentRequest request = new PostDocumentRequest(filename, 1024, "IMAGE/PNG", "AQA GCSE Physics");

        StagedDocumentUpload staged = stager.stageUpload(request);

        assertEquals(MediaType.IMAGE_PNG_TYPE, staged.contentType());
        assertEquals(filename, staged.filename());
        assertTrue(staged.key().matches(RegexUtils.uuidWithExtension("png")), "key was " + staged.key());
    }

    @Test
    void stageUploadThrowsWhenFilenameIsMissing() {
        PostDocumentRequest nullFilename = new PostDocumentRequest(null, 1024, "application/pdf", "AQA GCSE Physics");
        EntityValidationException nullFailure =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(nullFilename));
        assertEquals(new EntityValidationError("filename", "must not be blank"), nullFailure.error());

        PostDocumentRequest emptyFilename = new PostDocumentRequest("", 1024, "application/pdf", "AQA GCSE Physics");
        EntityValidationException emptyFailure =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(emptyFilename));
        assertEquals(new EntityValidationError("filename", "must not be blank"), emptyFailure.error());

        PostDocumentRequest whitespaceFilename =
                new PostDocumentRequest("   ", 1024, "application/pdf", "AQA GCSE Physics");
        EntityValidationException whitespaceFailure =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(whitespaceFilename));
        assertEquals(new EntityValidationError("filename", "must not be blank"), whitespaceFailure.error());
    }

    @Test
    void stageUploadThrowsWhenFilenameHasNoExtensionSeparator() {
        PostDocumentRequest request =
                new PostDocumentRequest("physics-notes", 1024, "application/pdf", "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(request));

        assertEquals(new EntityValidationError("filename", "must have a file extension"), exception.error());
    }

    @Test
    void stageUploadThrowsWhenFilenameEndsWithTheExtensionSeparator() {
        PostDocumentRequest request =
                new PostDocumentRequest("physics-notes.", 1024, "application/pdf", "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(request));

        assertEquals(new EntityValidationError("filename", "must have a file extension"), exception.error());
    }

    @Test
    void stageUploadThrowsWhenNoContentTypeIsKnownForTheExtension() {
        String extension = "dat";
        PostDocumentRequest request =
                new PostDocumentRequest("physics-notes." + extension, 1024, "application/pdf", "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(request));

        assertEquals(
                new EntityValidationError("filename", "'." + extension + "' is not a recognised file extension"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenTheExtensionsContentTypeIsNotAccepted() {
        String extension = "zip";
        PostDocumentRequest request =
                new PostDocumentRequest("archive." + extension, 1024, "application/pdf", "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(request));

        assertEquals(
                new EntityValidationError("filename", "'." + extension + "' files are not supported"),
                exception.error());
    }

    @Test
    void stageUploadThrowsWhenTheDeclaredAndDerivedContentTypesAreBothAcceptedButDiffer() {
        String contentType = "application/pdf";
        String extension = "png";
        PostDocumentRequest request =
                new PostDocumentRequest("diagram." + extension, 1024, contentType, "AQA GCSE Physics");

        EntityValidationException exception =
                assertThrows(EntityValidationException.class, () -> stager.stageUpload(request));

        assertEquals(
                new EntityValidationError(
                        "contentType", "'" + contentType + "' does not match the '." + extension + "' file extension"),
                exception.error());
    }
}
