package com.practiq.controller;

import com.practiq.domain.Concept;
import com.practiq.repository.ConceptRepository;
import com.practiq.test.ComponentTest;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.practiq.test.TestReflection.setField;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class ConceptControllerCT {

    private static final String CONCEPTS_PATH = "/v1/concepts";

    @Inject
    @Client("/")
    private HttpClient client;

    @Inject
    private ConceptRepository conceptRepository;

    @MockBean(ConceptRepository.class)
    ConceptRepository conceptRepository() {
        return mock(ConceptRepository.class);
    }

    @Test
    void getConceptsSerializesRepositoryResults() {
        long diffractionId = 1L;
        String diffractionName = "Diffraction";
        String diffractionDescription = "The spreading of waves through a gap or around an obstacle.";
        Instant diffractionCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        Concept diffraction = new Concept(diffractionName, diffractionDescription);
        setField(diffraction, "id", diffractionId);
        setField(diffraction, "createdAt", diffractionCreatedAt);

        long accelerationId = 2L;
        String accelerationName = "Acceleration";
        String accelerationDescription = "How the velocity of an object changes over time.";
        Instant accelerationCreatedAt = Instant.parse("2026-01-02T00:00:00Z");
        Concept acceleration = new Concept(accelerationName, accelerationDescription);
        setField(acceleration, "id", accelerationId);
        setField(acceleration, "createdAt", accelerationCreatedAt);

        when(conceptRepository.findAll()).thenReturn(List.of(diffraction, acceleration));

        HttpResponse<List<Map<String, Object>>> response = client.toBlocking().exchange(
                HttpRequest.GET(CONCEPTS_PATH),
                Argument.listOf(Argument.mapOf(String.class, Object.class)));

        assertEquals(HttpStatus.OK, response.getStatus());
        List<Map<String, Object>> body = response.body();
        assertEquals(2, body.size());

        assertConcept(body.get(0), diffractionId, diffractionName, diffractionDescription, diffractionCreatedAt);
        assertConcept(body.get(1), accelerationId, accelerationName, accelerationDescription, accelerationCreatedAt);
    }

    @Test
    void getConceptsReturnsEmptyArrayWhenRepositoryEmpty() {
        when(conceptRepository.findAll()).thenReturn(List.of());

        HttpResponse<List<Map<String, Object>>> response = client.toBlocking().exchange(
                HttpRequest.GET(CONCEPTS_PATH),
                Argument.listOf(Argument.mapOf(String.class, Object.class)));

        assertEquals(HttpStatus.OK, response.getStatus());
        List<Map<String, Object>> body = response.body();

        assertEquals(0, body.size());
    }

    // Asserts a serialized concept exposes exactly these fields, by these names, with these
    // values. id arrives as a JSON number; createdAt as its ISO-8601 string.
    private static void assertConcept(Map<String, Object> actual, long id, String name,
                                      String description, Instant createdAt) {
        assertEquals(Set.of("id", "name", "description", "createdAt"), actual.keySet());
        assertEquals(id, ((Number) actual.get("id")).longValue());
        assertEquals(name, actual.get("name"));
        assertEquals(description, actual.get("description"));
        assertEquals(createdAt.toString(), actual.get("createdAt"));
    }
}
