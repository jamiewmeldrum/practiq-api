package com.practiq.controller;

import com.practiq.domain.Concept;
import com.practiq.repository.ConceptRepository;
import com.practiq.test.ComponentTest;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class ConceptControllerCT {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ConceptRepository conceptRepository;

    @MockBean(ConceptRepository.class)
    ConceptRepository conceptRepository() {
        return mock(ConceptRepository.class);
    }

    @Test
    void getConceptsSerializesRepositoryResults() {

        when(conceptRepository.findAll()).thenReturn(List.of(
                new Concept("Diffraction", "The spreading of waves through a gap or around an obstacle."),
                new Concept("Acceleration", "How the velocity of an object changes over time.")
        ));

        // Assert against the JSON structure, not by deserializing back into Concept:
        // the entity is @Getter-only (no setters), so a round-trip into Concept can't
        // repopulate its fields. The wire format is what the endpoint actually returns.
        List<Map<String, Object>> body = client.toBlocking().retrieve(
                HttpRequest.GET("/v1/concepts"),
                Argument.listOf(Argument.mapOf(String.class, Object.class)));

        //TODO - HTTP CHeck etc?

        assertEquals(2, body.size());
        assertEquals("Diffraction", body.get(0).get("name"));
        assertEquals("Acceleration", body.get(1).get("name"));
    }

    @Test
    void getConceptsReturnsEmptyArrayWhenRepositoryEmpty() {
        // TODO: stub findAll() -> empty, GET /v1/concepts, assert 200 + []
        fail("not yet implemented");
    }
}
