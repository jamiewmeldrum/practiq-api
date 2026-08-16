package com.practiq.persistence.repository;

import com.practiq.persistence.DocumentEntity;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface DocumentRepository extends CrudRepository<DocumentEntity, Long> {}
