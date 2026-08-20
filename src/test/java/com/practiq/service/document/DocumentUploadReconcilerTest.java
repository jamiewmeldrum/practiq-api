package com.practiq.service.document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;
import static utils.DateTimeUtils.FIXED_CLOCK;
import static utils.DateTimeUtils.FIXED_NOW;

import com.practiq.foundation.types.DocumentStatus;
import com.practiq.persistence.DocumentEntity;
import com.practiq.persistence.repository.DocumentRepository;
import com.practiq.storage.S3DocumentStorage;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentUploadReconcilerTest {

    private static final int CUT_OFF_MINUTES = 15;

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final S3DocumentStorage s3DocumentStorage = mock(S3DocumentStorage.class);
    private final Clock clock = FIXED_CLOCK;

    private final DocumentUploadReconciler reconciler =
            new DocumentUploadReconciler(documentRepository, s3DocumentStorage, clock);

    @Test
    void executionTerminatedEarlyIfNoDocumentsFound() {
        Instant cutOff = FIXED_NOW.minus(CUT_OFF_MINUTES, ChronoUnit.MINUTES);
        when(documentRepository.findByStatusAndCreatedAtBefore(DocumentStatus.AWAITING_UPLOAD, cutOff))
                .thenReturn(List.of());

        reconciler.reconcileDocumentsAwaitingUpload();

        verify(documentRepository).findByStatusAndCreatedAtBefore(DocumentStatus.AWAITING_UPLOAD, cutOff);
        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(s3DocumentStorage);
    }

    @Test
    void ensureDocumentsWithUploadedFilesAreUpdated() {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        String key4 = "key4";

        DocumentEntity documentEntity1 = DocumentEntity.newUpload(key1, "filename", "");
        DocumentEntity documentEntity2 = DocumentEntity.newUpload(key2, "filename", "");
        DocumentEntity documentEntity3 = DocumentEntity.newUpload(key3, "filename", "");
        DocumentEntity documentEntity4 = DocumentEntity.newUpload(key4, "filename", "");

        Instant cutOff = FIXED_NOW.minus(CUT_OFF_MINUTES, ChronoUnit.MINUTES);
        when(documentRepository.findByStatusAndCreatedAtBefore(DocumentStatus.AWAITING_UPLOAD, cutOff))
                .thenReturn(List.of(documentEntity1, documentEntity2, documentEntity3, documentEntity4));
        when(s3DocumentStorage.filterToKeysThatExist(Set.of(key1, key2, key3, key4)))
                .thenReturn(Set.of(key2, key4));

        reconciler.reconcileDocumentsAwaitingUpload();

        verify(documentRepository).findByStatusAndCreatedAtBefore(DocumentStatus.AWAITING_UPLOAD, cutOff);
        verify(s3DocumentStorage).filterToKeysThatExist(Set.of(key1, key2, key3, key4));

        // Nothing is saved: the reconciler mutates the documents the repository handed back and lets the
        // transaction write them at commit. These are those same instances, so their state is the write.
        assertThat(documentEntity1.getStatus(), equalTo(DocumentStatus.AWAITING_UPLOAD));
        assertThat(documentEntity2.getStatus(), equalTo(DocumentStatus.UNAPPROVED));
        assertThat(documentEntity3.getStatus(), equalTo(DocumentStatus.AWAITING_UPLOAD));
        assertThat(documentEntity4.getStatus(), equalTo(DocumentStatus.UNAPPROVED));
    }

    @Test
    void ensureDocumentsWithoutUploadedFilesAreDeleted() {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        String key4 = "key4";

        DocumentEntity documentEntity1 = DocumentEntity.newUpload(key1, "filename", "");
        DocumentEntity documentEntity2 = DocumentEntity.newUpload(key2, "filename", "");
        DocumentEntity documentEntity3 = DocumentEntity.newUpload(key3, "filename", "");
        DocumentEntity documentEntity4 = DocumentEntity.newUpload(key4, "filename", "");

        Instant cutOff = FIXED_NOW.minus(CUT_OFF_MINUTES, ChronoUnit.MINUTES);
        when(documentRepository.findByStatusAndCreatedAtBefore(DocumentStatus.AWAITING_UPLOAD, cutOff))
                .thenReturn(List.of(documentEntity1, documentEntity2, documentEntity3, documentEntity4));
        when(s3DocumentStorage.filterToKeysThatExist(Set.of(key1, key2, key3, key4)))
                .thenReturn(Set.of(key2, key4));

        reconciler.reconcileDocumentsAwaitingUpload();

        verify(documentRepository).findByStatusAndCreatedAtBefore(DocumentStatus.AWAITING_UPLOAD, cutOff);
        verify(s3DocumentStorage).filterToKeysThatExist(Set.of(key1, key2, key3, key4));
        verify(documentRepository).deleteAll(List.of(documentEntity1, documentEntity3));

        assertThat(documentEntity1.getStatus(), equalTo(DocumentStatus.AWAITING_UPLOAD));
        assertThat(documentEntity3.getStatus(), equalTo(DocumentStatus.AWAITING_UPLOAD));
    }
}
