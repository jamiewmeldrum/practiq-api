package com.practiq.service;

import static com.practiq.service.document.DocumentUploadRules.UPLOAD_URL_EXPIRY;

import com.practiq.domain.Document;
import com.practiq.dto.request.PostDocumentRequest;
import com.practiq.dto.response.UploadDocumentResponse;
import com.practiq.repository.DocumentRepository;
import com.practiq.service.document.DocumentStager;
import com.practiq.service.document.DocumentUploadCommand;
import com.practiq.service.document.StagedDocumentUpload;
import com.practiq.storage.PresignedUpload;
import com.practiq.storage.PresignedUploadRequest;
import com.practiq.storage.S3DocumentStorage;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;

@Singleton
public class DocumentService {

    private final DocumentStager documentStager;
    private final DocumentRepository documentRepository;
    private final S3DocumentStorage documentStorage;

    public DocumentService(
            DocumentStager documentStager, DocumentRepository documentRepository, S3DocumentStorage documentStorage) {
        this.documentStager = documentStager;
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
    }

    public UploadDocumentResponse stageDocumentUpload(@Valid PostDocumentRequest request) {
        DocumentUploadCommand uploadCommand = new DocumentUploadCommand(
                request.filename(), request.contentType(), request.contentLength(), request.sourceSpec());
        StagedDocumentUpload stagedDocumentUpload = documentStager.stageUpload(uploadCommand);

        PresignedUploadRequest presignedUploadRequest = new PresignedUploadRequest(
                stagedDocumentUpload.key(),
                stagedDocumentUpload.contentType(),
                stagedDocumentUpload.contentLength(),
                UPLOAD_URL_EXPIRY);
        PresignedUpload presignedUpload = documentStorage.presignUpload(presignedUploadRequest);

        Document document = Document.newUpload(
                stagedDocumentUpload.key(), stagedDocumentUpload.filename(), stagedDocumentUpload.sourceSpec());
        Document savedDocument = documentRepository.save(document);

        return new UploadDocumentResponse(
                savedDocument.getId(), presignedUpload.url().toString(), presignedUpload.expiresAt());
    }
}
