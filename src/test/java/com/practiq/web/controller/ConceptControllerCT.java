package com.practiq.web.controller;

import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static utils.TestReflection.setField;

import com.practiq.persistence.ConceptEntity;
import com.practiq.persistence.repository.ConceptRepository;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

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
        ConceptEntity diffraction = new ConceptEntity(diffractionName, diffractionDescription);
        setField(diffraction, "id", diffractionId);
        setField(diffraction, "createdAt", diffractionCreatedAt);

        long accelerationId = 2L;
        String accelerationName = "Acceleration";
        String accelerationDescription = "How the velocity of an object changes over time.";
        Instant accelerationCreatedAt = Instant.parse("2026-01-02T00:00:00Z");
        ConceptEntity acceleration = new ConceptEntity(accelerationName, accelerationDescription);
        setField(acceleration, "id", accelerationId);
        setField(acceleration, "createdAt", accelerationCreatedAt);

        when(conceptRepository.listOrderByCreatedAtAsc()).thenReturn(List.of(diffraction, acceleration));

        given().when()
                .get(CONCEPTS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("name", containsInAnyOrder(diffractionName, accelerationName))
                .body("[0].keySet()", containsInAnyOrder("id", "name", "description", "createdAt"))
                .body("find { it.name == '" + diffractionName + "' }.id", equalTo((int) diffractionId))
                .body("find { it.name == '" + diffractionName + "' }.description", equalTo(diffractionDescription))
                .body(
                        "find { it.name == '" + diffractionName + "' }.createdAt",
                        equalTo(diffractionCreatedAt.toString()))
                .body("find { it.name == '" + accelerationName + "' }.description", equalTo(accelerationDescription))
                .body(
                        "find { it.name == '" + accelerationName + "' }.createdAt",
                        equalTo(accelerationCreatedAt.toString()))
                .body("find { it.name == '" + accelerationName + "' }.id", equalTo((int) accelerationId));

        verify(conceptRepository).listOrderByCreatedAtAsc();
    }

    @Test
    void getConceptsReturnsEmptyArrayWhenRepositoryEmpty() {
        when(conceptRepository.listOrderByCreatedAtAsc()).thenReturn(List.of());

        given().when()
                .get(CONCEPTS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("$", empty());

        verify(conceptRepository).listOrderByCreatedAtAsc();
    }

    @Test
    void getConceptByIdSerializesResult() {
        long id = 17L;
        String name = "Diffraction";
        String description = "The spreading of waves through a gap or around an obstacle.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        ConceptEntity concept = new ConceptEntity(name, description);
        setField(concept, "id", id);
        setField(concept, "createdAt", createdAt);

        when(conceptRepository.findById(id)).thenReturn(Optional.of(concept));

        given().when()
                .get(CONCEPTS_PATH + "/" + id)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("name", equalTo(name))
                .body("keySet()", containsInAnyOrder("id", "name", "description", "createdAt"))
                .body("id", equalTo((int) id))
                .body("description", equalTo(description))
                .body("createdAt", equalTo(createdAt.toString()));

        verify(conceptRepository).findById(id);
    }

    @Test
    void getConceptByIdReturnsNotFoundEnvelope() {
        long id = 17L;

        when(conceptRepository.findById(id)).thenReturn(Optional.empty());

        String path = CONCEPTS_PATH + "/" + id;
        given().when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));

        verify(conceptRepository).findById(id);
    }

    @Test
    void getConceptByIdReturnsEnvelopeForIdNotBeingANumber() {
        String path = CONCEPTS_PATH + "/BAD";
        given().when()
                .get(path)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("id: invalid value"))
                .body("status", equalTo(400));
    }

    @Test
    void getConceptByIdReturnsEnvelopeForIdBelowOneAndNeverAsksTheRepository() {
        // An id below one can never name a row, so it is refused as a bad value rather than answered 404 as
        // an absent one. Both arms live in one test so the boundary cannot drift apart into separate cases.
        for (String id : List.of("0", "-4002")) {
            given().when()
                    .get(CONCEPTS_PATH + "/" + id)
                    .then()
                    .statusCode(UNPROCESSABLE_ENTITY.getCode())
                    .contentType(ContentType.JSON)
                    .body("keySet()", containsInAnyOrder("error", "status"))
                    .body("error", equalTo("id: must be greater than or equal to 1"))
                    .body("status", equalTo(422));
        }

        verify(conceptRepository, never()).findById(anyLong());
    }
}
