package com.practiq.domain.query.question;

import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;

class TestQuestionQueryRunner extends QuestionQueryRunner<TestQuestionQueryPolicy> {
    protected TestQuestionQueryRunner(
            QuestionRepository questionRepository,
            QuestionConceptRepository questionConceptRepository,
            QuestionSpecificationFactory questionSpecificationFactory) {
        super(questionRepository, questionConceptRepository, questionSpecificationFactory, new TestQuestionQueryPolicy());
    }
}
