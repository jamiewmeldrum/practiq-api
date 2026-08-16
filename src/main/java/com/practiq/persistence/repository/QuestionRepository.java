package com.practiq.persistence.repository;

import com.practiq.persistence.QuestionEntity;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;

@Repository
public interface QuestionRepository
        extends CrudRepository<QuestionEntity, Long>, JpaSpecificationExecutor<QuestionEntity> {}
