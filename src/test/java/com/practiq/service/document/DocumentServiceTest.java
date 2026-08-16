package com.practiq.service.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static utils.data.TestData.DOCUMENT_UPLOAD_URL_EXPIRY;

import com.practiq.foundation.types.DocumentStatus;
import com.practiq.persistence.DocumentEntity;
import com.practiq.persistence.repository.DocumentRepository;
import com.practiq.service.document.dto.request.DocumentPresignUploadCommand;
import com.practiq.service.document.dto.response.DocumentPresignUpload;
import com.practiq.storage.PresignedUpload;
import com.practiq.storage.PresignedUploadRequest;
import com.practiq.storage.S3DocumentStorage;
import io.micronaut.http.MediaType;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import utils.TestReflection;

class DocumentServiceTest {

    private final DocumentStager documentStager = mock(DocumentStager.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final S3DocumentStorage documentStorage = mock(S3DocumentStorage.class);

    private final DocumentService documentService =
            new DocumentService(documentStager, documentRepository, documentStorage);

    @Test
    void stageDocumentUploadStagesTheRequestSavesTheDocumentAndReturnsThePresignedUrlAndGeneratePresign() {
        String filename = "physics-notes.pdf";
        String contentType = "application/pdf";
        String sourceSpec = "AQA GCSE Physics";
        int contentLength = 2048;
        long savedId = 99L;

        DocumentPresignUploadCommand command =
                new DocumentPresignUploadCommand(filename, contentType, contentLength, sourceSpec);

        StagedDocumentUpload staged = new StagedDocumentUpload(
                "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf",
                filename,
                sourceSpec,
                MediaType.APPLICATION_PDF_TYPE,
                contentLength);
        PresignedUpload presignedUpload = new PresignedUpload(
                URI.create("https://documents.s3.example.com/" + staged.key() + "?X-Amz-Signature=abc"),
                Instant.parse("2026-08-12T18:10:00Z"));

        when(documentStager.stageUpload(command)).thenReturn(staged);
        PresignedUploadRequest expectedPresignRequest = new PresignedUploadRequest(
                staged.key(), staged.contentType(), staged.contentLength(), DOCUMENT_UPLOAD_URL_EXPIRY);

        when(documentStorage.presignUpload(expectedPresignRequest)).thenReturn(presignedUpload);
        when(documentRepository.save(any(DocumentEntity.class)))
                .thenAnswer(invocation -> TestReflection.setField(invocation.getArgument(0), "id", savedId));

        DocumentPresignUpload presign = documentService.stageUploadAndPresign(command);

        verify(documentStager).stageUpload(command);
        verify(documentStorage).presignUpload(expectedPresignRequest);

        ArgumentCaptor<DocumentEntity> savedDocument = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(savedDocument.capture());

        DocumentEntity document = savedDocument.getValue();
        assertEquals(staged.key(), document.getS3Key());
        assertEquals(staged.filename(), document.getFilename());
        assertEquals(staged.sourceSpec(), document.getSourceSpec());
        assertEquals(DocumentStatus.AWAITING_UPLOAD, document.getStatus());

        assertEquals(
                new DocumentPresignUpload(savedId, presignedUpload.url().toString(), presignedUpload.expiresAt()),
                presign);
    }
}
