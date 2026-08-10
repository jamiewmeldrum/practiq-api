package com.practiq.exception.handler;

import static io.micronaut.http.HttpStatus.UNAUTHORIZED;

import com.practiq.dto.response.ErrorResponse;
import com.practiq.exception.UnauthorizedException;
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
@Requires(classes = {UnauthorizedException.class, ExceptionHandler.class})
public class UnauthorizedExceptionHandler
        implements ExceptionHandler<UnauthorizedException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, UnauthorizedException exception) {
        log.debug("Unauthorised access: {} {}", request.getMethodName(), request.getUri());
        log.trace(exception.getMessage(), exception);
        return HttpResponseFactory.INSTANCE.status(
                UNAUTHORIZED, new ErrorResponse("Unauthorised to access " + request.getUri(), UNAUTHORIZED.getCode()));
    }
}
