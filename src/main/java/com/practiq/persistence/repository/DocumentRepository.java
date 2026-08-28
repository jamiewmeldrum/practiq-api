package com.practiq.persistence.repository;

import com.practiq.foundation.types.DocumentStatus;
import com.practiq.persistence.DocumentEntity;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.CrudRepository;
import java.time.Instant;

@Repository
public interface DocumentRepository extends CrudRepository<DocumentEntity, Long> {
    // A Page rather than a List: the caller batches, so it needs the total to know whether what it was
    // handed is all of it. The ordering rides in on the Pageable - the caller owns that with the size.
    Page<DocumentEntity> findByStatusAndCreatedAtBefore(DocumentStatus status, Instant createdAt, Pageable pageable);
}
