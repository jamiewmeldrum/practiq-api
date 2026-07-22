package com.practiq.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Generated;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "mark_scheme")
@Getter
@ToString
public class MarkScheme {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Column(name = "question_id", nullable = false, updatable = false)
    private long questionId;

    @Version
    private int version;

    @NotNull
    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;

    protected MarkScheme() {}

    public MarkScheme(long questionId, String body) {
        this.questionId = questionId;
        this.body = body;
    }
}
