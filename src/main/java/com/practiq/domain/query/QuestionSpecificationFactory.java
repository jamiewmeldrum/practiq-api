package com.practiq.domain.query;

import com.practiq.domain.Question;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;
import jakarta.persistence.criteria.JoinType;

import java.util.List;

@Singleton
public class QuestionSpecificationFactory {

    public QuerySpecification<Question> from(QuestionQuery query) {
        QuerySpecification<Question> specification = fetchConceptLinks().and(hasStatus(query.status()));

        if (!query.types().isEmpty()) {
            specification = specification.and(isInQuestionTypes(query.types()));
        }

        return specification;
    }

    private QuerySpecification<Question> fetchConceptLinks() {
        return (root, criteriaQuery, cb) -> {
            root.fetch("conceptLinks", JoinType.LEFT);
            return cb.conjunction();  // no filter, this spec exists purely for the fetch side-effect
        };
    }

    private QuerySpecification<Question> hasStatus(QuestionStatus status) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get("status"), status);
    }

    private QuerySpecification<Question> isInQuestionTypes(List<QuestionType> types) {
        return (root, criteriaQuery, cb) -> root.get("type").in(types);
    }
}
