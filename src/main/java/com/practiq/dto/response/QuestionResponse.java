package com.practiq.dto.response;

import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
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
    private final QuestionSource source;
    private final QuestionStatus status;
    private final String sourceSpec;
    private final Instant createdAt;
    private final Set<Long> linkedConceptIds;

    public QuestionResponse(
            long id,
            String body,
            QuestionDifficultyResponse difficulty,
            QuestionType type,
            QuestionSource source,
            QuestionStatus status,
            String sourceSpec,
            Instant createdAt,
            Set<Long> linkedConceptIds
    ) {
        this.id = id;
        this.body = body;
        this.difficulty =  difficulty;
        this.type = type;
        this.source = source;
        this.status = status;
        this.sourceSpec = sourceSpec;
        this.createdAt = createdAt;
        this.linkedConceptIds = linkedConceptIds;
    }
}
