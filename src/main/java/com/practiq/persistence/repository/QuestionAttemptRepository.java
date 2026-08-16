package com.practiq.persistence.repository;

import com.practiq.persistence.QuestionAttemptEntity;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;

@Repository
public interface QuestionAttemptRepository
        extends CrudRepository<QuestionAttemptEntity, Long>, JpaSpecificationExecutor<QuestionAttemptEntity> {}
