package com.practiq.exception;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Slf4j
@Produces
@Singleton
@Replaces(io.micronaut.validation.exceptions.ConstraintExceptionHandler.class)
@Requires(classes = {ConstraintViolationException.class, ExceptionHandler.class})
public class ConstraintViolationExceptionHandler implements ExceptionHandler<ConstraintViolationException, HttpResponse<ErrorResponse>> {
    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> constraintViolations = exception.getConstraintViolations();

        List<String> messages = constraintViolations.stream()
                .map(this::formatViolation)
                .sorted()
                .toList();

        String message = String.join("; ", messages);

        log.debug(message);

        return HttpResponseFactory.INSTANCE.status(
                HttpStatus.UNPROCESSABLE_ENTITY,
                new ErrorResponse(message, UNPROCESSABLE_ENTITY.getCode())
        );
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        return "%s: %s".formatted(lastPathNode(violation.getPropertyPath()), violation.getMessage());
    }

    private String lastPathNode(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name != null ? name : path.toString();
    }
}
