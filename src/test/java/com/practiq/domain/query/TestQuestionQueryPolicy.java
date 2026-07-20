package com.practiq.domain.query;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionType;

import java.util.List;

// A neutral, policy-free double: it imposes nothing (no APPROVED, no concept-link requirement) and simply
// echoes its inputs into a QuestionQuery. That lets QuestionQueryRunnerTest prove the runner faithfully
// runs whatever the policy returns, without coupling the runner mechanics to any real policy's imposed
// fields. The imposed-field behaviour is a policy concern, tested on the concrete policies.
class TestQuestionQueryPolicy implements QuestionQueryPolicy {

    @Override
    public QuestionQuery forId(long questionId) {
        return QuestionQuery.builder()
                .questionId(questionId)
                .build();
    }

    @Override
    public QuestionQuery catalogue(List<QuestionType> types, List<QuestionDifficulty> difficulties, Long conceptId) {
        return QuestionQuery.builder()
                .types(types)
                .difficulties(difficulties)
                .conceptId(conceptId)
                .build();
    }
}
