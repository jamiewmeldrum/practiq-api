package com.practiq.dto.response;

import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import utils.ComponentTest;

import java.io.IOException;
import java.time.Instant;

@ComponentTest
class MarkSchemeResponseCT {

    @Inject
    private ObjectMapper objectMapper;

    @Test
    void markSchemeResponseSerializesFull() throws IOException {

        MarkSchemeResponse response = new MarkSchemeResponse(
                1L,
                10L,
                "Mark scheme body",
                Instant.parse("2026-06-29T10:15:30Z")
        );

        String actual = objectMapper.writeValueAsString(response);

        String expected = """
        {
          "id": 1,
          "questionId": 10,
          "body": "Mark scheme body",
          "createdAt": "2026-06-29T10:15:30Z"
        }
        """;

        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    void markSchemeResponseIncludesNullFields() throws IOException {

        MarkSchemeResponse response = new MarkSchemeResponse(
                -1,
                -2,
                null,
                null
        );

        String actual = objectMapper.writeValueAsString(response);

        String expected = """
        {
          "id": -1,
          "questionId": -2,
          "body": null,
          "createdAt": null
        }
        """;

        JSONAssert.assertEquals(expected, actual, true);
    }
}