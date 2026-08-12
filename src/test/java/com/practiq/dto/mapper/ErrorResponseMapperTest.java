package com.practiq.dto.mapper;

import static io.micronaut.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.practiq.dto.response.ErrorResponse;
import com.practiq.exception.EntityValidationError;
import org.junit.jupiter.api.Test;

class ErrorResponseMapperTest {

    @Test
    void toErrorResponseJoinsTheFieldAndReasonAndCarriesTheStatusCode() {
        EntityValidationError error = new EntityValidationError("contentType", "must not be blank");

        ErrorResponse response = ErrorResponseMapper.toErrorResponse(error, UNPROCESSABLE_ENTITY);

        assertEquals("contentType: must not be blank", response.error());
        assertEquals(422, response.status());
    }

    @Test
    void toErrorResponseTakesItsStatusCodeFromTheStatusItIsGiven() {
        EntityValidationError error = new EntityValidationError("contentLength", "must not be greater than 5242880");

        ErrorResponse response = ErrorResponseMapper.toErrorResponse(error, REQUEST_ENTITY_TOO_LARGE);

        assertEquals("contentLength: must not be greater than 5242880", response.error());
        assertEquals(413, response.status());
    }
}
