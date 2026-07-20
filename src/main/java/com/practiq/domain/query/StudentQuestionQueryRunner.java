package com.practiq.domain.query;

import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import jakarta.inject.Singleton;

@Singleton
public class StudentQuestionQueryRunner extends QuestionQueryRunner<StudentQuestionQueryPolicy> {
    public StudentQuestionQueryRunner(
            QuestionRepository questionRepository,
            QuestionConceptRepository questionConceptRepository,
            QuestionSpecificationFactory questionSpecificationFactory) {
        super(questionRepository, questionConceptRepository, questionSpecificationFactory, new StudentQuestionQueryPolicy());
    }
}
