package com.practiq.dto;

import com.practiq.domain.types.QuestionDifficulty;
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
public class QuestionDto {
    private final long id;
    private final String body;
    private final String markScheme;
    private final QuestionDifficulty difficulty;
    private final QuestionType type;
    private final QuestionSource source;
    private final QuestionStatus status;
    private final String sourceSpec;
    private final Instant createdAt;
    private final Set<Long> linkedConceptIds;

    public QuestionDto(
            long id,
            String body,
            String markScheme,
            QuestionDifficulty difficulty,
            QuestionType type,
            QuestionSource source,
            QuestionStatus status,
            String sourceSpec,
            Instant createdAt,
            Set<Long> linkedConceptIds
    ) {
        this.id = id;
        this.body = body;
        this.markScheme = markScheme;
        this.difficulty = difficulty;
        this.type = type;
        this.source = source;
        this.status = status;
        this.sourceSpec = sourceSpec;
        this.createdAt = createdAt;
        this.linkedConceptIds = linkedConceptIds;
    }
}
