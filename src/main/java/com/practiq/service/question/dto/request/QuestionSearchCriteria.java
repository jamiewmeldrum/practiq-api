package com.practiq.service.question.dto.request;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionType;
import java.util.List;

public record QuestionSearchCriteria(List<QuestionType> types, List<QuestionDifficulty> difficulties, Long conceptId) {
    public QuestionSearchCriteria {
        if (conceptId != null && conceptId < 1) {
            throw new IllegalArgumentException("conceptId must be greater than 0");
        }
    }
}
