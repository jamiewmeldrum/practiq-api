package com.practiq.domain.query.attempt;

import com.practiq.domain.QuestionAttempt;
import com.practiq.dto.filter.UserRequestFilter;
import com.practiq.repository.QuestionAttemptRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class QuestionAttemptQueryRunner {

    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    private final QuestionAttemptSpecificationFactory questionAttemptSpecificationFactory;
    private final QuestionAttemptRepository questionAttemptRepository;

    public QuestionAttemptQueryRunner(
            QuestionAttemptRepository questionAttemptRepository,
            QuestionAttemptSpecificationFactory questionAttemptSpecificationFactory
    ) {
        this.questionAttemptRepository = questionAttemptRepository;
        this.questionAttemptSpecificationFactory = questionAttemptSpecificationFactory;
    }

    public List<QuestionAttempt> getQuestionAttempts(
            UserRequestFilter userRequestFilter,
            long questionId) {
        QuestionAttemptQuery questionAttemptQuery = new QuestionAttemptQuery(questionId, userRequestFilter.sessionToken());
        QuerySpecification<QuestionAttempt> spec = questionAttemptSpecificationFactory.forQuery(questionAttemptQuery);

        return questionAttemptRepository.findAll(spec, STABLE_ORDER);
    }
}
