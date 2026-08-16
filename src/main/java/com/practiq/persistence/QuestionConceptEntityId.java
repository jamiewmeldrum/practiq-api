package com.practiq.persistence;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Embeddable
@Getter
@EqualsAndHashCode
public class QuestionConceptEntityId implements Serializable {

    private long questionId;
    private long conceptId;

    protected QuestionConceptEntityId() {}

    public QuestionConceptEntityId(long questionId, long conceptId) {
        this.questionId = questionId;
        this.conceptId = conceptId;
    }
}
