package com.practiq.web.dto.request;

import com.practiq.persistence.DocumentEntity;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Serdeable
public record PostDocumentRequest(
        @NotBlank @Size(max = DocumentEntity.FILENAME_MAX_LENGTH) String filename,
        @NotNull @Min(1) Integer contentLength,
        @NotBlank String contentType,
        @Nullable @Size(max = DocumentEntity.SOURCE_SPEC_MAX_LENGTH) String sourceSpec) {}
