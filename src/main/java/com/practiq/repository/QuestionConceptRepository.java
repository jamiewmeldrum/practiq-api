package com.practiq.repository;

import com.practiq.domain.QuestionConcept;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.QuestionConceptId;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.GenericRepository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionConceptRepository extends GenericRepository<QuestionConcept, QuestionConceptId> {

    // Loads just the (questionId, conceptId) pairs for a page of questions, so the service can stitch
    // concept ids onto the DTOs without fetch-joining the collection into the paged query. Selecting the
    // two scalars into a projection (with explicit aliases) avoids both the lazy associations and Micronaut
    // Data's inability to map a selected @EmbeddedId back to QuestionConceptId (see QuestionConceptLink).
    @Query("SELECT qc.id.questionId AS questionId, qc.id.conceptId AS conceptId "
            + "FROM QuestionConcept qc WHERE qc.id.questionId IN (:questionIds)")
    List<QuestionConceptLink> findLinksByQuestionIds(Collection<Long> questionIds);
}
