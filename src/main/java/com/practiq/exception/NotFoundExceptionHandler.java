package com.practiq.exception;

import com.practiq.dto.ErrorResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.NotFoundException;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

@Slf4j
@Produces
@Singleton
@Requires(classes = {NotFoundException.class, ExceptionHandler.class})
public class NotFoundExceptionHandler implements ExceptionHandler<NotFoundException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, NotFoundException exception) {
        log.debug("No resource matched: {} {}", request.getMethodName(), request.getUri());
        return HttpResponse.notFound(
                new ErrorResponse(
                        "Could not find resource for path: " + request.getUri(),
                        NOT_FOUND.getCode()
                )
        );
    }
}
