package com.practiq.domain.projection;

import io.micronaut.core.annotation.Introspected;

// Lightweight projection of a question_concept row — just the id pair. Lets a page of concept links be
// loaded and stitched onto questions without materialising QuestionConcept entities or their lazy
// question/concept associations. A dedicated projection is required because Micronaut Data can't map a
// selected @EmbeddedId (SELECT qc.id) back to QuestionConceptId — it NPEs in its tuple collector.
@Introspected
public record QuestionConceptLink(long questionId, long conceptId) {
}
