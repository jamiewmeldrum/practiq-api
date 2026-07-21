package com.practiq.exception;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.web.router.exceptions.UnsatisfiedHeaderRouteException;
import io.micronaut.web.router.exceptions.UnsatisfiedQueryValueRouteException;
import io.micronaut.web.router.exceptions.UnsatisfiedRouteException;
import org.junit.jupiter.api.Test;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UnsatisfiedRouteExceptionHandlerTest {

    private final UnsatisfiedRouteExceptionHandler handler = new UnsatisfiedRouteExceptionHandler();

    // The handler surfaces getMessage() (which carries the wire header name), not getArgument().getName()
    // (the method-parameter name). The fixture makes the two differ so a regression to the argument name fails.
    @Test
    void handlerPassesMessageFromExceptionAndSets400Error() {
        Argument<?> methodParameter = Argument.of(String.class, "sessionToken");
        UnsatisfiedRouteException exception = new UnsatisfiedHeaderRouteException("X-Session-Token", methodParameter);

        HttpResponse<ErrorResponse> response =
                handler.handle(HttpRequest.GET("/api/v1/questions/4/attempts"), exception);

        assertEquals(BAD_REQUEST.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("Required Header [X-Session-Token] not specified", body.error());
        assertEquals(BAD_REQUEST.getCode(), body.status());
    }

    // The handler binds to the abstract parent, so it serves every unsatisfied-binding flavour, not just
    // headers. Exercising a sibling proves that breadth and pins the handler's parameter to the parent type:
    // narrowing it to UnsatisfiedHeaderRouteException would stop this compiling.
    @Test
    void handlerAlsoServesSiblingUnsatisfiedRouteExceptions() {
        Argument<?> methodParameter = Argument.of(String.class, "conceptId");
        UnsatisfiedRouteException exception = new UnsatisfiedQueryValueRouteException("conceptId", methodParameter);

        HttpResponse<ErrorResponse> response =
                handler.handle(HttpRequest.GET("/api/v1/questions"), exception);

        assertEquals(BAD_REQUEST.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("Required QueryValue [conceptId] not specified", body.error());
        assertEquals(BAD_REQUEST.getCode(), body.status());
    }
}
