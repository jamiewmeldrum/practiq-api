package com.practiq.exception;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ConversionErrorHandler;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Produces
@Singleton
@Replaces(ConversionErrorHandler.class)
@Requires(classes = {ConversionErrorException.class, ExceptionHandler.class})
public class ConversionErrorExceptionHandler implements ExceptionHandler<ConversionErrorException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, ConversionErrorException exception) {
        log.debug(exception.getMessage());

        Argument<?> argument = exception.getArgument();
        String message = buildMessage(argument);

        return HttpResponseFactory.INSTANCE.status(
                BAD_REQUEST,
                new ErrorResponse(message, BAD_REQUEST.getCode())
        );
    }

    private String buildMessage(Argument<?> argument) {
        Class<?> enumType = resolveEnumType(argument);
        if (enumType != null) {
            String allowedValues = Arrays.stream(enumType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "%s: must be one of %s".formatted(argument.getName(), allowedValues);
        }
        return "%s: invalid value".formatted(argument.getName());
    }

    private Class<?> resolveEnumType(Argument<?> argument) {
        if (argument.getType().isEnum()) {
            return argument.getType();
        }
        return argument.getFirstTypeVariable()
                .map(Argument::getType)
                .filter(Class::isEnum)
                .orElse(null);
    }
}
