package com.practiq.storage;

import com.practiq.util.StringUtil;
import io.micronaut.http.MediaType;
import java.time.Duration;

// Guards its own values because presignUpload is a public entry point to this component: a blank key,
// an impossible length or a spent expiry all sign successfully and hand the caller a plausible URL
// that can never be used, rather than failing where the mistake was made.
public record PresignedUploadRequest(String key, MediaType contentType, int contentLength, Duration expiresIn) {

    public PresignedUploadRequest {
        if (StringUtil.isBlank(key)) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        if (contentLength < 1) {
            throw new IllegalArgumentException("contentLength must be greater than or equal to 1");
        }
        if (expiresIn == null || expiresIn.isZero() || expiresIn.isNegative()) {
            throw new IllegalArgumentException("expiresIn must be a positive duration");
        }
    }
}
