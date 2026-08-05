package com.practiq.domain;

import static jakarta.persistence.GenerationType.IDENTITY;

import com.practiq.domain.types.QuestionAuthorship;
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
    @Column(name = "authorship", nullable = false)
    private QuestionAuthorship authorship;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;
}
