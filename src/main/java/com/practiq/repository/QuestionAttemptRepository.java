package com.practiq.repository;

import com.practiq.domain.QuestionAttempt;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

//TODO - testing to shadow MarkSchemeRepositoryIT
@Repository
public interface QuestionAttemptRepository extends CrudRepository<QuestionAttempt, Long> {
    Optional<QuestionAttempt> findByQuestionId(long questionId);
}
