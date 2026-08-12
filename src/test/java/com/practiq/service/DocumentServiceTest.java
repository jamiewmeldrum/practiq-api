package com.practiq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.data.TestData.UPLOAD_URL_EXPIRY;

import com.practiq.domain.Document;
import com.practiq.domain.types.DocumentStatus;
import com.practiq.dto.request.PostDocumentRequest;
import com.practiq.dto.response.UploadDocumentResponse;
import com.practiq.repository.DocumentRepository;
import com.practiq.service.document.DocumentStager;
import com.practiq.service.document.DocumentUploadCommand;
import com.practiq.service.document.StagedDocumentUpload;
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
    void stageDocumentUploadStagesTheRequestSavesTheDocumentAndReturnsThePresignedUrl() {
        String filename = "physics-notes.pdf";
        String contentType = "application/pdf";
        String sourceSpec = "AQA GCSE Physics";
        int contentLength = 2048;
        long savedId = 99L;

        PostDocumentRequest request = new PostDocumentRequest(filename, contentLength, contentType, sourceSpec);
        DocumentUploadCommand expectedCommand =
                new DocumentUploadCommand(filename, contentType, contentLength, sourceSpec);

        StagedDocumentUpload staged = new StagedDocumentUpload(
                "cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf",
                filename,
                sourceSpec,
                MediaType.APPLICATION_PDF_TYPE,
                contentLength);
        PresignedUpload presignedUpload = new PresignedUpload(
                URI.create("https://documents.s3.example.com/" + staged.key() + "?X-Amz-Signature=abc"),
                Instant.parse("2026-08-12T18:10:00Z"));

        when(documentStager.stageUpload(expectedCommand)).thenReturn(staged);
        PresignedUploadRequest expectedPresignRequest = new PresignedUploadRequest(
                staged.key(), staged.contentType(), staged.contentLength(), UPLOAD_URL_EXPIRY);

        when(documentStorage.presignUpload(expectedPresignRequest)).thenReturn(presignedUpload);
        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> TestReflection.setField(invocation.getArgument(0), "id", savedId));

        UploadDocumentResponse response = documentService.stageDocumentUpload(request);

        verify(documentStager).stageUpload(expectedCommand);
        verify(documentStorage).presignUpload(expectedPresignRequest);

        ArgumentCaptor<Document> savedDocument = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(savedDocument.capture());

        Document document = savedDocument.getValue();
        assertEquals(staged.key(), document.getS3Key());
        assertEquals(staged.filename(), document.getFilename());
        assertEquals(staged.sourceSpec(), document.getSourceSpec());
        assertEquals(DocumentStatus.AWAITING_UPLOAD, document.getStatus());

        assertEquals(
                new UploadDocumentResponse(savedId, presignedUpload.url().toString(), presignedUpload.expiresAt()),
                response);
    }
}
