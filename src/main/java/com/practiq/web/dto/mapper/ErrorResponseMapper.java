package com.practiq.web.dto.mapper;

import com.practiq.foundation.exception.EntityValidationError;
import com.practiq.web.dto.response.ErrorResponse;
import io.micronaut.http.HttpStatus;

public class ErrorResponseMapper {

    public static ErrorResponse toErrorResponse(EntityValidationError error, HttpStatus status) {
        return new ErrorResponse("%s: %s".formatted(error.errorField(), error.failureReason()), status.getCode());
    }
}
