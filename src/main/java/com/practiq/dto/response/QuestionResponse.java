package com.practiq.dto.response;

import com.practiq.domain.types.QuestionType;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Set;

@Serdeable
@Getter
@ToString
public class QuestionResponse {
    private final long id;
    private final String body;
    private final QuestionDifficultyResponse difficulty;
    private final QuestionType type;
    private final Instant createdAt;
    private final Set<Long> linkedConceptIds;

    public QuestionResponse(
            long id,
            String body,
            QuestionDifficultyResponse difficulty,
            QuestionType type,
            Instant createdAt,
            Set<Long> linkedConceptIds
    ) {
        this.id = id;
        this.body = body;
        this.difficulty =  difficulty;
        this.type = type;
        this.createdAt = createdAt;
        this.linkedConceptIds = linkedConceptIds;
    }
}
