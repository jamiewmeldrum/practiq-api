package com.practiq.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;

import java.time.Instant;

@Serdeable
@Getter
public class ConceptDto {

    private final long id;
    private final String name;
    private final String description;
    private final Instant createdAt;

    public ConceptDto(
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

    @Override
    public String toString() {
        return "ConceptDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", created_at='" + createdAt + '\'' +
                '}';
    }
}
