package com.practiq.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Embeddable
@Getter
@EqualsAndHashCode
public class QuestionConceptId implements Serializable {

    private long questionId;
    private long conceptId;

    protected QuestionConceptId() {}

    public QuestionConceptId(long questionId, long conceptId) {
        this.questionId = questionId;
        this.conceptId = conceptId;
    }
}
