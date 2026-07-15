package com.practiq.exception;

import com.practiq.service.ConceptService;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class ErrorHandlingCT {

    private static final String CONCEPTS_PATH = "/api/v1/concepts";
    private static final String INVALID_RESOURCES_PATH = "/api/v1/invalid_resource";

    @Inject
    private EmbeddedServer embeddedServer;

    @Inject
    private ConceptService conceptService;

    @MockBean(ConceptService.class)
    ConceptService conceptService() {
        return mock(ConceptService.class);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void unmappedRouteReturnsNotFoundEnvelope() {
        given()
                .when()
                .get(INVALID_RESOURCES_PATH)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Could not find resource for path: " + INVALID_RESOURCES_PATH))
                .body("status", equalTo(404));
    }

    @Test
    void unexpectedRuntimeErrorReturnsInternalServerErrorEnvelope() {
        when(conceptService.get()).thenThrow(new RuntimeException("Test Error"));
        given()
                .when()
                .get(CONCEPTS_PATH)
                .then()
                .statusCode(INTERNAL_SERVER_ERROR.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("An unspecified error occurred."))
                .body("status", equalTo(500));
    }

    // Pins what a caller sees if a stale @Version write escapes to the web layer: the generic 500
    // envelope, because no OptimisticLockException-specific handler exists. No current endpoint can
    // trigger this (the API is read-only so far) — when the first write endpoint lands, the right
    // answer is a 409 Conflict handler, and this test is the tripwire forcing that decision.
    @Test
    void optimisticLockFailureCurrentlyReturnsTheGenericErrorEnvelope() {
        when(conceptService.get()).thenThrow(new OptimisticLockException("stale version"));
        given()
                .when()
                .get(CONCEPTS_PATH)
                .then()
                .statusCode(INTERNAL_SERVER_ERROR.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("An unspecified error occurred."))
                .body("status", equalTo(500));
    }
}
