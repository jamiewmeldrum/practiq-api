package com.practiq.repository;

import com.practiq.domain.QuestionAttempt;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;

@Repository
public interface QuestionAttemptRepository extends CrudRepository<QuestionAttempt, Long>, JpaSpecificationExecutor<QuestionAttempt> {
}
