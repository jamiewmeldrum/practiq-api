package com.practiq.service.document;

import io.micronaut.http.MediaType;

public record StagedDocumentUpload(
        String key, String filename, String sourceSpec, MediaType contentType, long contentLength) {

    // Facts about any staged upload, however it was built — not the request rules, which stay in
    // DocumentStager. Reaching here with a bad value means a caller has a bug, so these fail loudly
    // rather than becoming an EntityValidationException the client would see as a 422.
    public StagedDocumentUpload {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename must not be blank");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        if (contentLength < 1) {
            throw new IllegalArgumentException("contentLength must be greater than or equal to 1");
        }
    }
}
