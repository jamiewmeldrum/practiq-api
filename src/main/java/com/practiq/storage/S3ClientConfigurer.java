package com.practiq.storage;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import java.time.Duration;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Singleton
public class S3ClientConfigurer implements BeanCreatedEventListener<S3ClientBuilder> {

    public static final String API_CALL_ATTEMPT_TIMEOUT_CONFIG_PARAM = "${practiq.s3.api-call-attempt-timeout}";

    private final Duration apiCallAttemptTimeout;

    public S3ClientConfigurer(@Value(API_CALL_ATTEMPT_TIMEOUT_CONFIG_PARAM) Duration apiCallAttemptTimeout) {
        this.apiCallAttemptTimeout = apiCallAttemptTimeout;
    }

    // Reading the existing configuration rather than replacing it keeps the user-agent suffix
    // Micronaut sets on the builder before this listener sees it.
    @Override
    public S3ClientBuilder onCreated(BeanCreatedEvent<S3ClientBuilder> event) {
        S3ClientBuilder builder = event.getBean();
        return builder.overrideConfiguration(builder.overrideConfiguration().toBuilder()
                .apiCallAttemptTimeout(apiCallAttemptTimeout)
                .build());
    }
}
