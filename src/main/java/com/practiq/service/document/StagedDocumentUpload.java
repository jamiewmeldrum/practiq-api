package com.practiq.service.document;

import io.micronaut.http.MediaType;

public record StagedDocumentUpload(
        String key, String filename, String sourceSpec, MediaType contentType, int contentLength) {}
