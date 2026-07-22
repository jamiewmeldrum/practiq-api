package com.practiq.exception;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.UnsatisfiedArgumentHandler;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Produces
@Singleton
@Replaces(UnsatisfiedArgumentHandler.class)
@Requires(classes = {UnsatisfiedArgumentException.class, ExceptionHandler.class})
public class UnsatisfiedArgumentExceptionHandler implements ExceptionHandler<UnsatisfiedArgumentException, HttpResponse<ErrorResponse>> {
    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, UnsatisfiedArgumentException exception) {
        log.debug(exception.getMessage());

        Argument<?> argument = exception.getArgument();
        return HttpResponseFactory.INSTANCE.status(
                BAD_REQUEST,
                new ErrorResponse(argument.getName() + ": argument not specified", BAD_REQUEST.getCode())
        );
    }
}
