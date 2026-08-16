package com.practiq.persistence;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "question_attempt")
@Getter
@ToString
@EqualsAndHashCode
public class QuestionAttemptEntity {

    // A client-generated UUID is 36 characters; 64 leaves room for a different token format. Bounded
    // because the value is client-supplied and indexed — an oversized token would fail at insert on
    // Postgres's btree entry limit rather than at validation.
    public static final int SESSION_TOKEN_MAX_LENGTH = 64;

    // The storage guard, sitting above the product rule on the request DTO so that raising what a client
    // may send does not immediately owe a migration.
    public static final int BODY_MAX_LENGTH = 25000;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Column(name = "question_id", nullable = false, updatable = false)
    private long questionId;

    @NotBlank @Size(max = SESSION_TOKEN_MAX_LENGTH) @Column(name = "session_token", nullable = false)
    private String sessionToken;

    @NotBlank @Size(max = BODY_MAX_LENGTH) @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;

    protected QuestionAttemptEntity() {}

    public QuestionAttemptEntity(long questionId, String sessionToken, String body) {
        this.questionId = questionId;
        this.sessionToken = sessionToken;
        this.body = body;
    }
}
