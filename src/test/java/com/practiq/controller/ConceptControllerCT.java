package com.practiq.controller;

import com.practiq.domain.Concept;
import com.practiq.repository.ConceptRepository;
import com.practiq.test.ComponentTest;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.practiq.test.TestReflection.setField;
import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class ConceptControllerCT {

    private static final String CONCEPTS_PATH = "/api/v1/concepts";

    @Inject
    private ConceptRepository conceptRepository;

    @Inject
    private EmbeddedServer embeddedServer;

    @MockBean(ConceptRepository.class)
    ConceptRepository conceptRepository() {
        return mock(ConceptRepository.class);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = embeddedServer.getPort();
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

        when(conceptRepository.listOrderByCreatedAtAsc()).thenReturn(List.of(diffraction, acceleration));

        given()
            .when()
            .get(CONCEPTS_PATH)
            .then()
            .statusCode(OK.getCode())
            .contentType(ContentType.JSON)
            .body("name", containsInAnyOrder(diffractionName, accelerationName))
            .body("[0].keySet()", containsInAnyOrder("id", "name", "description", "createdAt"))
            .body("find { it.name == '" + diffractionName + "' }.id", equalTo((int) diffractionId))
            .body("find { it.name == '" + diffractionName + "' }.description", equalTo(diffractionDescription))
            .body("find { it.name == '" + diffractionName + "' }.createdAt", equalTo(diffractionCreatedAt.toString()))
            .body("find { it.name == '" + accelerationName + "' }.description", equalTo(accelerationDescription))
            .body("find { it.name == '" + accelerationName + "' }.createdAt", equalTo(accelerationCreatedAt.toString()))
            .body("find { it.name == '" + accelerationName + "' }.id", equalTo((int) accelerationId));
    }

    @Test
    void getConceptsReturnsEmptyArrayWhenRepositoryEmpty() {
        when(conceptRepository.listOrderByCreatedAtAsc()).thenReturn(List.of());

        given()
            .when()
            .get(CONCEPTS_PATH)
            .then()
            .statusCode(OK.getCode())
            .contentType(ContentType.JSON)
            .body("$", empty());
    }
}
