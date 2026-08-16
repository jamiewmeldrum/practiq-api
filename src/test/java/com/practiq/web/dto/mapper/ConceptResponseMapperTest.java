package com.practiq.web.dto.mapper;

import static com.practiq.web.dto.mapper.ConceptResponseMapper.toConceptResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.practiq.service.concept.dto.response.Concept;
import com.practiq.web.dto.response.ConceptResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConceptResponseMapperTest {

    @Test
    void conceptMapsToConceptResponse() {
        long id = 1L;
        String name = "Diffraction";
        String description = "The spreading of waves through a gap or around an obstacle.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        // A non-zero version the response has no field for: the web mapper selects a subset, so the lock
        // token must be dropped here rather than reaching a client.
        Concept concept = new Concept(id, 9, name, description, createdAt);

        ConceptResponse conceptResponse = toConceptResponse(concept);

        assertThat(conceptResponse.id(), equalTo(id));
        assertThat(conceptResponse.name(), equalTo(name));
        assertThat(conceptResponse.description(), equalTo(description));
        assertThat(conceptResponse.createdAt(), equalTo(createdAt));
    }
}
