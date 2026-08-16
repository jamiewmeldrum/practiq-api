package com.practiq.persistence.query.attempt;

import com.practiq.persistence.QuestionAttemptEntity;
import com.practiq.persistence.QuestionAttemptEntity_;
import com.practiq.persistence.query.QueryRestriction;
import com.practiq.persistence.query.QuerySpecificationFactory;
import com.practiq.persistence.query.SessionTokenQueryRestriction;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class QuestionAttemptSpecificationFactory
        extends QuerySpecificationFactory<QuestionAttemptEntity, QuestionAttemptQuery> {

    @Override
    protected QuerySpecification<QuestionAttemptEntity> applyDomain(
            QuerySpecification<QuestionAttemptEntity> specification, QuestionAttemptQuery query) {
        return specification.and(hasQuestionId(query.questionId()));
    }

    @Override
    protected List<QueryRestriction<QuestionAttemptEntity, ? super QuestionAttemptQuery>> restrictions() {
        return List.of(new SessionTokenQueryRestriction<>(QuestionAttemptEntity_.sessionToken));
    }

    private QuerySpecification<QuestionAttemptEntity> hasQuestionId(long questionId) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get(QuestionAttemptEntity_.questionId), questionId);
    }
}
