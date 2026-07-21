package com.practiq.exception;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.UnsatisfiedRouteHandler;
import io.micronaut.web.router.exceptions.UnsatisfiedRouteException;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Produces
@Singleton
@Replaces(UnsatisfiedRouteHandler.class)
@Requires(classes = {UnsatisfiedRouteException.class, ExceptionHandler.class})
public class UnsatisfiedRouteExceptionHandler implements ExceptionHandler<UnsatisfiedRouteException, HttpResponse<ErrorResponse>> {
    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, UnsatisfiedRouteException exception) {
        log.debug(exception.getMessage());
        return HttpResponseFactory.INSTANCE.status(
                BAD_REQUEST,
                new ErrorResponse(exception.getMessage(), BAD_REQUEST.getCode())
        );
    }
}
