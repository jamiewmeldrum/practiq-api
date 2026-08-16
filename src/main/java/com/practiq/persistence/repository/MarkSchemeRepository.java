package com.practiq.persistence.repository;

import com.practiq.persistence.MarkSchemeEntity;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import java.util.Optional;

@Repository
public interface MarkSchemeRepository extends CrudRepository<MarkSchemeEntity, Long> {
    Optional<MarkSchemeEntity> findByQuestionId(long questionId);
}
