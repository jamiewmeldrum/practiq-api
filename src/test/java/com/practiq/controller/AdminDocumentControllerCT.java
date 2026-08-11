package com.practiq.controller;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static utils.data.TestData.ADMIN_KEY_HEADER;

import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

@ComponentTest
public class AdminDocumentControllerCT {

    private static final String ADMIN_DOCUMENTS_PATH = "/api/v1/admin/documents";

    @Inject
    private EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void postDocumentErrorsIfAdminKeyOmitted() {
        given().when()
                .body(aValidRequestBody())
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Required Header [" + ADMIN_KEY_HEADER + "] not specified"))
                .body("status", equalTo(400));
    }

    @Test
    void postDocumentErrorsIfAdminKeyBlank() {
        // Empty
        given().when()
                .body(aValidRequestBody())
                .header(new Header(ADMIN_KEY_HEADER, ""))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNAUTHORIZED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Unauthorised to access " + ADMIN_DOCUMENTS_PATH))
                .body("status", equalTo(401));

        // Blank
        given().when()
                .body(aValidRequestBody())
                .header(new Header(ADMIN_KEY_HEADER, "  "))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNAUTHORIZED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Unauthorised to access " + ADMIN_DOCUMENTS_PATH))
                .body("status", equalTo(401));
    }

    @Test
    void postDocumentErrorsIfAdminKeyMismatches() {
        given().when()
                .body(aValidRequestBody())
                .header(new Header(ADMIN_KEY_HEADER, "admin"))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNAUTHORIZED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Unauthorised to access " + ADMIN_DOCUMENTS_PATH))
                .body("status", equalTo(401));
    }

    private Map<String, Object> aValidRequestBody() {
        String filename = "practiq-presign.txt";
        String sourceSpec = "AQA 2007";
        byte[] content = "uploaded straight to S3 through a presigned URL".getBytes(StandardCharsets.UTF_8);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filename", filename);
        requestBody.put("contentType", "text/plain");
        requestBody.put("contentLength", content.length);
        requestBody.put("sourceSpec", sourceSpec);
        return requestBody;
    }
}
