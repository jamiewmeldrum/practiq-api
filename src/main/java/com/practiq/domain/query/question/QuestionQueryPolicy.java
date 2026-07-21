package com.practiq.domain.query.question;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionType;

import java.util.List;

interface QuestionQueryPolicy {
    QuestionQuery forId(long questionId);

    QuestionQuery catalogue(
            List<QuestionType> types,
            List<QuestionDifficulty> difficulties,
            Long conceptId);
}
