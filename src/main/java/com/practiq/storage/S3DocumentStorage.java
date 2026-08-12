package com.practiq.storage;

import io.micronaut.objectstorage.aws.AwsS3Operations;
import io.micronaut.objectstorage.request.PresignRequest;
import io.micronaut.objectstorage.response.PresignResponse;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class S3DocumentStorage {

    private static final String TARGET_BUCKET = "documents";

    private final AwsS3Operations documentsBucket;

    public S3DocumentStorage(@Named(value = TARGET_BUCKET) AwsS3Operations documentsBucket) {
        this.documentsBucket = documentsBucket;
    }

    public PresignedUpload presignUpload(PresignedUploadRequest uploadRequest) {
        PresignRequest request = PresignRequest.builder(uploadRequest.key(), PresignRequest.Operation.UPLOAD)
                .contentLength(uploadRequest.contentLength())
                .expiresIn(uploadRequest.expiresIn())
                .contentType(uploadRequest.contentType().getName())
                .build();

        PresignResponse presignResponse = documentsBucket.presign(request);

        return new PresignedUpload(presignResponse.url(), presignResponse.expiration());
    }
}
