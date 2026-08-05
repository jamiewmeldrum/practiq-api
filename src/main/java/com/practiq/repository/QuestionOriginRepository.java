package com.practiq.repository;

import com.practiq.domain.QuestionOrigin;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface QuestionOriginRepository extends CrudRepository<QuestionOrigin, Long> {}
