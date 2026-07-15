package com.practiq.dto.mapper;

import com.practiq.domain.Concept;
import com.practiq.dto.response.ConceptResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.practiq.dto.mapper.ConceptResponseMapper.toConceptResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.TestReflection.setField;

class ConceptResponseMapperTest {

    @Test
    void conceptMapsToConceptResponse() {
        long conceptId = 1L;
        String conceptName = "Diffraction";
        String conceptDescription = "The spreading of waves through a gap or around an obstacle.";
        Instant conceptCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        Concept concept = new Concept(conceptName, conceptDescription);
        setField(concept, "id", conceptId);
        setField(concept, "createdAt", conceptCreatedAt);

        ConceptResponse conceptResponse = toConceptResponse(concept);

        assertThat(conceptResponse.getId(), equalTo(conceptId));
        assertThat(conceptResponse.getName(), equalTo(conceptName));
        assertThat(conceptResponse.getDescription(), equalTo(conceptDescription));
        assertThat(conceptResponse.getCreatedAt(), equalTo(conceptCreatedAt));
    }
}