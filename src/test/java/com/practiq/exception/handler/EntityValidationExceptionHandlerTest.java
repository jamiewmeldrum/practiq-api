package com.practiq.exception.handler;

import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practiq.dto.response.ErrorResponse;
import com.practiq.exception.EntityValidationException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;

class EntityValidationExceptionHandlerTest {

    @Test
    void handleBuildsUnprocessableEntityEnvelopeFromTheExceptionMessage() {
        HttpRequest<?> request = HttpRequest.POST("/api/v1/admin/documents", "{}");

        EntityValidationExceptionHandler handler = new EntityValidationExceptionHandler();
        HttpResponse<ErrorResponse> response =
                handler.handle(request, new EntityValidationException("Unsupported media type specified: text/csv"));

        assertEquals(UNPROCESSABLE_ENTITY.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("Unsupported media type specified: text/csv", body.error());
        assertEquals(UNPROCESSABLE_ENTITY.getCode(), body.status());
    }
}
