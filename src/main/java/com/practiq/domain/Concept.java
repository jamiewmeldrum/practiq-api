package com.practiq.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "concept")
@Getter
@ToString
public class Concept {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @NotNull
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @NotNull
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Concept() {}

    public Concept(@NotNull String name, @NotNull String description) {
        this.name = name;
        this.description = description;
    }

    @ManyToMany(mappedBy = "concepts")
    private Set<Question> questions = new HashSet<>();
}
