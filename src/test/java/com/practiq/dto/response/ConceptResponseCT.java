package com.practiq.dto.response;

import utils.ComponentTest;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.time.Instant;

// Uses the context-configured ObjectMapper (not ObjectMapper.getDefault()), so these tests exercise
// the real application.properties serialization config — including inclusion=always. That makes the
// null test a genuine guard: drop that config line and it fails here, without needing Docker.
@ComponentTest
class ConceptResponseCT {

    @Inject
    private ObjectMapper objectMapper;

    @Test
    void conceptResponseSerializesFull() throws IOException {
        ConceptResponse concept = new ConceptResponse(
                42,
                "Diffraction",
                "The bending of waves around obstacles or through openings.",
                Instant.parse("2026-06-29T10:15:30Z")
        );
        String actual = objectMapper.writeValueAsString(concept);

        String expected = """
        {
          "id": 42,
          "name": "Diffraction",
          "description": "The bending of waves around obstacles or through openings.",
          "createdAt": "2026-06-29T10:15:30Z"
        }
        """;

        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    void conceptResponseIncludesNullFields() throws IOException {
        ConceptResponse concept = new ConceptResponse(-1, null, null, null);
        String actual = objectMapper.writeValueAsString(concept);

        // inclusion=always: every field is present, nulls stay null rather than being dropped.
        String expected = """
        {
          "id": -1,
          "name": null,
          "description": null,
          "createdAt": null
        }
        """;

        JSONAssert.assertEquals(expected, actual, true);
    }
}
