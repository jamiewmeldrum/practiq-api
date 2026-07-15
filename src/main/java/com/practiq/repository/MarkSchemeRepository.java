package com.practiq.repository;

import com.practiq.domain.MarkScheme;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

//TODO - explicit tests for usage path
@Repository
public interface MarkSchemeRepository extends CrudRepository<MarkScheme, Long> {
    Optional<MarkScheme> findByQuestionId(long questionId);
}
