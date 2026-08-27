package com.practiq.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import utils.ComponentTest;

@ComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3ClientConfigurerCT implements TestPropertyProvider {

    private static final Duration ATTEMPT_TIMEOUT = Duration.ofMillis(500);
    // Comfortably over the retried attempt timeouts and far under the 30s socket timeout the SDK
    // would otherwise fall back on, so only a configured attempt timeout gets the call under it.
    private static final Duration GIVE_UP_WITHIN = Duration.ofSeconds(5);

    private ServerSocket unresponsiveS3;

    @Inject
    private S3DocumentStorage s3DocumentStorage;

    // The socket is never accepted from, so the SDK's connection completes and then waits forever
    // for a response — the shape of an S3 that has stopped answering rather than refused.
    @Override
    public Map<String, String> getProperties() {
        try {
            unresponsiveS3 = new ServerSocket(0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Map.of(
                "aws.services.s3.endpoint-override", "http://127.0.0.1:" + unresponsiveS3.getLocalPort(),
                "practiq.s3.api-call-attempt-timeout", ATTEMPT_TIMEOUT.toMillis() + "ms");
    }

    @AfterAll
    void closeUnresponsiveS3() throws IOException {
        unresponsiveS3.close();
    }

    @Test
    void abandonsAnExistenceCheckAgainstAnS3ThatNeverResponds() {
        assertTimeoutPreemptively(
                GIVE_UP_WITHIN,
                () -> assertThrows(
                        CompletionException.class, () -> s3DocumentStorage.filterToKeysThatExist(Set.of("a-key"))));
    }
}
