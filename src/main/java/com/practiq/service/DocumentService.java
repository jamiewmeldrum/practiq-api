package com.practiq.service;

import com.practiq.domain.Document;
import com.practiq.dto.request.PostDocumentRequest;
import com.practiq.dto.response.UploadDocumentResponse;
import com.practiq.repository.DocumentRepository;
import com.practiq.service.document.DocumentStager;
import com.practiq.service.document.DocumentUploadCommand;
import com.practiq.service.document.StagedDocumentUpload;
import com.practiq.storage.S3DocumentStorage;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.net.URI;

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

        Document document = Document.newUpload(
                stagedDocumentUpload.key(), stagedDocumentUpload.filename(), stagedDocumentUpload.sourceSpec());

        URI uri = documentStorage.generatePresignedUploadURI(
                stagedDocumentUpload.key(), stagedDocumentUpload.contentType(), stagedDocumentUpload.contentLength());

        documentRepository.save(document);
        return new UploadDocumentResponse(document.getId(), uri.toString());
    }
}
