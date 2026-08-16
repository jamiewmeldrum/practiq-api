package com.practiq.web.dto.response;

import com.practiq.foundation.types.QuestionType;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;
import java.util.Set;

@Serdeable
public record QuestionResponse(
        long id,
        String body,
        QuestionDifficultyResponse difficulty,
        QuestionType type,
        Instant createdAt,
        Set<Long> linkedConceptIds) {}
