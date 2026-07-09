package com.practiq.domain.query;

import com.practiq.domain.Question;
import com.practiq.domain.QuestionConcept;
import com.practiq.domain.QuestionConcept_;
import com.practiq.domain.QuestionConceptId_;
import com.practiq.domain.Question_;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.List;

@Singleton
public class QuestionSpecificationFactory {

    // Produces only the WHERE predicate. Ordering and the concept-link load live outside the spec now
    // (in the service), so this no longer fetch-joins, no longer needs distinct, and no longer has to
    // guard against the paged count query.
    public QuerySpecification<Question> forQuery(QuestionQuery query) {
        QuerySpecification<Question> specification = hasStatus(query.status());

        if (!query.types().isEmpty()) {
            specification = specification.and(isInQuestionTypes(query.types()));
        }

        if (!query.difficulties().isEmpty()) {
            specification = specification.and(isInQuestionDifficulties(query.difficulties()));
        }

        // Serving policy comes from the query, not this factory: a conceptId filter implies a link, and
        // otherwise the link requirement applies only when the query demands it (student catalogue does;
        // an admin review query must be able to see unlinked questions).
        if (query.conceptId() != null) {
            specification = specification.and(hasConceptForId(query.conceptId()));
        } else if (query.requiresConceptLink()) {
            specification = specification.and(hasConcept());
        }
        return specification;
    }

    private QuerySpecification<Question> hasStatus(QuestionStatus status) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get(Question_.status), status);
    }

    private QuerySpecification<Question> isInQuestionTypes(List<QuestionType> types) {
        return (root, criteriaQuery, cb) -> root.get(Question_.type).in(types);
    }

    private QuerySpecification<Question> isInQuestionDifficulties(List<QuestionDifficulty> difficulties) {
        return (root, criteriaQuery, cb) -> root.get(Question_.difficulty).in(difficulties);
    }

    // Filter on the to-many via EXISTS, deliberately NOT a join: joining question_concept would
    // multiply question rows (one per link) and corrupt the page count. EXISTS keeps exactly one row
    // per question, so pagination and counting stay correct with no distinct needed. This is why the
    // filter side stays clean even as concept filtering arrives.
    private QuerySpecification<Question> hasConceptForId(long conceptId) {
        return (root, criteriaQuery, cb) -> {
            Subquery<Long> matchingLink = criteriaQuery.subquery(Long.class);
            Root<QuestionConcept> link = matchingLink.from(QuestionConcept.class);
            matchingLink.select(cb.literal(1L)).where(
                    cb.equal(link.get(QuestionConcept_.id).get(QuestionConceptId_.questionId), root.get(Question_.id)),
                    cb.equal(link.get(QuestionConcept_.id).get(QuestionConceptId_.conceptId), conceptId)
            );
            return cb.exists(matchingLink);
        };
    }

    private QuerySpecification<Question> hasConcept() {
        return (root, criteriaQuery, cb) -> {
            Subquery<Long> matchingLink = criteriaQuery.subquery(Long.class);
            Root<QuestionConcept> link = matchingLink.from(QuestionConcept.class);
            matchingLink.select(cb.literal(1L)).where(
                    cb.equal(link.get(QuestionConcept_.id).get(QuestionConceptId_.questionId), root.get(Question_.id))
            );
            return cb.exists(matchingLink);
        };
    }
}
