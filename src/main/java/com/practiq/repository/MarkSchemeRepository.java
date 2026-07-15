package com.practiq.repository;

import com.practiq.domain.MarkScheme;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface MarkSchemeRepository extends CrudRepository<MarkScheme, Long> {
}
