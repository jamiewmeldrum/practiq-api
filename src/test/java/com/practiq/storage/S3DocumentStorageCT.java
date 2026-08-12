package com.practiq.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.http.MediaType;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

@ComponentTest
class S3DocumentStorageCT {

    @Inject
    private S3DocumentStorage storage;

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
