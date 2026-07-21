package com.practiq.exception;

import io.micronaut.context.annotation.Property;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

@ComponentTest
@Property(name = "spec.name", value = "ErrorHandlingCT")
class ErrorHandlingCT {

    private static final String INVALID_RESOURCES_PATH = "/api/v1/invalid_resource";

    @Inject
    private EmbeddedServer embeddedServer;

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
    void missingRequiredHeaderReturnsBadRequestEnvelope() {
        given()
                .when()
                .get("/test/errors/required-header")
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Required Header [X-Required-Header] not specified"))
                .body("status", equalTo(400));
    }

    // A sibling of the missing-header case: both are UnsatisfiedRouteException subtypes, so one handler
    // serves both. Two flavours prove the handler's breadth end to end without covering every subtype.
    @Test
    void missingRequiredQueryValueReturnsBadRequestEnvelope() {
        given()
                .when()
                .get("/test/errors/required-query")
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Required QueryValue [requiredParam] not specified"))
                .body("status", equalTo(400));
    }

    @Test
    void unexpectedRuntimeErrorReturnsInternalServerErrorEnvelope() {
        given()
                .when()
                .get("/test/errors/runtime-error")
                .then()
                .statusCode(INTERNAL_SERVER_ERROR.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("An unspecified error occurred."))
                .body("status", equalTo(500));
    }

    // Pins what a caller sees if a stale @Version write escapes to the web layer: the generic 500
    // envelope, because no OptimisticLockException-specific handler exists yet. The right answer is a
    // 409 Conflict handler; this test is the tripwire forcing that decision when it lands.
    @Test
    void optimisticLockFailureCurrentlyReturnsTheGenericErrorEnvelope() {
        given()
                .when()
                .get("/test/errors/optimistic-lock")
                .then()
                .statusCode(INTERNAL_SERVER_ERROR.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("An unspecified error occurred."))
                .body("status", equalTo(500));
    }
}
