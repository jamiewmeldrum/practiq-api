package com.practiq.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

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

    // Scalar reference, not a @OneToOne Question: mark scheme and question are separate aggregates with
    // independent edit lifecycles (D-018), so the relationship is held once as the FK and navigated by id
    // through the repository when needed — never as an eager association.
    @Column(name = "question_id", nullable = false, updatable = false)
    private long questionId;

    @Version
    private int version;

    @NotNull
    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
