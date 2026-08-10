package com.practiq.domain;

import static jakarta.persistence.GenerationType.IDENTITY;

import com.practiq.domain.types.DocumentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "document")
@Getter
@ToString
public class Document {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Version
    private int version;

    @NotNull @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @NotNull @Column(name = "filename", nullable = false)
    private String filename;

    @Size(max = 255) @Column(name = "source_spec")
    private String sourceSpec;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;
}
