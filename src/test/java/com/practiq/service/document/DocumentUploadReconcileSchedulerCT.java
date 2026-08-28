package com.practiq.service.document;

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.practiq.service.document.dto.response.DocumentUploadReconciliationSummary;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

// Deliberately not @ComponentTest, which is otherwise the annotation for this tier. A method-level
// @Property is only read when the context is rebuilt per test, and rebuildContext is read straight off
// the reflectively-found annotation, so it cannot be reached through one that wraps it.
@MicronautTest(transactional = false, environments = "ctslice", rebuildContext = true)
@Property(name = "practiq.document-upload-reconcile-scheduler.interval", value = "50ms")
@Property(name = "practiq.document-upload-reconcile-scheduler.initial-delay", value = "50ms")
class DocumentUploadReconcileSchedulerCT {

    @Inject
    private DocumentUploadReconciler documentUploadReconciler;

    @MockBean(DocumentUploadReconciler.class)
    DocumentUploadReconciler documentUploadReconciler() {
        DocumentUploadReconciler reconciler = mock(DocumentUploadReconciler.class);
        when(reconciler.reconcileDocumentsAwaitingUpload())
                .thenReturn(new DocumentUploadReconciliationSummary(0, 0, 0, 0));
        return reconciler;
    }

    @Test
    @Property(name = "practiq.document-upload-reconcile-scheduler.enabled", value = "true")
    void firesTheReconcileWithoutAnyoneCallingIt() {
        verify(documentUploadReconciler, timeout(2000)).reconcileDocumentsAwaitingUpload();
    }

    // Whether the schedule runs is left to the test environment's own configuration, so this fails if
    // that switch is ever mis-set rather than the tier quietly acquiring a job competing with fixtures.
    @Test
    void staysSilentWhileTheScheduleIsSwitchedOffForTests() {
        verify(documentUploadReconciler, after(1000).never()).reconcileDocumentsAwaitingUpload();
    }
}
