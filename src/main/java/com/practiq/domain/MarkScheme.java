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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Version
    private int version;

    @NotNull
    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
