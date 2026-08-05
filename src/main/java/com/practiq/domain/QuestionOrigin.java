package com.practiq.domain;

import static jakarta.persistence.GenerationType.IDENTITY;

import com.practiq.domain.types.QuestionSource;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "question_origin")
@Getter
@ToString
@EqualsAndHashCode
public class QuestionOrigin {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Column(name = "question_id", nullable = false, updatable = false, unique = true)
    private long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private QuestionSource source;

    @Column(name = "document_id")
    private long documentId;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;
}
