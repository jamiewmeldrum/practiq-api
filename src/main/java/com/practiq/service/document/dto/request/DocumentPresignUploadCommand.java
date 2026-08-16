package com.practiq.service.document.dto.request;

import com.practiq.foundation.util.StringUtil;
import com.practiq.persistence.DocumentEntity;

// A staging request that has already been checked for shape, so DocumentStager is left with the
// rules that are its own — the size cap, the allow-list, and the declared-versus-derived match.
// Reaching here with a bad value means a caller has a bug, so these fail loudly rather than
// becoming an EntityValidationException the client would see as a 422.
public record DocumentPresignUploadCommand(String filename, String contentType, int contentLength, String sourceSpec) {

    public DocumentPresignUploadCommand {
        if (StringUtil.isBlank(filename)) {
            throw new IllegalArgumentException("filename must not be blank");
        }

        if (filename.length() > DocumentEntity.FILENAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "filename cannot exceed max length " + DocumentEntity.FILENAME_MAX_LENGTH);
        }

        if (StringUtil.isBlank(contentType)) {
            throw new IllegalArgumentException("contentType must not be blank");
        }

        if (contentLength < 1) {
            throw new IllegalArgumentException("contentLength must be greater than or equal to 1");
        }

        if (sourceSpec != null && sourceSpec.length() > DocumentEntity.SOURCE_SPEC_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "sourceSpec cannot exceed max length " + DocumentEntity.SOURCE_SPEC_MAX_LENGTH);
        }
    }
}
