package com.practiq.service.concept.mapper;

import static com.practiq.service.concept.mapper.ConceptMapper.toConcept;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.TestReflection.setField;

import com.practiq.persistence.ConceptEntity;
import com.practiq.service.concept.dto.response.Concept;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConceptMapperTest {

    @Test
    void conceptEntityMapsToConcept() {
        long id = 1L;
        int version = 4;
        String name = "Diffraction";
        String description = "The spreading of waves through a gap or around an obstacle.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        ConceptEntity conceptEntity = new ConceptEntity(name, description);
        setField(conceptEntity, "id", id);
        setField(conceptEntity, "version", version);
        setField(conceptEntity, "createdAt", createdAt);

        Concept concept = toConcept(conceptEntity);

        assertThat(concept.id(), equalTo(id));
        // The service model carries the lock token even though no web response exposes it: the caller of a
        // service method is not necessarily the web layer.
        assertThat(concept.version(), equalTo(version));
        assertThat(concept.name(), equalTo(name));
        assertThat(concept.description(), equalTo(description));
        assertThat(concept.createdAt(), equalTo(createdAt));
    }
}
