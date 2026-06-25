package com.practiq.repository;

import com.practiq.domain.Concept;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface ConceptRepository extends CrudRepository<Concept, Long> {
}
