package com.practiq.dto.response;

import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import utils.ComponentTest;

import java.io.IOException;
import java.util.List;

// Uses the context-configured ObjectMapper (not ObjectMapper.getDefault()), so these tests exercise
// the real application.properties serialization config — including inclusion=always. That makes the
// null test a genuine guard: drop that config line and it fails here, without needing Docker.
@ComponentTest
class PageResponseCT {

    @Inject
    private ObjectMapper objectMapper;

    @Test
    void pageResponseSerializesFull() throws IOException {
        PageResponse<String> pageResponse = new PageResponse<>(
                List.of("Entry 1", "Entry 2", "Entry 3"),
                2,
                20,
                200
        );
        String actual = objectMapper.writeValueAsString(pageResponse);

        String expected = """
        {
          "content": ["Entry 1", "Entry 2", "Entry 3"],
          "page": 2,
          "size": 20,
          "totalCount": 200
        }
        """;

        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    void pageResponseIncludesEmptyContent() throws IOException {
        PageResponse<String> pageResponse = new PageResponse<>(
                List.of(),
                0,
                0,
                0
        );
        String actual = objectMapper.writeValueAsString(pageResponse);

        String expected = """
        {
          "content": [],
          "page": 0,
          "size": 0,
          "totalCount": 0
        }
        """;

        JSONAssert.assertEquals(expected, actual, true);
    }
}
