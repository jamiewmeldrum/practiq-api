package com.practiq.repository;

import com.practiq.domain.Question;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;

@Repository
public interface QuestionRepository extends CrudRepository<Question, Long> {

    @Join(value = "conceptLinks", type = Join.Type.LEFT_FETCH)
    @Override
    List<Question> findAll();
}
