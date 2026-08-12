package com.practiq.exception.handler;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.exceptions.ContentLengthExceededException;
import jakarta.persistence.OptimisticLockException;

// Test-only controller for ErrorHandlingCT: endpoints whose sole job is to provoke each failure the
// global handlers must catch. Scoped to that test via the spec.name idiom. See the README testing section.
@Requires(property = "spec.name", value = "ErrorHandlingCT")
@Controller("/test/errors")
class ErrorHandlingTestController {

    @Get("/required-header")
    public String requiresHeader(@Header("X-Required-Header") String header) {
        return header;
    }

    @Get("/required-query")
    public String requiresQuery(@QueryValue String requiredParam) {
        return requiredParam;
    }

    @Get("/runtime-error")
    public String throwsRuntimeException() {
        throw new RuntimeException("Test Error");
    }

    @Get("/content-length-exceeded")
    public String throwsContentLengthExceeded() {
        throw new ContentLengthExceededException("body too large");
    }

    @Post("/echo")
    @Consumes(MediaType.TEXT_PLAIN)
    public String echo(@Body String body) {
        return body;
    }

    @Get("/optimistic-lock")
    public String throwsOptimisticLock() {
        throw new OptimisticLockException("stale version");
    }
}
