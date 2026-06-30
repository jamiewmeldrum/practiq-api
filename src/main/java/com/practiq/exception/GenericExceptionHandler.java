package com.practiq.exception;

import com.practiq.dto.ErrorResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;


@Slf4j
@Produces
@Singleton
@Requires(classes = {Exception.class, ExceptionHandler.class})
public class GenericExceptionHandler implements ExceptionHandler<Exception, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, Exception exception) {
        log.error("An unexpected error occurred. This should be investigated", exception);
        return HttpResponse.serverError(
                new ErrorResponse(
                        "An unspecified error occurred.",
                        INTERNAL_SERVER_ERROR.getCode()
                )
        );
    }
}
