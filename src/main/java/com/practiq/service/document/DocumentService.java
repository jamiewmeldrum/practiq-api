package com.practiq.service.document;

import static com.practiq.service.document.DocumentUploadRules.UPLOAD_URL_EXPIRY;

import com.practiq.persistence.DocumentEntity;
import com.practiq.persistence.repository.DocumentRepository;
import com.practiq.service.document.dto.request.DocumentPresignUploadCommand;
import com.practiq.service.document.dto.response.DocumentPresignUpload;
import com.practiq.storage.PresignedUpload;
import com.practiq.storage.PresignedUploadRequest;
import com.practiq.storage.S3DocumentStorage;
import jakarta.inject.Singleton;

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

    public DocumentPresignUpload stageUploadAndPresign(DocumentPresignUploadCommand uploadCommand) {
        StagedDocumentUpload stagedDocumentUpload = documentStager.stageUpload(uploadCommand);

        PresignedUploadRequest presignedUploadRequest = new PresignedUploadRequest(
                stagedDocumentUpload.key(),
                stagedDocumentUpload.contentType(),
                stagedDocumentUpload.contentLength(),
                UPLOAD_URL_EXPIRY);
        PresignedUpload presignedUpload = documentStorage.presignUpload(presignedUploadRequest);

        DocumentEntity document = DocumentEntity.newUpload(
                stagedDocumentUpload.key(), stagedDocumentUpload.filename(), stagedDocumentUpload.sourceSpec());
        DocumentEntity savedDocument = documentRepository.save(document);

        return new DocumentPresignUpload(
                savedDocument.getId(), presignedUpload.url().toString(), presignedUpload.expiresAt());
    }
}
