package com.practiq.exception.handler;

import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practiq.dto.response.ErrorResponse;
import com.practiq.exception.UnauthorizedException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;

class UnauthorizedExceptionHandlerTest {

    @Test
    void handleBuildsUnauthorisedEnvelopeFromTheRequestPath() {
        HttpRequest<?> request = HttpRequest.GET("/api/v1/unauthorised");

        UnauthorizedExceptionHandler handler = new UnauthorizedExceptionHandler();
        HttpResponse<ErrorResponse> response = handler.handle(request, new UnauthorizedException("BadHeader"));

        assertEquals(UNAUTHORIZED.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        assertEquals("Unauthorised to access /api/v1/unauthorised", body.error());
        assertEquals(UNAUTHORIZED.getCode(), body.status());
    }
}
