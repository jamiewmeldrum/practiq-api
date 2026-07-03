package com.practiq.domain.query;

import com.practiq.domain.Question;
import com.practiq.domain.types.QuestionStatus;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;
import jakarta.persistence.criteria.JoinType;

@Singleton
public class QuestionSpecificationFactory {

    public QuerySpecification<Question> from(QuestionQuery query) {
        return fetchConceptLinks().and(hasStatus(query.status()));
    }

    private static QuerySpecification<Question> hasStatus(QuestionStatus status) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get("status"), status);
    }

    private static QuerySpecification<Question> fetchConceptLinks() {
        return (root, criteriaQuery, cb) -> {
            root.fetch("conceptLinks", JoinType.LEFT);
            return cb.conjunction();  // no filter, this spec exists purely for the fetch side-effect
        };
    }
}
