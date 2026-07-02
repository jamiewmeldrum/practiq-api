package com.practiq.controller;

import com.practiq.test.TestDatabase;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@MicronautTest(transactional = false)
class ConceptControllerIT {

    private static final String CONCEPT_TABLE = "concept";

    private static final String CONCEPTS_PATH = "/api/v1/concepts";

    private static final String CREATED_AT_PATTERN = "\\d{4}-\\d{2}-\\d{2}T.*Z";

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
                .body("createdAt", everyItem(matchesPattern(CREATED_AT_PATTERN)))
                .body("find { it.name == '" + diffractionName + "' }.description", equalTo(diffractionDescription))
                .body("find { it.name == '" + accelerationName + "' }.description", equalTo(accelerationDescription));
    }

    @Test
    void getConceptsReturnsInCreatedAtOrder() {
        String diffractionName = "Diffraction";
        String accelerationName = "Acceleration";
        String forcesName = "Forces";

        testDatabase.insert(
                CONCEPT_TABLE,
                Map.of(
                        "name", diffractionName,
                        "description", "The spreading of waves through a gap or around an obstacle."
                )
        );

        testDatabase.insert(
                CONCEPT_TABLE,
                Map.of(
                        "name", accelerationName,
                        "description", "How the velocity of an object changes over time."
                )
        );

        testDatabase.insert(
                CONCEPT_TABLE,
                Map.of(
                        "name", forcesName,
                        "description", "Newtons's laws etc"
                )
        );

        // Each insert commits in its own transaction, so the three rows get distinct created_at defaults in insert order.
        Response response =
                given()
                        .when()
                        .get(CONCEPTS_PATH)
                        .then()
                        .statusCode(OK.getCode())
                        .contentType(ContentType.JSON)
                        .body("[0].name", equalTo(diffractionName))
                        .body("[1].name", equalTo(accelerationName))
                        .body("[2].name", equalTo(forcesName))
                        .extract()
                        .response();

        long firstId = ((Number) response.path("[0].id")).longValue();

        //Now update the created_at to prove ordering isn't coincidence
        testDatabase.update(
                CONCEPT_TABLE,
                firstId,
                "created_at",
                OffsetDateTime.now()
        );

        given()
                .when()
                .get(CONCEPTS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("[0].name", equalTo(accelerationName))
                .body("[1].name", equalTo(forcesName))
                .body("[2].name", equalTo(diffractionName));
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

    @Test
    void getConceptsReturnsSeededConceptById() {
        long id = 45;
        String name = "Diffraction";
        String description = "The spreading of waves through a gap or around an obstacle.";

        testDatabase.insert(
                CONCEPT_TABLE,
                Map.of(
                        "id", id,
                        "name", name,
                        "description", description
                )
        );

        given()
                .when()
                .get(CONCEPTS_PATH + "/" + id)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("id", "name", "description", "createdAt"))
                .body("id", equalTo((int) id))
                .body("name", equalTo(name))
                .body("description", equalTo(description))
                .body("createdAt", matchesPattern(CREATED_AT_PATTERN));
    }

    @Test
    void getConceptsReturnsSeededConceptByIdNotFound() {
        long id = 46;

        String path = CONCEPTS_PATH + "/" + id;
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }
}
