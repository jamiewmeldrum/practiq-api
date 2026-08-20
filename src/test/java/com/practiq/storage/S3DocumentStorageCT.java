package com.practiq.storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.MediaType;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import utils.ComponentTest;

@ComponentTest
@ExtendWith(MockitoExtension.class)
class S3DocumentStorageCT {

    private static final String DOCUMENTS_BUCKET = "documents";

    @Inject
    private S3Client s3Client;

    @Inject
    private S3DocumentStorage storage;

    @MockBean(S3Client.class)
    S3Client s3Client() {
        return mock(S3Client.class);
    }

    @Test
    void presignUploadBindsTheContentTypeLengthAndExpiryIntoTheSignature() {
        String key = "0e6f1b0a-1c3d-4f5e-8a9b-2c3d4e5f6a7b.pdf";
        int contentLength = 2048;
        Duration expiresIn = Duration.ofMinutes(10);

        Instant before = Instant.now();

        PresignedUpload presigned = storage.presignUpload(
                new PresignedUploadRequest(key, MediaType.APPLICATION_PDF_TYPE, contentLength, expiresIn));

        URI url = presigned.url();
        Map<String, String> query = queryParameters(url);

        assertEquals("documents.s3.eu-west-1.amazonaws.com", url.getHost(), "url was " + url);
        assertEquals("/" + key, url.getPath(), "url was " + url);
        assertEquals("AWS4-HMAC-SHA256", query.get("X-Amz-Algorithm"), "query was " + query);
        // S3 expresses the presign's lifetime in seconds, so ten minutes is signed as 600.
        assertEquals(String.valueOf(expiresIn.toSeconds()), query.get("X-Amz-Expires"), "query was " + query);
        assertEquals("content-length;content-type;host", query.get("X-Amz-SignedHeaders"), "query was " + query);
        assertEquals("test/eu-west-1/s3/aws4_request", credentialWithoutDate(query), "query was " + query);

        // The reported expiry is the presigner's own, so it must sit within the window that was asked for.
        assertTrue(presigned.expiresAt().isAfter(before), "expiresAt was " + presigned.expiresAt());
        assertTrue(
                presigned.expiresAt().isBefore(before.plus(expiresIn).plusSeconds(1)),
                "expiresAt was " + presigned.expiresAt());
    }

    @Test
    void canFilterToKeysThatExist() {
        String matchingKey1 = "matchingKey1";
        String matchingKey2 = "matchingKey2";
        String mismatchingKey1 = "mismatchingKey1";
        String mismatchingKey2 = "mismatchingKey2";
        Set<String> keysToCheck = Set.of(matchingKey1, matchingKey2, mismatchingKey1, mismatchingKey2);

        HeadObjectRequest matchingKey1Request = aHeadRequestForKey(matchingKey1);
        when(s3Client.headObject(matchingKey1Request))
                .thenReturn(HeadObjectResponse.builder().build());

        HeadObjectRequest matchingKey2Request = aHeadRequestForKey(matchingKey2);
        when(s3Client.headObject(matchingKey2Request))
                .thenReturn(HeadObjectResponse.builder().build());

        HeadObjectRequest mismatchingKey1Request = aHeadRequestForKey(mismatchingKey1);
        when(s3Client.headObject(mismatchingKey1Request)).thenThrow(NoSuchKeyException.class);

        HeadObjectRequest mismatchingKey2Request = aHeadRequestForKey(mismatchingKey2);
        when(s3Client.headObject(mismatchingKey2Request)).thenThrow(NoSuchKeyException.class);

        Set<String> matchedKeys = storage.filterToKeysThatExist(keysToCheck);
        assertThat(matchedKeys, containsInAnyOrder(matchingKey1, matchingKey2));

        verify(s3Client).headObject(matchingKey1Request);
        verify(s3Client).headObject(matchingKey2Request);
        verify(s3Client).headObject(mismatchingKey1Request);
        verify(s3Client).headObject(mismatchingKey2Request);
    }

    @Test
    void existsChecksForAllKeysRunConcurrently() {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        Set<String> keysToCheck = Set.of(key1, key2, key3);

        // Every check announces its arrival and then waits for the others, so the latch can only reach
        // zero if all three are in flight at once. Checking the keys one at a time strands the first
        // call on the await, and the test fails on its timeout rather than hanging.
        CountDownLatch allChecksArrived = new CountDownLatch(keysToCheck.size());
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenAnswer(invocation -> {
            allChecksArrived.countDown();
            if (!allChecksArrived.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("A key was checked before the others had started.");
            }
            return HeadObjectResponse.builder().build();
        });

        Set<String> matchedKeys = storage.filterToKeysThatExist(keysToCheck);
        assertThat(matchedKeys, containsInAnyOrder(key1, key2, key3));
    }

    @Test
    void awsServiceExceptionDuringSingleKeyExistsCheckCausesOperationFailure() {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        Set<String> keysToCheck = Set.of(key1, key2, key3);

        HeadObjectRequest errorRequest = aHeadRequestForKey(key3);
        when(s3Client.headObject(errorRequest)).thenThrow(AwsServiceException.class);

        assertThrows(CompletionException.class, () -> storage.filterToKeysThatExist(keysToCheck));
    }

    @Test
    void sdkClientExceptionDuringSingleKeyExistsCheckCausesOperationFailure() {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        Set<String> keysToCheck = Set.of(key1, key2, key3);

        HeadObjectRequest errorRequest = aHeadRequestForKey(key3);
        when(s3Client.headObject(errorRequest)).thenThrow(SdkClientException.class);

        assertThrows(CompletionException.class, () -> storage.filterToKeysThatExist(keysToCheck));
    }

    @Test
    void runtimeExceptionDuringSingleKeyExistsCheckCausesOperationFailure() {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        Set<String> keysToCheck = Set.of(key1, key2, key3);

        HeadObjectRequest errorRequest = aHeadRequestForKey(key2);
        when(s3Client.headObject(errorRequest)).thenThrow(RuntimeException.class);

        assertThrows(CompletionException.class, () -> storage.filterToKeysThatExist(keysToCheck));
    }

    private HeadObjectRequest aHeadRequestForKey(String matchingKey1) {
        return HeadObjectRequest.builder()
                .bucket(DOCUMENTS_BUCKET)
                .key(matchingKey1)
                .build();
    }

    private Map<String, String> queryParameters(URI url) {
        Map<String, String> parameters = new LinkedHashMap<>();
        Arrays.stream(url.getQuery().split("&")).forEach(parameter -> {
            int separator = parameter.indexOf('=');
            parameters.put(
                    parameter.substring(0, separator),
                    URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8));
        });
        return parameters;
    }

    // X-Amz-Credential is "<accessKey>/<yyyyMMdd>/<region>/<service>/aws4_request"; the date is today's
    // and would make the assertion a moving target, so it is dropped and the rest asserted whole.
    private String credentialWithoutDate(Map<String, String> query) {
        String[] credential = query.get("X-Amz-Credential").split("/");
        return credential[0] + "/" + credential[2] + "/" + credential[3] + "/" + credential[4];
    }
}
