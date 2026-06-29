package com.practiq.dto;

import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;

class ErrorResponseTest {

    private final ObjectMapper objectMapper = ObjectMapper.getDefault();

    @Test
    void errorResponseSerializesFull() throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                "Test Error Response",
                999
        );
        String actual = objectMapper.writeValueAsString(errorResponse);

        String expected = """
        {
          "error": "Test Error Response",
          "status": 999
        }
        """;

        JSONAssert.assertEquals(expected, actual, true);
    }
}
