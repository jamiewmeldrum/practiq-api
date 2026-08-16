package com.practiq.web.handler;

import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practiq.web.dto.response.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.server.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;

class NotFoundExceptionHandlerTest {

    private final NotFoundExceptionHandler handler = new NotFoundExceptionHandler();

    @Test
    void handleBuildsNotFoundEnvelopeFromTheRequestPath() {
        HttpRequest<?> request = HttpRequest.GET("/api/v1/unmapped");

        HttpResponse<ErrorResponse> response = handler.handle(request, new NotFoundException());

        assertEquals(NOT_FOUND.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("Could not find resource for path: /api/v1/unmapped", body.error());
        assertEquals(NOT_FOUND.getCode(), body.status());
    }
}
