package com.practiq.web.dto.request;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionType;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.validation.annotation.UniqueElements;
import jakarta.validation.constraints.Min;
import java.util.List;

@Introspected
public record QuestionRequest(
        @Nullable @UniqueElements List<QuestionType> types,
        @Nullable @UniqueElements List<QuestionDifficulty> difficulties,
        @Nullable @Min(1) Long conceptId) {}
