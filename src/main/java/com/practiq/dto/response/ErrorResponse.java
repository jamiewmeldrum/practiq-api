package com.practiq.dto.response;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ErrorResponse(String error, int status) {
}
