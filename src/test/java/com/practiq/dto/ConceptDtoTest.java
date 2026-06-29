package com.practiq.dto;

import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.time.Instant;

class ConceptDtoTest {

    private final ObjectMapper objectMapper = ObjectMapper.getDefault();

    @Test
    void conceptDtoSerializesFull() throws IOException {
        ConceptDto concept = new ConceptDto(
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
    void conceptDtoOmitsNullFields() throws IOException {
        ConceptDto concept = new ConceptDto(-1, null, null, null);
        String actual = objectMapper.writeValueAsString(concept);

        String expected = """
        {
          "id": -1
        }
        """;

        JSONAssert.assertEquals(expected, actual, true);
    }
}
