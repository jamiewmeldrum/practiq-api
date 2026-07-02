package com.practiq.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "question_concept")
@Getter
public class QuestionConcept {

    @EmbeddedId
    private QuestionConceptId id;

    @MapsId("questionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @MapsId("conceptId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id")
    private Concept concept;

    protected QuestionConcept() {
    }

    public QuestionConcept(Question question, Concept concept) {
        this.question = question;
        this.concept = concept;
        this.id = new QuestionConceptId(question.getId(), concept.getId());
    }
}