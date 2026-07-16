package com.practiq.repository;

import com.practiq.domain.Question;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;

//TODO - test this pagination a little more after merge - oneToMany etc
@Repository
public interface QuestionRepository extends CrudRepository<Question, Long>, JpaSpecificationExecutor<Question> {
}
