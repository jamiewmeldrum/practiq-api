package com.practiq.dto.request;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionType;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.validation.annotation.UniqueElements;

import java.util.List;

@Introspected
public record QuestionRequest(
        @Nullable @UniqueElements List<QuestionType> types,
        @Nullable @UniqueElements List<QuestionDifficulty> difficulties,
        @Nullable Long conceptId
) {
}
