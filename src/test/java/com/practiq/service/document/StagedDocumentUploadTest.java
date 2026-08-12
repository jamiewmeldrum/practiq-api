package com.practiq.service.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micronaut.http.MediaType;
import org.junit.jupiter.api.Test;

class StagedDocumentUploadTest {

    @Test
    void aStagedUploadHoldsTheValuesItWasBuiltWith() {
        String key = "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf";
        String filename = "physics-notes.pdf";
        String sourceSpec = "AQA GCSE Physics";
        int contentLength = 2048;

        StagedDocumentUpload staged =
                new StagedDocumentUpload(key, filename, sourceSpec, MediaType.APPLICATION_PDF_TYPE, contentLength);

        assertEquals(key, staged.key());
        assertEquals(filename, staged.filename());
        assertEquals(sourceSpec, staged.sourceSpec());
        assertEquals(MediaType.APPLICATION_PDF_TYPE, staged.contentType());
        assertEquals(contentLength, staged.contentLength());
    }

    @Test
    void sourceSpecIsOptional() {
        StagedDocumentUpload staged = new StagedDocumentUpload(
                "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf",
                "physics-notes.pdf",
                null,
                MediaType.APPLICATION_PDF_TYPE,
                2048);

        assertNull(staged.sourceSpec());
    }

    @Test
    void aStagedUploadCannotBeBuiltWithoutAKey() {
        IllegalArgumentException nullKey = assertThrows(
                IllegalArgumentException.class,
                () -> new StagedDocumentUpload(
                        null, "physics-notes.pdf", "AQA GCSE Physics", MediaType.APPLICATION_PDF_TYPE, 2048));
        assertEquals("key must not be blank", nullKey.getMessage());

        IllegalArgumentException blankKey = assertThrows(
                IllegalArgumentException.class,
                () -> new StagedDocumentUpload(
                        "   ", "physics-notes.pdf", "AQA GCSE Physics", MediaType.APPLICATION_PDF_TYPE, 2048));
        assertEquals("key must not be blank", blankKey.getMessage());
    }

    @Test
    void aStagedUploadCannotBeBuiltWithoutAFilename() {
        IllegalArgumentException nullFilename = assertThrows(
                IllegalArgumentException.class,
                () -> new StagedDocumentUpload(
                        "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf",
                        null,
                        "AQA GCSE Physics",
                        MediaType.APPLICATION_PDF_TYPE,
                        2048));
        assertEquals("filename must not be blank", nullFilename.getMessage());

        IllegalArgumentException blankFilename = assertThrows(
                IllegalArgumentException.class,
                () -> new StagedDocumentUpload(
                        "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf",
                        "   ",
                        "AQA GCSE Physics",
                        MediaType.APPLICATION_PDF_TYPE,
                        2048));
        assertEquals("filename must not be blank", blankFilename.getMessage());
    }

    @Test
    void aStagedUploadCannotBeBuiltWithoutAContentType() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new StagedDocumentUpload(
                        "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf",
                        "physics-notes.pdf",
                        "AQA GCSE Physics",
                        null,
                        2048));

        assertEquals("contentType must not be null", exception.getMessage());
    }

    @Test
    void aStagedUploadCannotBeBuiltWithAContentLengthBelowOne() {
        IllegalArgumentException zero = assertThrows(
                IllegalArgumentException.class,
                () -> new StagedDocumentUpload(
                        "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf",
                        "physics-notes.pdf",
                        "AQA GCSE Physics",
                        MediaType.APPLICATION_PDF_TYPE,
                        0));
        assertEquals("contentLength must be greater than or equal to 1", zero.getMessage());

        IllegalArgumentException negative = assertThrows(
                IllegalArgumentException.class,
                () -> new StagedDocumentUpload(
                        "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf",
                        "physics-notes.pdf",
                        "AQA GCSE Physics",
                        MediaType.APPLICATION_PDF_TYPE,
                        -1));
        assertEquals("contentLength must be greater than or equal to 1", negative.getMessage());
    }
}
