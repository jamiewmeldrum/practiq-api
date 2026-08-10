package com.practiq.controller;

import com.practiq.dto.request.PostDocumentRequest;
import com.practiq.dto.response.UploadDocumentResponse;
import com.practiq.http.HttpConstants;
import com.practiq.service.DocumentService;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.netty.util.internal.StringUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/admin/documents")
public class AdminDocumentController {

    private final DocumentService documentService;

    public AdminDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Post
    @Status(HttpStatus.CREATED)
    public UploadDocumentResponse postDocumentAndReturnPresignUrl(
            @NotBlank @Header(HttpConstants.ADMIN_KEY_HEADER)
                    String adminKey, // TODO - would be nice to have an annotation to auto check this has been provided
            // or at least wrap this @MandatoryHeader or something
            @Body PostDocumentRequest request) {
        log.debug("Requested to POST document with filename {}", request.filename());

        // TODO - this authenticates nothing: any non-blank header value is accepted, so the endpoint is
        //  effectively open. It checks presence only, which reads like security and is not. Compare against a
        //  configured static admin key (constant-time), reject with 401, and cover it with a CT before this
        //  endpoint is reachable from anywhere but a test. Static key is the intended mechanism until
        //  accounts arrive in Phase 6.
        // Temporary belt and braces
        if (StringUtil.isNullOrEmpty(adminKey)) {
            throw new RuntimeException("Admin Key is required");
        }

        return documentService.stageDocumentUpload(request);
    }
}
