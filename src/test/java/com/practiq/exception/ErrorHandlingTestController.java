package com.practiq.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.QueryValue;
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

    @Get("/optimistic-lock")
    public String throwsOptimisticLock() {
        throw new OptimisticLockException("stale version");
    }
}
