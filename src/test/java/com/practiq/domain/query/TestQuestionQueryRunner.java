package com.practiq.domain.query;

import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;

public class TestQuestionQueryRunner extends QuestionQueryRunner<TestQuestionQueryPolicy> {
    protected TestQuestionQueryRunner(
            QuestionRepository questionRepository,
            QuestionConceptRepository questionConceptRepository,
            QuestionSpecificationFactory questionSpecificationFactory) {
        super(questionRepository, questionConceptRepository, questionSpecificationFactory, new TestQuestionQueryPolicy());
    }
}
