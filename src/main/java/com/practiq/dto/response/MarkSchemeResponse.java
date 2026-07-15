package com.practiq.dto.response;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

//TODO - explicit tests
@Serdeable
@Getter
@ToString
public class MarkSchemeResponse {

    private final long id;
    private final long questionId;
    private final String body;
    private final Instant createdAt;

    public MarkSchemeResponse(
            long id,
            long questionId,
            String body,
            Instant createdAt
    ) {
        this.id = id;
        this.questionId = questionId;
        this.body = body;
        this.createdAt = createdAt;
    }
}
