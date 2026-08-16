package com.practiq.web.handler;

import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practiq.web.dto.response.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;

class GenericExceptionHandlerTest {

    private final GenericExceptionHandler handler = new GenericExceptionHandler();

    @Test
    void handleBuildsTheServerErrorEnvelopeWithoutLeakingTheCause() {
        HttpRequest<?> request = HttpRequest.GET("/api/v1/questions");

        HttpResponse<ErrorResponse> response =
                handler.handle(request, new IllegalStateException("connection pool exhausted at 10.0.0.4:5432"));

        assertEquals(INTERNAL_SERVER_ERROR.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("An unspecified error occurred.", body.error());
        assertEquals(INTERNAL_SERVER_ERROR.getCode(), body.status());
    }
}
