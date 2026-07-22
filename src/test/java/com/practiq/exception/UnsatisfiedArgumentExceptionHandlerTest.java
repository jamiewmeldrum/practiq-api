package com.practiq.exception;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UnsatisfiedArgumentExceptionHandlerTest {

    private final UnsatisfiedArgumentExceptionHandler handler = new UnsatisfiedArgumentExceptionHandler();

    @Test
    void handlerBuildsMessageFromExceptionAndSets400Error() {
        Argument<?> argument = Argument.of(String.class, "arg");
        UnsatisfiedArgumentException exception = new UnsatisfiedArgumentException(argument);

        HttpResponse<ErrorResponse> response =
                handler.handle(HttpRequest.GET("/api/v1/questions/4/attempts"), exception);

        assertEquals(BAD_REQUEST.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("arg: argument not specified", body.error());
        assertEquals(BAD_REQUEST.getCode(), body.status());
    }
}
