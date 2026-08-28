package com.practiq.service.document;

import java.time.Duration;

public class DocumentUploadRules {

    public static final int MAX_CONTENT_LENGTH = 1024 * 1024 * 25; // 25Mb — a 300dpi scan of a
    // 40-page past paper lands around 15-20Mb, so this clears the realistic worst case.

    // Long enough for the ceiling above to finish on a sustained 0.5Mbps connection, and short enough
    // that the presigned URL — a bearer credential anyone holding it can upload with — is not useful for
    // long if it leaks.
    public static final Duration UPLOAD_URL_EXPIRY = Duration.ofMinutes(10);

    public static final Duration UPLOAD_COMPLETION_GRACE = Duration.ofMinutes(10);
}
