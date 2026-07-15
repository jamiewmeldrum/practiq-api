package com.practiq.domain.projection;

import com.practiq.domain.Question;

import java.util.Set;

public record LinkedQuestion(Question question, Set<QuestionConceptLink> conceptLinks) {
}
