package com.practiq.dto.mapper;

import com.practiq.dto.response.ErrorResponse;
import com.practiq.exception.EntityValidationError;
import io.micronaut.http.HttpStatus;

public class ErrorResponseMapper {

    public static ErrorResponse toErrorResponse(EntityValidationError error, HttpStatus status) {
        return new ErrorResponse("%s: %s".formatted(error.errorField(), error.failureReason()), status.getCode());
    }
}
