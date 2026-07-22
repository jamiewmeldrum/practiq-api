package com.practiq.dto.request;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;

@Introspected
public record QuestionAttemptRequest(@NotBlank String body) {
}
