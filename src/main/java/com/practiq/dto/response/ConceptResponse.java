package com.practiq.dto.response;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Serdeable
@Getter
@ToString
public class ConceptResponse {

    private final long id;
    private final String name;
    private final String description;
    private final Instant createdAt;

    public ConceptResponse(
            long id,
            String name,
            String description,
            Instant createdAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }
}
