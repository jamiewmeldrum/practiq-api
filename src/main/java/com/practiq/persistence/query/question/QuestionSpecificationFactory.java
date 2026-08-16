package com.practiq.persistence.query.question;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import com.practiq.persistence.QuestionConceptEntity;
import com.practiq.persistence.QuestionConceptEntityId_;
import com.practiq.persistence.QuestionConceptEntity_;
import com.practiq.persistence.QuestionEntity;
import com.practiq.persistence.QuestionEntity_;
import com.practiq.persistence.query.QuerySpecificationFactory;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;

@Singleton
public class QuestionSpecificationFactory extends QuerySpecificationFactory<QuestionEntity, QuestionQuery> {

    @Override
    protected QuerySpecification<QuestionEntity> applyDomain(
            QuerySpecification<QuestionEntity> specification, QuestionQuery query) {
        if (query.getStatus() != null) {
            specification = specification.and(hasStatus(query.getStatus()));
        }

        if (!CollectionUtils.isEmpty(query.getTypes())) {
            specification = specification.and(isInQuestionTypes(query.getTypes()));
        }

        if (!CollectionUtils.isEmpty(query.getDifficulties())) {
            specification = specification.and(isInQuestionDifficulties(query.getDifficulties()));
        }

        // Serving policy comes from the query, not this factory: a conceptId filter implies a link, and
        // otherwise the link requirement applies only when the query demands it (student catalogue does;
        // an admin review query must be able to see unlinked questions).
        if (query.getConceptId() != null) {
            specification = specification.and(hasConceptForId(query.getConceptId()));
        } else if (query.isRequiresConceptLink()) {
            specification = specification.and(hasConcept());
        }

        if (query.getQuestionId() != null) {
            specification = specification.and(hasId(query.getQuestionId()));
        }

        return specification;
    }

    private QuerySpecification<QuestionEntity> hasStatus(QuestionStatus status) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get(QuestionEntity_.status), status);
    }

    private QuerySpecification<QuestionEntity> isInQuestionTypes(List<QuestionType> types) {
        return (root, criteriaQuery, cb) -> root.get(QuestionEntity_.type).in(types);
    }

    private QuerySpecification<QuestionEntity> isInQuestionDifficulties(List<QuestionDifficulty> difficulties) {
        return (root, criteriaQuery, cb) -> root.get(QuestionEntity_.difficulty).in(difficulties);
    }

    // Filter on the to-many via EXISTS, deliberately NOT a join: joining question_concept would
    // multiply question rows (one per link) and corrupt the page count. EXISTS keeps exactly one row
    // per question, so pagination and counting stay correct with no distinct needed. This is why the
    // filter side stays clean even as concept filtering arrives.
    private QuerySpecification<QuestionEntity> hasConceptForId(long conceptId) {
        return (root, criteriaQuery, cb) -> {
            Subquery<Long> matchingLink = criteriaQuery.subquery(Long.class);
            Root<QuestionConceptEntity> link = matchingLink.from(QuestionConceptEntity.class);
            matchingLink
                    .select(cb.literal(1L))
                    .where(
                            cb.equal(
                                    link.get(QuestionConceptEntity_.id).get(QuestionConceptEntityId_.questionId),
                                    root.get(QuestionEntity_.id)),
                            cb.equal(
                                    link.get(QuestionConceptEntity_.id).get(QuestionConceptEntityId_.conceptId),
                                    conceptId));
            return cb.exists(matchingLink);
        };
    }

    private QuerySpecification<QuestionEntity> hasConcept() {
        return (root, criteriaQuery, cb) -> {
            Subquery<Long> matchingLink = criteriaQuery.subquery(Long.class);
            Root<QuestionConceptEntity> link = matchingLink.from(QuestionConceptEntity.class);
            matchingLink
                    .select(cb.literal(1L))
                    .where(cb.equal(
                            link.get(QuestionConceptEntity_.id).get(QuestionConceptEntityId_.questionId),
                            root.get(QuestionEntity_.id)));
            return cb.exists(matchingLink);
        };
    }

    private QuerySpecification<QuestionEntity> hasId(long id) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get(QuestionEntity_.id), id);
    }
}
