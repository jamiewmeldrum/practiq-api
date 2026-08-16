package com.practiq.persistence.repository;

import com.practiq.persistence.QuestionConceptEntity;
import com.practiq.persistence.QuestionConceptEntityId;
import com.practiq.persistence.projection.QuestionConceptLinkProjection;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.GenericRepository;
import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionConceptRepository extends GenericRepository<QuestionConceptEntity, QuestionConceptEntityId> {

    // Loads just the (questionId, conceptId) pairs for a set of questions, so the query runner can attach
    // concept ids without fetch-joining the collection into a paged query. Selecting the two scalars into a
    // projection (with explicit aliases) avoids both the lazy associations and Micronaut Data's inability to
    // map a selected @EmbeddedId back to QuestionConceptEntityId (see QuestionConceptLinkProjection).
    @Query("SELECT qc.id.questionId AS questionId, qc.id.conceptId AS conceptId "
            + "FROM QuestionConceptEntity qc WHERE qc.id.questionId IN (:questionIds)")
    List<QuestionConceptLinkProjection> findLinksByQuestionIds(Collection<Long> questionIds);
}
