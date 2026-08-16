package com.practiq.service.question.dto.response;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import java.time.Instant;
import java.util.Set;

public record Question(
        long id,
        int version,
        String body,
        QuestionDifficulty difficulty,
        QuestionType type,
        QuestionStatus status,
        Instant createdAt,
        Set<Long> linkedConceptIds) {}
