package com.practiq.service.document.dto.response;

import java.time.Instant;

public record DocumentPresignUpload(long id, String url, Instant expiresAt) {}
