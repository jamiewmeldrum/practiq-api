package com.practiq.web.handler;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practiq.web.dto.response.ErrorResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.web.router.exceptions.UnsatisfiedBodyRouteException;
import org.junit.jupiter.api.Test;

class UnsatisfiedBodyRouteExceptionHandlerTest {
    private final UnsatisfiedBodyRouteExceptionHandler handler = new UnsatisfiedBodyRouteExceptionHandler();

    @Test
    void handlerBuildsMessageFromExceptionAndSets400Error() {
        Argument<?> argument = Argument.of(String.class, "arg");
        UnsatisfiedBodyRouteException exception = new UnsatisfiedBodyRouteException("masked", argument);

        HttpResponse<ErrorResponse> response =
                handler.handle(HttpRequest.GET("/api/v1/questions/4/attempts"), exception);

        assertEquals(BAD_REQUEST.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("Request body not specified", body.error());
        assertEquals(BAD_REQUEST.getCode(), body.status());
    }
}
