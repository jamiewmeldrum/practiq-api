package com.practiq.persistence.repository;

import com.practiq.persistence.ConceptEntity;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@Repository
public interface ConceptRepository extends CrudRepository<ConceptEntity, Long> {
    List<ConceptEntity> listOrderByCreatedAtAsc();
}
