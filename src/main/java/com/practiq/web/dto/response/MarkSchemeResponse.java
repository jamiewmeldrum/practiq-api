package com.practiq.web.dto.response;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record MarkSchemeResponse(long id, long questionId, String body, Instant createdAt) {}
