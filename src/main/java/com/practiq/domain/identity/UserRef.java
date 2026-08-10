package com.practiq.domain.identity;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;

// The user an operation is about — not necessarily the caller. @Introspected is load bearing:
// the validator is reflection-free, so a cascaded @Valid on a non-introspected type silently
// validates nothing.
@Introspected
public record UserRef(@NotBlank String sessionToken) {}
