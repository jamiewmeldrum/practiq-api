package com.practiq.service;

import com.practiq.domain.Document;
import com.practiq.dto.request.PostDocumentRequest;
import com.practiq.dto.response.UploadDocumentResponse;
import com.practiq.repository.DocumentRepository;
import com.practiq.storage.FileType;
import com.practiq.storage.S3DocumentStorage;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;

@Singleton
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final S3DocumentStorage documentStorage;

    public DocumentService(DocumentRepository documentRepository, S3DocumentStorage documentStorage) {
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
    }

    public UploadDocumentResponse stageDocumentUpload(@Valid PostDocumentRequest request) {

        // TODO - likely brittle so resolve shortly
        String filename = request.filename();
        String[] filenameSplit = filename.split("\\.(?=[^.]+$)");
        String extension = filenameSplit[filenameSplit.length - 1];

        // TODO - rework the two upload checks below; both are currently weaker than they look.
        //  1. Deliberateness check: PostDocumentRequest.contentType should carry a MIME type
        //     ("application/pdf"), not an extension ("pdf"), and be compared to fileType.contentType().
        //     As it stands it is compared to fileType.extension(), which the server derived from the
        //     filename the same client sent — one input compared with itself, so it cannot catch a
        //     mistake. A browser sources the two independently (File.name vs File.type), so against a
        //     MIME type the comparison actually fires on a mismatch. It is also the value that gets
        //     signed, that the client must then send as its Content-Type header on the PUT, and that
        //     ends up as the stored object's metadata for the extractor to read.
        //     Watch for: browsers report .doc/.docx as application/octet-stream (or nothing) often
        //     enough that strict equality will reject valid uploads. If that bites, let each FileType
        //     hold a set of acceptable declared types rather than one — not before.
        //  2. Allow-list: S3DocumentStorage.ALLOWED_FILE_TYPES lists every FileType constant, so
        //     isAcceptedFileType is unconditionally true. FileType.fromExtension returning empty is
        //     what actually rejects unwanted extensions — the enum is the allow-list. Delete the set
        //     (and isAcceptedFileType with it, which also gets product policy out of the storage
        //     adapter) unless a reason appears to model extensions we recognise but refuse.
        //  Both rejections are caller error and should surface as 4xx, not the 500 that a bare
        //  RuntimeException gets from GenericExceptionHandler.
        if (StringUtils.isEmpty(extension)) {
            throw new IllegalArgumentException("extension is empty");
        }

        FileType fileType = FileType.fromExtension(extension).orElseThrow(RuntimeException::new);

        if (!documentStorage.isAcceptedFileType(fileType)) {
            throw new RuntimeException("Unsupported file type: " + fileType);
        }

        if (!request.contentType().equalsIgnoreCase(fileType.extension())) {
            throw new RuntimeException("Specified type mismatch");
        }

        String key = UUID.randomUUID() + "." + extension;
        // TODO - no reason to set the default explicitly
        Document document = Document.newUpload(key, request.filename()).withSourceSpec(request.sourceSpec());

        URI uri = documentStorage.generatePresignedUploadURI(key, fileType.contentType(), request.contentLength());

        // TODO - do this last. No point in saving document if couldn't get a URL. Maybe an explicit error thrown to
        // make sure this always breaks as intended
        documentRepository.save(document);

        return new UploadDocumentResponse(document.getId(), uri.toString());
    }
}
