package com.practiq.service.document;

import java.time.Duration;

// The product limits on registering a document upload, in one place because they are read together
// and argued about together. Static because they are values, not behaviour — a bean would add
// lifecycle to a pair of constants and buy nothing.
public class DocumentUploadRules {

    public static final int MAX_CONTENT_LENGTH = 1024 * 1024 * 25; // 25Mb — a 300dpi scan of a
    // 40-page past paper lands around 15-20Mb, so this clears the realistic worst case.

    public static final Duration UPLOAD_URL_EXPIRY = Duration.ofMinutes(10);
}
