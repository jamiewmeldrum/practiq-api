package com.practiq.exception;

import com.practiq.dto.ErrorResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.NotFoundException;
import jakarta.inject.Singleton;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

// TODO: this only covers 404. Other failures (400 binding/conversion, 422 validation, 500)
// still return Micronaut's default error body, not the {error, status} envelope. Add a generic
// fallback ExceptionHandler so the whole API shares one error contract — see CLAUDE.md §7.
@Produces
@Singleton
@Requires(classes = {NotFoundException.class, ExceptionHandler.class})
public class NotFoundExceptionHandler implements ExceptionHandler<NotFoundException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, NotFoundException exception) {

        return HttpResponse.notFound(
                new ErrorResponse(
                        "Could not find resource for path: " + request.getUri(),
                        NOT_FOUND.getCode()
                )
        );
    }
}
