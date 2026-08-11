package com.practiq.exception.handler;

import static io.micronaut.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.exceptions.ContentLengthExceededException;
import org.junit.jupiter.api.Test;

class ContentLengthExceededHandlerTest {

    @Test
    void handleBuildsContentLengthTooLongEnvelope() {
        HttpRequest<?> request = HttpRequest.GET("/api/v1/too-long");

        ContentLengthExceededHandler handler = new ContentLengthExceededHandler();
        HttpResponse<ErrorResponse> response = handler.handle(request, new ContentLengthExceededException("Error"));

        assertEquals(REQUEST_ENTITY_TOO_LARGE.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("Content length too long for request made to url /api/v1/too-long", body.error());
        assertEquals(REQUEST_ENTITY_TOO_LARGE.getCode(), body.status());
    }
}
