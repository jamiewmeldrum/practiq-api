package com.practiq.web.controller;

import static com.practiq.web.HttpConstants.ADMIN_KEY_HEADER;
import static com.practiq.web.dto.mapper.UploadDocumentResponseMapper.toUploadDocumentResponse;

import com.practiq.service.document.DocumentService;
import com.practiq.service.document.dto.request.DocumentPresignUploadCommand;
import com.practiq.service.document.dto.response.DocumentPresignUpload;
import com.practiq.web.AdminKeyValidator;
import com.practiq.web.dto.request.PostDocumentRequest;
import com.practiq.web.dto.response.UploadDocumentResponse;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/admin/documents")
public class AdminDocumentController {

    private final DocumentService documentService;
    private final AdminKeyValidator adminKeyValidator;

    public AdminDocumentController(DocumentService documentService, AdminKeyValidator adminKeyValidator) {
        this.documentService = documentService;
        this.adminKeyValidator = adminKeyValidator;
    }

    @Post
    @Status(HttpStatus.CREATED)
    public UploadDocumentResponse postDocumentAndReturnPresignUrl(
            // Nullable so an absent header reaches the validator rather than failing to bind: a caller must
            // not be able to tell a missing key from a wrong one.
            @Nullable @Header(ADMIN_KEY_HEADER) String adminKey, @Valid @Body PostDocumentRequest request) {
        log.debug("Requested to POST document with filename {}", request.filename());

        adminKeyValidator.validate(adminKey);

        DocumentPresignUploadCommand uploadCommand = new DocumentPresignUploadCommand(
                request.filename(), request.contentType(), request.contentLength(), request.sourceSpec());
        DocumentPresignUpload documentPresignUpload = documentService.stageUploadAndPresign(uploadCommand);

        return toUploadDocumentResponse(documentPresignUpload);
    }
}
