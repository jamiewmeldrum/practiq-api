package com.practiq.persistence.projection;

import io.micronaut.core.annotation.Introspected;

// Lightweight projection of a question_concept row — just the id pair. Lets a page of concept links be
// loaded and attached to questions without materialising QuestionConceptEntity or its lazy question and
// concept associations. A dedicated projection is required because Micronaut Data can't map a selected
// @EmbeddedId (SELECT qc.id) back to QuestionConceptEntityId — it NPEs in its tuple collector.
@Introspected
public record QuestionConceptLinkProjection(long questionId, long conceptId) {}
