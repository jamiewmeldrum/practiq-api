package com.practiq.exception.handler;

import static com.practiq.dto.mapper.ErrorResponseMapper.toErrorResponse;
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.practiq.dto.response.ErrorResponse;
import com.practiq.exception.EntityValidationException;
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
@Requires(classes = {EntityValidationException.class, ExceptionHandler.class})
public class EntityValidationExceptionHandler
        implements ExceptionHandler<EntityValidationException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, EntityValidationException exception) {
        log.debug("Invalid entity state: {} {}", request.getMethodName(), request.getUri());
        log.trace(exception.getMessage(), exception);

        return HttpResponseFactory.INSTANCE.status(
                UNPROCESSABLE_ENTITY, toErrorResponse(exception.error(), UNPROCESSABLE_ENTITY));
    }
}
