package com.practiq.exception;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.web.router.exceptions.UnsatisfiedBodyRouteException;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Produces
@Singleton
@Requires(classes = {UnsatisfiedBodyRouteException.class, ExceptionHandler.class})
public class UnsatisfiedBodyRouteExceptionHandler implements ExceptionHandler<UnsatisfiedBodyRouteException, HttpResponse<ErrorResponse>> {
    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, UnsatisfiedBodyRouteException exception) {
        log.debug(exception.getMessage());

        return HttpResponseFactory.INSTANCE.status(
                BAD_REQUEST,
                new ErrorResponse("Request body not specified", BAD_REQUEST.getCode())
        );
    }
}
