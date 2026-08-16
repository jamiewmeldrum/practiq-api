package com.practiq.persistence.query.question;

import com.practiq.persistence.QuestionEntity;
import java.util.Set;

// A question as read, with the concept ids that a fetch-join would have carried had paging allowed one.
// Assembled by the runner rather than returned by a query, so it is deliberately not called a projection.
public record QuestionWithConceptIds(QuestionEntity question, Set<Long> conceptIds) {}
