package com.practiq.repository;

import com.practiq.domain.Question;
import com.practiq.domain.types.QuestionStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.annotation.EntityGraph;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;

import java.util.Optional;

@Repository
public interface QuestionRepository extends CrudRepository<Question, Long>, JpaSpecificationExecutor<Question> {
    @EntityGraph(attributePaths = "conceptLinks")
    Optional<Question> findByIdAndStatus(Long id, QuestionStatus status);
}
