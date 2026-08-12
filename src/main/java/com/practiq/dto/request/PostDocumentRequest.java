package com.practiq.dto.request;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Serdeable
public record PostDocumentRequest(
        @NotBlank String filename,
        @Min(1) @Max(10000) long contentLength, // TODO - consider limits
        @NotBlank String contentType, // TODO - maybe make MediaType
        @Nullable String sourceSpec) {}
