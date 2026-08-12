package utils.aws;

import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Singleton
public class S3TestUtils {

    private final S3Client s3Client;

    public S3TestUtils(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void ensureBucketExistsAndIsEmpty(String bucketName) {
        // The bucket needs to exist. It makes sense to make sure it is present but empty no matter what.
        try {
            emptyBucket(bucketName);
            s3Client.deleteBucket(
                    DeleteBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            // Not a problem as that just tells us the bucket wasn't there to start with, which is out desired state.
        }
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    }

    public void emptyBucket(String bucketName) {
        Set<ObjectIdentifier> objectsToDelete =
                s3Client
                        .listObjects(
                                ListObjectsRequest.builder().bucket(bucketName).build())
                        .contents()
                        .stream()
                        .map(S3Object::key)
                        .map(k -> ObjectIdentifier.builder().key(k).build())
                        .collect(Collectors.toSet());

        if (!objectsToDelete.isEmpty()) {
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objectsToDelete).build())
                    .build());
        }
    }

    public void createObject(String bucketName, String objectKey, String content) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(objectKey).build(), RequestBody.fromString(content));
    }

    public Optional<String> getContentType(String bucketName, String objectKey) {
        try {
            return Optional.of(s3Client.headObject(HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build())
                    .contentType());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    public Optional<String> getFileContent(String bucketName, String objectKey) throws IOException {
        try {
            return Optional.of(new String(
                    s3Client.getObject(GetObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(objectKey)
                                    .build())
                            .readAllBytes(),
                    StandardCharsets.UTF_8));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }
}
