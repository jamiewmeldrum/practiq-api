package com.practiq.web.dto.response;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record QuestionAttemptResponse(long id, long questionId, String body, Instant createdAt) {}
