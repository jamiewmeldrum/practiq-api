package integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import io.micronaut.objectstorage.aws.AwsS3Operations;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import utils.IntegrationTest;
import utils.aws.S3TestUtils;

/**
 * TODO - remove once document presigning complete
 * Not a permanent test fixture, just some test operations to prove integration with s3 local stack and proof
 * around how to test using localstack. To be removed once intentional features added and tested.
 */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AWSSmokeTestIT {

    private static final String TARGET_BUCKET = "documents";

    @Inject
    @Named(value = TARGET_BUCKET)
    private AwsS3Operations objectStorage;

    @Inject
    private S3TestUtils s3TestUtils;

    @BeforeAll
    public void setup() {
        s3TestUtils.ensureBucketExistsAndIsEmpty(TARGET_BUCKET);
    }

    @BeforeEach
    public void clear() {
        s3TestUtils.emptyBucket(TARGET_BUCKET);
    }

    @Test
    public void smokeTest() throws IOException {
        String documentKey = "key1";
        String content = "content1";

        assertThat(objectStorage.exists(documentKey), equalTo(false));
        s3TestUtils.createObject(TARGET_BUCKET, documentKey, content);

        InputStream retrievedContentInputStream =
                objectStorage.retrieve(documentKey).get().getInputStream();
        String retrievedContent = new String(retrievedContentInputStream.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(retrievedContent, equalTo(content));

        assertThat(objectStorage.exists(documentKey), equalTo(true));

        String otherDocumentKey = "key2";
        String otherContent = "content2";
        s3TestUtils.createObject(TARGET_BUCKET, otherDocumentKey, otherContent);

        InputStream otherRetrievedContentInputStream =
                objectStorage.retrieve(otherDocumentKey).get().getInputStream();
        String otherRetrievedContent =
                new String(otherRetrievedContentInputStream.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(otherRetrievedContent, equalTo(otherContent));

        Set<String> objectKeys = objectStorage.listObjects();
        assertThat(objectKeys, contains(documentKey, otherDocumentKey));
    }
}
