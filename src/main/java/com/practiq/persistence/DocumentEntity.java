package com.practiq.persistence;

import static jakarta.persistence.GenerationType.IDENTITY;

import com.practiq.foundation.types.DocumentStatus;
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
public class DocumentEntity {

    public static final int S3_KEY_MAX_LENGTH = 255;
    public static final int FILENAME_MAX_LENGTH = 255;
    public static final int SOURCE_SPEC_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Version
    private int version;

    @NotNull @Size(max = S3_KEY_MAX_LENGTH) @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @NotNull @Size(max = FILENAME_MAX_LENGTH) @Column(name = "filename", nullable = false)
    private String filename;

    @Size(max = SOURCE_SPEC_MAX_LENGTH) @Column(name = "source_spec")
    private String sourceSpec;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;

    protected DocumentEntity() {}

    private DocumentEntity(String s3Key, String filename, String sourceSpec, DocumentStatus status) {
        this.s3Key = s3Key;
        this.filename = filename;
        this.sourceSpec = sourceSpec;
        this.status = status;
    }

    public static DocumentEntity newUpload(String s3Key, String filename, String sourceSpec) {
        return new DocumentEntity(s3Key, filename, sourceSpec, DocumentStatus.AWAITING_UPLOAD);
    }
}
