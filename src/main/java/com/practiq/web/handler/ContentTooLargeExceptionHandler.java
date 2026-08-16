package com.practiq.web.handler;

import static com.practiq.web.dto.mapper.ErrorResponseMapper.toErrorResponse;
import static io.micronaut.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;

import com.practiq.foundation.exception.ContentTooLargeException;
import com.practiq.web.dto.response.ErrorResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Produces
@Singleton
@Requires(classes = {ContentTooLargeException.class, ExceptionHandler.class})
public class ContentTooLargeExceptionHandler
        implements ExceptionHandler<ContentTooLargeException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, ContentTooLargeException exception) {
        log.debug("Content too large: {} {}", request.getMethodName(), request.getUri());
        log.trace(exception.getMessage(), exception);

        return HttpResponseFactory.INSTANCE.status(
                REQUEST_ENTITY_TOO_LARGE, toErrorResponse(exception.error(), REQUEST_ENTITY_TOO_LARGE));
    }
}
