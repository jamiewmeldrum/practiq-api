package com.practiq.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micronaut.http.MediaType;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PresignedUploadRequestTest {

    private static final String KEY = "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf";

    @Test
    void aRequestHoldsTheValuesItWasBuiltWith() {
        int contentLength = 2048;
        Duration expiresIn = Duration.ofMinutes(10);

        PresignedUploadRequest request =
                new PresignedUploadRequest(KEY, MediaType.APPLICATION_PDF_TYPE, contentLength, expiresIn);

        assertEquals(KEY, request.key());
        assertEquals(MediaType.APPLICATION_PDF_TYPE, request.contentType());
        assertEquals(contentLength, request.contentLength());
        assertEquals(expiresIn, request.expiresIn());
    }

    @Test
    void aRequestCannotBeBuiltWithoutAKey() {
        IllegalArgumentException nullKey = assertThrows(
                IllegalArgumentException.class,
                () -> new PresignedUploadRequest(null, MediaType.APPLICATION_PDF_TYPE, 2048, Duration.ofMinutes(10)));

        assertEquals("key must not be blank", nullKey.getMessage());

        IllegalArgumentException emptyKey = assertThrows(
                IllegalArgumentException.class,
                () -> new PresignedUploadRequest("", MediaType.APPLICATION_PDF_TYPE, 2048, Duration.ofMinutes(10)));

        assertEquals("key must not be blank", emptyKey.getMessage());

        IllegalArgumentException whitespaceKey = assertThrows(
                IllegalArgumentException.class,
                () -> new PresignedUploadRequest("   ", MediaType.APPLICATION_PDF_TYPE, 2048, Duration.ofMinutes(10)));

        assertEquals("key must not be blank", whitespaceKey.getMessage());
    }

    @Test
    void aRequestCannotBeBuiltWithoutAContentType() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new PresignedUploadRequest(KEY, null, 2048, Duration.ofMinutes(10)));

        assertEquals("contentType must not be null", exception.getMessage());
    }

    @Test
    void aRequestCannotBeBuiltWithAContentLengthBelowOne() {
        IllegalArgumentException zero = assertThrows(
                IllegalArgumentException.class,
                () -> new PresignedUploadRequest(KEY, MediaType.APPLICATION_PDF_TYPE, 0, Duration.ofMinutes(10)));

        assertEquals("contentLength must be greater than or equal to 1", zero.getMessage());

        IllegalArgumentException negative = assertThrows(
                IllegalArgumentException.class,
                () -> new PresignedUploadRequest(KEY, MediaType.APPLICATION_PDF_TYPE, -1, Duration.ofMinutes(10)));

        assertEquals("contentLength must be greater than or equal to 1", negative.getMessage());
    }

    @Test
    void aRequestCannotBeBuiltWithoutAPositiveExpiry() {
        IllegalArgumentException nullExpiry = assertThrows(
                IllegalArgumentException.class,
                () -> new PresignedUploadRequest(KEY, MediaType.APPLICATION_PDF_TYPE, 2048, null));

        assertEquals("expiresIn must be a positive duration", nullExpiry.getMessage());

        IllegalArgumentException zeroExpiry = assertThrows(
                IllegalArgumentException.class,
                () -> new PresignedUploadRequest(KEY, MediaType.APPLICATION_PDF_TYPE, 2048, Duration.ZERO));

        assertEquals("expiresIn must be a positive duration", zeroExpiry.getMessage());

        IllegalArgumentException negativeExpiry = assertThrows(
                IllegalArgumentException.class,
                () -> new PresignedUploadRequest(KEY, MediaType.APPLICATION_PDF_TYPE, 2048, Duration.ofMinutes(-1)));

        assertEquals("expiresIn must be a positive duration", negativeExpiry.getMessage());
    }
}
