package com.practiq.web.dto.response;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record ConceptResponse(long id, String name, String description, Instant createdAt) {}
