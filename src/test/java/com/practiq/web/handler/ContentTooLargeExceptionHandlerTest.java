package com.practiq.web.handler;

import static io.micronaut.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practiq.foundation.exception.ContentTooLargeException;
import com.practiq.web.dto.response.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;

class ContentTooLargeExceptionHandlerTest {

    private final ContentTooLargeExceptionHandler handler = new ContentTooLargeExceptionHandler();

    @Test
    void handleBuildsRequestEntityTooLargeEnvelopeFromTheValidationError() {
        HttpRequest<?> request = HttpRequest.POST("/api/v1/admin/documents", "{}");

        HttpResponse<ErrorResponse> response = handler.handle(
                request, new ContentTooLargeException("contentLength", "must not be greater than 5242880"));

        assertEquals(REQUEST_ENTITY_TOO_LARGE.getCode(), response.getStatus().getCode());

        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("contentLength: must not be greater than 5242880", body.error());
        assertEquals(REQUEST_ENTITY_TOO_LARGE.getCode(), body.status());
    }
}
