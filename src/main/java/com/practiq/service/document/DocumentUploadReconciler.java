package com.practiq.service.document;

import static com.practiq.service.document.DocumentUploadRules.UPLOAD_URL_EXPIRY;

import com.practiq.foundation.types.DocumentStatus;
import com.practiq.persistence.DocumentEntity;
import com.practiq.persistence.repository.DocumentRepository;
import com.practiq.storage.S3DocumentStorage;
import io.micronaut.core.util.CollectionUtils;
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

    private final DocumentRepository documentRepository;
    private final S3DocumentStorage s3DocumentStorage;
    private final Clock clock;

    public DocumentUploadReconciler(
            DocumentRepository documentRepository, S3DocumentStorage s3DocumentStorage, Clock clock) {
        this.documentRepository = documentRepository;
        this.s3DocumentStorage = s3DocumentStorage;
        this.clock = clock;
    }

    // The pooled connection is deliberately held across the S3 checks: the job is single-threaded, so that
    // costs one connection, and promoting and expiring in one transaction is worth more than releasing it.
    @Transactional
    public void reconcileDocumentsAwaitingUpload() {
        // We want anything that should really have been uploaded by now but hasn't, with a little bit of cooling off
        // to prevent being over eager and grabbing something just as it ticks over despite still being uploaded.
        Instant cutOff = clock.instant().minus(UPLOAD_URL_EXPIRY.plusMinutes(5));
        log.info("Reconciling documents awaiting upload created before {}", cutOff);

        List<DocumentEntity> documents =
                documentRepository.findByStatusAndCreatedAtBefore(DocumentStatus.AWAITING_UPLOAD, cutOff);
        if (CollectionUtils.isEmpty(documents)) {
            logCompletion(0, 0, 0);
            return;
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

        logCompletion(documents.size(), completedUploads.size(), missingUploads.size());
    }

    private void logCompletion(int examined, int promoted, int expired) {
        log.info(
                "Document upload reconcile complete: examined={}, promoted={}, expired={}",
                examined,
                promoted,
                expired);
    }
}
