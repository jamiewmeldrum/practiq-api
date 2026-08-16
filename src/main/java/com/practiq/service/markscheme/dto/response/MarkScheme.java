package com.practiq.service.markscheme.dto.response;

import java.time.Instant;

public record MarkScheme(long id, int version, long questionId, String body, Instant createdAt) {}
