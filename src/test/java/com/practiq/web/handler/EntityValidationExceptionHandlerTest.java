package com.practiq.web.handler;

import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practiq.foundation.exception.EntityValidationException;
import com.practiq.web.dto.response.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;

class EntityValidationExceptionHandlerTest {

    private final EntityValidationExceptionHandler handler = new EntityValidationExceptionHandler();

    @Test
    void handleBuildsUnprocessableEntityEnvelopeFromTheExceptionMessage() {
        HttpRequest<?> request = HttpRequest.POST("/api/v1/admin/documents", "{}");

        HttpResponse<ErrorResponse> response = handler.handle(
                request, new EntityValidationException("contentType", "'text/csv' is not a supported content type"));

        assertEquals(UNPROCESSABLE_ENTITY.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("contentType: 'text/csv' is not a supported content type", body.error());
        assertEquals(UNPROCESSABLE_ENTITY.getCode(), body.status());
    }
}
