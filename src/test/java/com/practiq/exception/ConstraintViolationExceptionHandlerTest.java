package com.practiq.exception;

import com.practiq.dto.response.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConstraintViolationExceptionHandlerTest {

    private final ConstraintViolationExceptionHandler handler = new ConstraintViolationExceptionHandler();

    @Test
    void handleReports422AndFormatsUsingTheLeafPropertyName() {
        // A real bean-validation path is prefixed with the method and parameter (get.request.types);
        // the handler must report only the leaf so the client sees the field, not the plumbing.
        ConstraintViolation<?> violation =
                mockViolation("size must be between 0 and 3", "get", "request", "types");

        ErrorResponse body = handle(violation);

        assertEquals("types: size must be between 0 and 3", body.error());
        assertEquals(UNPROCESSABLE_ENTITY.getCode(), body.status());
    }

    @Test
    void handleSortsViolationsAndJoinsThemWithSemicolons() {
        // Two violations handed in as an unordered set; the handler sorts the formatted strings so the
        // output is deterministic regardless of set iteration order.
        ConstraintViolation<?> types = mockViolation("size must be between 0 and 3", "types");
        ConstraintViolation<?> difficulty = mockViolation("must be greater than 0", "difficulty");

        ErrorResponse body = handle(types, difficulty);

        assertEquals(
                "difficulty: must be greater than 0; types: size must be between 0 and 3",
                body.error());
        assertEquals(UNPROCESSABLE_ENTITY.getCode(), body.status());
    }

    private ErrorResponse handle(ConstraintViolation<?>... violations) {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violations));

        HttpResponse<ErrorResponse> response =
                handler.handle(HttpRequest.GET("/api/v1/questions"), exception);

        assertEquals(UNPROCESSABLE_ENTITY.getCode(), response.getStatus().getCode());
        ErrorResponse body = response.body();
        assertNotNull(body);
        return body;
    }

    // Builds a violation whose property path iterates the given nodes (last one is the leaf the
    // handler reports) and whose message is fixed, so formatting is asserted independently of any
    // validation provider's message catalogue.
    private ConstraintViolation<?> mockViolation(String message, String... pathNodes) {
        List<Path.Node> nodes = Arrays.stream(pathNodes)
                .map(name -> {
                    Path.Node node = mock(Path.Node.class);
                    when(node.getName()).thenReturn(name);
                    return node;
                })
                .toList();

        Path path = mock(Path.class);
        when(path.iterator()).thenReturn(nodes.iterator());

        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn(message);
        when(violation.getPropertyPath()).thenReturn(path);
        return violation;
    }
}
