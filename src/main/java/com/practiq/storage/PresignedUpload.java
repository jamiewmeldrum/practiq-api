package com.practiq.storage;

import java.net.URI;
import java.time.Instant;

// The expiry is the presigner's own, not a value recomputed from the requested duration, so what the
// caller is told matches what the signature actually encodes.
public record PresignedUpload(URI url, Instant expiresAt) {}
