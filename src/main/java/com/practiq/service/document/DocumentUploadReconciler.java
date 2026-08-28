package com.practiq.service.document;

import static com.practiq.service.document.DocumentUploadRules.UPLOAD_COMPLETION_GRACE;
import static com.practiq.service.document.DocumentUploadRules.UPLOAD_URL_EXPIRY;

import com.practiq.foundation.types.DocumentStatus;
import com.practiq.persistence.DocumentEntity;
import com.practiq.persistence.repository.DocumentRepository;
import com.practiq.service.document.dto.response.DocumentUploadReconciliationSummary;
import com.practiq.storage.S3DocumentStorage;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DocumentUploadReconciler {

    private static final Sort OLDEST_FIRST = Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    private final DocumentRepository documentRepository;
    private final S3DocumentStorage s3DocumentStorage;
    private final Clock clock;
    private final int batchSize;

    public DocumentUploadReconciler(
            DocumentRepository documentRepository,
            S3DocumentStorage s3DocumentStorage,
            Clock clock,
            @Value("${practiq.document-upload-reconcile.batch-size}") int batchSize) {
        this.documentRepository = documentRepository;
        this.s3DocumentStorage = s3DocumentStorage;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    // The pooled connection is deliberately held across the S3 checks: the job is single-threaded, so that
    // costs one connection, and promoting and expiring in one transaction is worth more than releasing it.
    @Transactional
    public DocumentUploadReconciliationSummary reconcileDocumentsAwaitingUpload() {
        // We want anything that should really have been uploaded by now but hasn't, with a little bit of cooling off
        // to prevent being over eager and grabbing something just as it ticks over despite still being uploaded.
        Instant cutOff = clock.instant().minus(UPLOAD_URL_EXPIRY.plus(UPLOAD_COMPLETION_GRACE));
        log.info("Reconciling documents awaiting upload created before {}", cutOff);

        Page<DocumentEntity> due = documentRepository.findByStatusAndCreatedAtBefore(
                DocumentStatus.AWAITING_UPLOAD, cutOff, Pageable.from(0, batchSize, OLDEST_FIRST));
        List<DocumentEntity> documents = due.getContent();
        if (CollectionUtils.isEmpty(documents)) {
            // Not assumed to be zero: an empty page with a non-zero total would otherwise report a clear
            // backlog as nothing left, and that number feeds alerting.
            return createReconciliationSummary(0, 0, 0, due.getTotalSize());
        }

        Set<String> s3Keys = documents.stream().map(DocumentEntity::getS3Key).collect(Collectors.toSet());
        Set<String> uploadedKeys = s3DocumentStorage.filterToKeysThatExist(s3Keys);

        List<DocumentEntity> completedUploads = documents.stream()
                .filter(document -> uploadedKeys.contains(document.getS3Key()))
                .toList();
        // These documents are managed for the life of the transaction, so the status change is written
        // at commit and an explicit update call would issue no statement of its own.
        completedUploads.forEach(document -> document.updateStatus(DocumentStatus.UNAPPROVED));

        List<DocumentEntity> missingUploads = documents.stream()
                .filter(document -> !uploadedKeys.contains(document.getS3Key()))
                .toList();
        documentRepository.deleteAll(missingUploads);

        // Everything this run took is either promoted out of AWAITING_UPLOAD or deleted, so what is left
        // is simply what the batch could not reach. No second count is needed to know it.
        long remaining = due.getTotalSize() - documents.size();
        return createReconciliationSummary(documents.size(), completedUploads.size(), missingUploads.size(), remaining);
    }

    private DocumentUploadReconciliationSummary createReconciliationSummary(
            int examined, int promoted, int expired, long remaining) {
        log.info(
                "Document upload reconcile complete: examined={}, promoted={}, expired={}, remaining={}",
                examined,
                promoted,
                expired,
                remaining);
        return new DocumentUploadReconciliationSummary(examined, promoted, expired, remaining);
    }
}
