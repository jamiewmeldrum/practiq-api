package com.practiq.storage;

import io.micronaut.http.MediaType;
import io.micronaut.objectstorage.aws.AwsS3Operations;
import io.micronaut.objectstorage.request.PresignRequest;
import io.micronaut.objectstorage.response.PresignResponse;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.net.URI;
import java.time.Duration;

@Singleton
public class S3DocumentStorage {

    private static final String TARGET_BUCKET = "documents";

    public static final int UPLOAD_TIMEOUT_MINUTES = 10;

    private final AwsS3Operations documentsBucket;

    public S3DocumentStorage(@Named(value = TARGET_BUCKET) AwsS3Operations documentsBucket) {
        this.documentsBucket = documentsBucket;
    }

    public URI generatePresignedUploadURI(String key, MediaType contentType, long contentLength) {
        PresignRequest request = PresignRequest.builder(key, PresignRequest.Operation.UPLOAD)
                .contentLength(contentLength)
                .expiresIn(Duration.ofMinutes(UPLOAD_TIMEOUT_MINUTES))
                .contentType(contentType.getName())
                .build();
        PresignResponse presignResponse = documentsBucket.presign(request);
        return presignResponse.url();
    }
}
