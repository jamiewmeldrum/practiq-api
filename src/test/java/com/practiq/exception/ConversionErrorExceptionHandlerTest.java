package com.practiq.exception;

import com.practiq.domain.types.QuestionType;
import com.practiq.dto.response.ErrorResponse;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConversionErrorExceptionHandlerTest {

    private final ConversionErrorExceptionHandler handler = new ConversionErrorExceptionHandler();

    // The real scenario: ?types=BAD binds to a List<QuestionType>, so the failing argument is a
    // collection and the enum has to be resolved from its type variable.
    @Test
    void handleListOfEnumArgumentListsTheAllowedEnumValues() {
        Argument<?> argument = Argument.of(List.class, "types", Argument.of(QuestionType.class));

        ErrorResponse body = handle(argument);

        assertEquals("types: must be one of SHORT_ANSWER, EXTENDED, MCQ", body.error());
        assertEquals(BAD_REQUEST.getCode(), body.status());
    }

    // A scalar enum argument: the enum is the argument's own type, not a type variable.
    @Test
    void handleScalarEnumArgumentListsTheAllowedEnumValues() {
        Argument<?> argument = Argument.of(QuestionType.class, "type");

        ErrorResponse body = handle(argument);

        assertEquals("type: must be one of SHORT_ANSWER, EXTENDED, MCQ", body.error());
        assertEquals(BAD_REQUEST.getCode(), body.status());
    }

    // A non-enum argument can't enumerate allowed values, so it falls back to a generic message.
    @Test
    void handleNonEnumArgumentFallsBackToGenericMessage() {
        Argument<?> argument = Argument.of(Integer.class, "difficulty");

        ErrorResponse body = handle(argument);

        assertEquals("difficulty: invalid value", body.error());
        assertEquals(BAD_REQUEST.getCode(), body.status());
    }

    private ErrorResponse handle(Argument<?> argument) {
        ConversionError error = () -> new IllegalArgumentException("bad value");
        ConversionErrorException exception = new ConversionErrorException(argument, error);

        HttpResponse<ErrorResponse> response = handler.handle(HttpRequest.GET("/api/v1/questions"), exception);

        assertEquals(BAD_REQUEST.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        return body;
    }
}
