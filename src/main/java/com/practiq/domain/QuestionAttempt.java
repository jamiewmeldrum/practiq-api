package com.practiq.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Generated;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "question_attempt")
@Getter
@ToString
@EqualsAndHashCode
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Column(name = "question_id", nullable = false, updatable = false)
    private long questionId;

    @NotNull
    @Column(name = "session_token", nullable = false)
    private String sessionToken;

    @NotNull
    @Column(name = "body", nullable = false)
    @Size(min = 1, max = 100000)
    private String body;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;

    public QuestionAttempt() {}

    public QuestionAttempt(long questionId, String sessionToken, String body) {
        this.questionId = questionId;
        this.sessionToken = sessionToken;
        this.body = body;
    }
}
