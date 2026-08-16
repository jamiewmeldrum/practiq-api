package com.practiq.persistence;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "question_concept")
@Getter
public class QuestionConceptEntity {

    @EmbeddedId
    private QuestionConceptEntityId id;

    @MapsId("questionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    @MapsId("conceptId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id")
    private ConceptEntity concept;

    protected QuestionConceptEntity() {}

    public QuestionConceptEntity(QuestionEntity question, ConceptEntity concept) {
        this.question = question;
        this.concept = concept;
        this.id = new QuestionConceptEntityId(question.getId(), concept.getId());
    }
}
