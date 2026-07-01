package com.practiq.domain;

import com.practiq.domain.converters.QuestionDifficultyAttributeConverter;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.GenerationType.IDENTITY;

//TODO - seed data needs to be enriched with null values where suitable
//TODO - why have a SEED Type. Seems pointless and restricts the realism of the data.
@Entity
@Table(name = "question")
@Getter
@ToString
public class Question {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @NotNull
    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "mark_scheme")
    private String markScheme;

    @Convert(converter = QuestionDifficultyAttributeConverter.class)
    @Column(name = "difficulty")
    private QuestionDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private QuestionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuestionStatus status = QuestionStatus.PENDING;

    @Column(name = "source_spec")
    private String sourceSpec;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany
    @JoinTable(
            name = "question_concept",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "concept_id")
    )
    private Set<Concept> concepts = new HashSet<>();
}
