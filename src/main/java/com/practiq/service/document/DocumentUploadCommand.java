package com.practiq.service.document;

import com.practiq.util.StringUtil;

// A staging request that has already been checked for shape, so DocumentStager is left with the
// rules that are its own — the size cap, the allow-list, and the declared-versus-derived match.
// Reaching here with a bad value means a caller has a bug, so these fail loudly rather than
// becoming an EntityValidationException the client would see as a 422.
public record DocumentUploadCommand(String filename, String contentType, Integer contentLength, String sourceSpec) {

    public DocumentUploadCommand {
        if (StringUtil.isBlank(filename)) {
            throw new IllegalArgumentException("filename must not be blank");
        }
        if (StringUtil.isBlank(contentType)) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        if (contentLength == null) {
            throw new IllegalArgumentException("contentLength must not be null");
        }
        if (contentLength < 1) {
            throw new IllegalArgumentException("contentLength must be greater than or equal to 1");
        }
    }
}
