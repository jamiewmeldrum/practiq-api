package com.practiq.controller;

import com.practiq.test.TestDatabase;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@MicronautTest(transactional = false)
class ConceptControllerIT {

    private static final String CONCEPT_TABLE = "concept";

    private static final String CONCEPTS_PATH = "api/v1/concepts";

    @Inject
    private TestDatabase testDatabase;

    @Inject
    private EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        testDatabase.clear(CONCEPT_TABLE);
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void getConceptsReturnsSeededConcepts() {
        String diffractionName = "Diffraction";
        String diffractionDescription = "The spreading of waves through a gap or around an obstacle.";
        String accelerationName = "Acceleration";
        String accelerationDescription = "How the velocity of an object changes over time.";

        testDatabase.insert(
                CONCEPT_TABLE,
                Map.of(
                        "name", diffractionName,
                        "description", diffractionDescription
                )
        );

        testDatabase.insert(
                CONCEPT_TABLE,
                Map.of(
                        "name", accelerationName,
                        "description", accelerationDescription
                )
        );

        given()
                .when()
                .get(CONCEPTS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("[0].keySet()", containsInAnyOrder("id", "name", "description", "createdAt"))
                .body("name", containsInAnyOrder(diffractionName, accelerationName))
                .body("id", everyItem(greaterThan(0)))
                .body("createdAt", everyItem(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
                .body("find { it.name == '" + diffractionName + "' }.description", equalTo(diffractionDescription))
                .body("find { it.name == '" + accelerationName + "' }.description", equalTo(accelerationDescription));
    }

    @Test
    void getConceptsReturnsEmptyArrayWhenNoneExist() {
        given()
                .when()
                .get(CONCEPTS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("$", empty());
    }
}
