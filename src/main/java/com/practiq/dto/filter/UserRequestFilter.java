package com.practiq.dto.filter;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;

@Introspected
public record UserRequestFilter(@NotBlank String sessionToken) {}
