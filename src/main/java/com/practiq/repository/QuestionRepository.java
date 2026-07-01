package com.practiq.repository;

import com.practiq.domain.Question;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface QuestionRepository extends CrudRepository<Question, Long> {
}
