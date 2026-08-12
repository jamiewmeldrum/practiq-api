package com.practiq.controller;

import static com.practiq.http.HttpConstants.ADMIN_KEY_HEADER;

import com.practiq.dto.request.PostDocumentRequest;
import com.practiq.dto.response.UploadDocumentResponse;
import com.practiq.http.AdminKeyValidator;
import com.practiq.service.DocumentService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
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
            @Nullable @Header(ADMIN_KEY_HEADER) String adminKey, @Body PostDocumentRequest request) {
        log.debug("Requested to POST document with filename {}", request.filename());

        adminKeyValidator.validate(adminKey);
        return documentService.stageDocumentUpload(request);
    }
}
