package com.practiq.domain.query.question;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;

import java.util.List;

class StudentQuestionQueryPolicy implements QuestionQueryPolicy {

    @Override
    public QuestionQuery forId(long questionId) {
        return studentCatalogueBuilder()
                .questionId(questionId)
                .build();
    }

    @Override
    public QuestionQuery catalogue(List<QuestionType> types, List<QuestionDifficulty> difficulties, Long conceptId) {
        return studentCatalogueBuilder()
                .types(types)
                .difficulties(difficulties)
                .conceptId(conceptId)
                .build();
    }

    private QuestionQuery.QuestionQueryBuilder studentCatalogueBuilder() {
        return QuestionQuery.builder()
                .status(QuestionStatus.APPROVED)
                .requiresConceptLink(true);
    }
}
