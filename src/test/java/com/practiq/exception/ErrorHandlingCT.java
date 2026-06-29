package com.practiq.exception;

import com.practiq.test.ComponentTest;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@ComponentTest
class ErrorHandlingCT {

    private static final String INVALID_RESOURCES_PATH = "/api/v1/invalid_resource";

    @Inject
    private EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void getInvalidResourceResultsInNotFound() {
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
}
