package com.practiq.dto.response;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record UploadDocumentResponse(long id, String url, Instant expiresAt) {}
