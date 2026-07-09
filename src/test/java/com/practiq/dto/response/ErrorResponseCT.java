package com.practiq.dto.response;

import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import utils.ComponentTest;

import java.io.IOException;

@ComponentTest
class ErrorResponseCT {

    @Inject
    private ObjectMapper objectMapper;

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
