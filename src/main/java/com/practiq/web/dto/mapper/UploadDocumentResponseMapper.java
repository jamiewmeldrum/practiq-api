package com.practiq.web.dto.mapper;

import com.practiq.service.document.dto.response.DocumentPresignUpload;
import com.practiq.web.dto.response.UploadDocumentResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UploadDocumentResponseMapper {
    public static UploadDocumentResponse toUploadDocumentResponse(DocumentPresignUpload documentPresignUpload) {
        log.trace("Converting DocumentPresignUpload to UploadDocumentResponse: {}", documentPresignUpload.id());

        return new UploadDocumentResponse(
                documentPresignUpload.id(), documentPresignUpload.url(), documentPresignUpload.expiresAt());
    }
}
