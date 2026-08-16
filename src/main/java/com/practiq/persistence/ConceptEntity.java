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
@Table(name = "concept")
@Getter
@ToString
public class ConceptEntity {

    public static final int NAME_MAX_LENGTH = 200;
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Version
    private int version;

    @NotNull @Size(max = NAME_MAX_LENGTH) @Column(name = "name", nullable = false, unique = true)
    private String name;

    @NotNull @Size(max = DESCRIPTION_MAX_LENGTH) @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated
    private Instant createdAt;

    protected ConceptEntity() {}

    public ConceptEntity(@NotNull String name, @NotNull String description) {
        this.name = name;
        this.description = description;
    }
}
