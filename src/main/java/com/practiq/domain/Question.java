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

@Entity
@Table(name = "question")
@Getter
@ToString
public class Question {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Version
    private int version;

    @NotNull
    @Column(name = "body", nullable = false)
    private String body;

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

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionConcept> conceptLinks = new HashSet<>();

    protected Question() {}

    public Question(
            String body,
            QuestionDifficulty difficulty,
            QuestionType type,
            QuestionSource source,
            QuestionStatus status,
            String sourceSpec
    ) {
        this.body = body;
        this.difficulty = difficulty;
        this.type = type;
        this.source = source;
        this.status = status;
        this.sourceSpec = sourceSpec;
    }
}
