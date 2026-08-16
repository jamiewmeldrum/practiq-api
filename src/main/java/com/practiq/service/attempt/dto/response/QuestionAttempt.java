package com.practiq.service.attempt.dto.response;

import java.time.Instant;

public record QuestionAttempt(long id, long questionId, String sessionToken, String body, Instant createdAt) {}
