package com.practiq.repository;

import com.practiq.domain.Document;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface DocumentRepository extends CrudRepository<Document, Long> {}
