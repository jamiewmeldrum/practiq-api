package com.practiq.persistence.repository;

import com.practiq.foundation.types.DocumentStatus;
import com.practiq.persistence.DocumentEntity;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import java.time.Instant;
import java.util.List;

@Repository
public interface DocumentRepository extends CrudRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByStatusAndCreatedAtBefore(DocumentStatus status, Instant createdAt);
}
