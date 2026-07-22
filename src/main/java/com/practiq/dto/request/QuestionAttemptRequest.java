package com.practiq.dto.request;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Serdeable
public record QuestionAttemptRequest(@NotBlank @Size(max = 20000) String body) {
}
