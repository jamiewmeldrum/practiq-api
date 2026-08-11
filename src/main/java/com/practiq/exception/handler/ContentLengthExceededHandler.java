package com.practiq.exception.handler;

import static io.micronaut.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.exceptions.ContentLengthExceededException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Produces
@Singleton
@Replaces(io.micronaut.http.server.exceptions.ContentLengthExceededHandler.class)
@Requires(classes = {ContentLengthExceededException.class, ExceptionHandler.class})
public class ContentLengthExceededHandler
        implements ExceptionHandler<ContentLengthExceededException, HttpResponse<ErrorResponse>> {
    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, ContentLengthExceededException exception) {
        log.debug("Content length too long: {} {}", request.getMethodName(), request.getUri());
        log.trace(exception.getMessage(), exception);
        return HttpResponseFactory.INSTANCE.status(
                REQUEST_ENTITY_TOO_LARGE,
                new ErrorResponse(
                        "Content length too long for request made to url " + request.getUri(),
                        REQUEST_ENTITY_TOO_LARGE.getCode()));
    }
}
