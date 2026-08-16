package com.practiq.persistence;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "mark_scheme")
@Getter
@ToString
public class MarkSchemeEntity {

    public static final int BODY_MAX_LENGTH = 10000;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Column(name = "question_id", nullable = false, updatable = false, unique = true)
    private long questionId;

    @Version
    private int version;

    @NotNull @Size(max = BODY_MAX_LENGTH) @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;

    protected MarkSchemeEntity() {}

    public MarkSchemeEntity(long questionId, @NotNull String body) {
        this.questionId = questionId;
        this.body = body;
    }
}
