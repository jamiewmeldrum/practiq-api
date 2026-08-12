package com.practiq.dto.response;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record UploadDocumentResponse(long id, String url) {}
