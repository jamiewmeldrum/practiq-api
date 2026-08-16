package com.practiq.service.concept.dto.response;

import java.time.Instant;

public record Concept(long id, int version, String name, String description, Instant createdAt) {}
