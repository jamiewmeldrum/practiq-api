package com.practiq.persistence.query.attempt;

import com.practiq.persistence.QuestionAttemptEntity;
import com.practiq.persistence.repository.QuestionAttemptRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;
import java.util.List;

// Runs the query it is handed. The session-token restriction is not applied here by choice — it rides on
// QuestionAttemptQuery implementing UserRestrictedQuery, which the specification factory acts on
// unconditionally, so an attempt query that escapes it cannot be constructed.
@Singleton
public class QuestionAttemptQueryRunner {

    // Newest first, with id as a tiebreak so attempts made in the same instant have a defined order.
    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    private final QuestionAttemptSpecificationFactory questionAttemptSpecificationFactory;
    private final QuestionAttemptRepository questionAttemptRepository;

    public QuestionAttemptQueryRunner(
            QuestionAttemptRepository questionAttemptRepository,
            QuestionAttemptSpecificationFactory questionAttemptSpecificationFactory) {
        this.questionAttemptRepository = questionAttemptRepository;
        this.questionAttemptSpecificationFactory = questionAttemptSpecificationFactory;
    }

    public List<QuestionAttemptEntity> findAll(QuestionAttemptQuery query) {
        QuerySpecification<QuestionAttemptEntity> spec = questionAttemptSpecificationFactory.forQuery(query);
        return questionAttemptRepository.findAll(spec, STABLE_ORDER);
    }
}
