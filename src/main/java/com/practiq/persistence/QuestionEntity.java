package com.practiq.persistence;

import static jakarta.persistence.GenerationType.IDENTITY;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import com.practiq.persistence.converter.QuestionDifficultyEntityAttributeConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "question")
@Getter
@ToString
public class QuestionEntity {

    public static final int BODY_MAX_LENGTH = 10000;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Version
    private int version;

    @NotNull @Size(max = BODY_MAX_LENGTH) @Column(name = "body", nullable = false)
    private String body;

    @Convert(converter = QuestionDifficultyEntityAttributeConverter.class)
    @Column(name = "difficulty")
    private QuestionDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private QuestionType type;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuestionStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;

    protected QuestionEntity() {}

    public QuestionEntity(String body, QuestionDifficulty difficulty, QuestionType type, QuestionStatus status) {
        this.body = body;
        this.difficulty = difficulty;
        this.type = type;
        this.status = status;
    }
}
