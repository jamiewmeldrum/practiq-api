package com.practiq.service.document;

import com.practiq.service.document.dto.response.DocumentUploadReconciliationSummary;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DocumentUploadReconcileScheduler {

    private final DocumentUploadReconciler documentUploadReconciler;

    public DocumentUploadReconcileScheduler(DocumentUploadReconciler documentUploadReconciler) {
        this.documentUploadReconciler = documentUploadReconciler;
    }

    // The condition gates the scheduler's own firing, not the method, so a direct call still reconciles.
    @Scheduled(
            fixedDelay = "${practiq.document-upload-reconcile-scheduler.interval}",
            initialDelay = "${practiq.document-upload-reconcile-scheduler.initial-delay}",
            condition = "${practiq.document-upload-reconcile-scheduler.enabled}")
    public void runUploadReconcile() {
        log.info("Scheduled document upload reconcile fired");
        DocumentUploadReconciliationSummary summary = documentUploadReconciler.reconcileDocumentsAwaitingUpload();
        log.info(
                "Scheduled document upload reconcile complete: processed={}, remaining={}",
                summary.examined(),
                summary.remaining());
    }
}
