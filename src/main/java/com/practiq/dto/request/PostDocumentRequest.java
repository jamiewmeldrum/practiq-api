package com.practiq.dto.request;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Serdeable
public record PostDocumentRequest(
        @NotBlank String filename,
        @NotNull @Min(1) Integer contentLength,
        @NotBlank String contentType,
        @Nullable @Size(max = 255) String sourceSpec) {}
