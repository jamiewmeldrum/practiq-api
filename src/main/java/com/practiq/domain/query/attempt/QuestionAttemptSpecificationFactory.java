package com.practiq.domain.query.attempt;

import com.practiq.domain.QuestionAttempt;
import com.practiq.domain.QuestionAttempt_;
import com.practiq.domain.query.QueryRestriction;
import com.practiq.domain.query.QuerySpecificationFactory;
import com.practiq.domain.query.SessionTokenRestriction;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class QuestionAttemptSpecificationFactory
        extends QuerySpecificationFactory<QuestionAttempt, QuestionAttemptQuery> {

    @Override
    protected QuerySpecification<QuestionAttempt> applyDomain(
            QuerySpecification<QuestionAttempt> specification,
            QuestionAttemptQuery query) {
        return specification.and(hasQuestionId(query.questionId()));
    }

    @Override
    protected List<QueryRestriction<QuestionAttempt, ? super QuestionAttemptQuery>> restrictions() {
        return List.of(new SessionTokenRestriction<>(QuestionAttempt_.sessionToken));
    }

    private QuerySpecification<QuestionAttempt> hasQuestionId(long questionId) {
        return (root, criteriaQuery, cb) ->
                cb.equal(root.get(QuestionAttempt_.questionId), questionId);
    }
}
