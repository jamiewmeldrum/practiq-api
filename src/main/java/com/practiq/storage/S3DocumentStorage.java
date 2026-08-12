package com.practiq.storage;

import static com.practiq.storage.FileType.*;

import io.micronaut.objectstorage.aws.AwsS3Operations;
import io.micronaut.objectstorage.request.PresignRequest;
import io.micronaut.objectstorage.response.PresignResponse;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.net.URI;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

@Singleton
public class S3DocumentStorage {

    private static final String TARGET_BUCKET = "documents";

    private static final Set<FileType> ALLOWED_FILE_TYPES =
            EnumSet.of(PDF, JPG, JPEG, PNG, GIF, WEBP, SVG, BMP, TXT, DOC, DOCX);

    private final AwsS3Operations documentsBucket;

    public S3DocumentStorage(@Named(value = TARGET_BUCKET) AwsS3Operations documentsBucket) {
        this.documentsBucket = documentsBucket;
    }

    public URI generatePresignedUploadURI(String key, String contentType, long contentLength) {
        PresignRequest request = PresignRequest.builder(key, PresignRequest.Operation.UPLOAD)
                .contentLength(contentLength)
                .expiresIn(Duration.ofMinutes(10))
                .contentType(contentType)
                .build();
        PresignResponse presignResponse = documentsBucket.presign(request);
        return presignResponse.url();
    }

    // TODO - split out storage rules. Think about this.
    public boolean isAcceptedFileType(FileType fileType) {
        return ALLOWED_FILE_TYPES.contains(fileType);
    }
}
