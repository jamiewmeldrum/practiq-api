package com.practiq.service.question.policy;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import com.practiq.persistence.query.question.QuestionQuery;
import jakarta.inject.Singleton;
import java.util.List;

// Students see approved questions that are linked to a concept, and nothing else. Request filters narrow
// that further; they can never widen it, because every query starts from this builder.
@Singleton
public class StudentQuestionQueryPolicy implements QuestionQueryPolicy {

    @Override
    public QuestionQuery forId(long questionId) {
        return studentCatalogueBuilder().questionId(questionId).build();
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
        return QuestionQuery.builder().status(QuestionStatus.APPROVED).requiresConceptLink(true);
    }
}
