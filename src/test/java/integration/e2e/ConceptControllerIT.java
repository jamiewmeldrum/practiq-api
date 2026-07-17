package integration.e2e;

import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import java.time.OffsetDateTime;

import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@IntegrationTest
class ConceptControllerIT {

    private static final String CONCEPTS_PATH = "/api/v1/concepts";

    @Inject
    private QuestionTestData data;

    @Inject
    private EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        data.clear();
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void getConceptsReturnsSeededConceptsInCreatedAtOrder() {
        String diffractionName = "Diffraction";
        String accelerationName = "Acceleration";
        String forcesName = "Forces";

        data.concept().name(diffractionName).description("desc 1").insert();
        data.concept().name(accelerationName).description("desc 2").insert();
        data.concept().name(forcesName).description("desc 3").insert();

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
        data.updateConcept(firstId, "created_at", OffsetDateTime.now());

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

    // Both by-id tests carry a second concept. Without it a handler returning *a* concept rather than *the*
    // one asked for would pass.
    @Test
    void getConceptsReturnsSeededConceptById() {
        long id = 45;
        String name = "Diffraction";
        String description = "The spreading of waves through a gap or around an obstacle.";

        data.concept().id(id).name(name).description(description).insert();
        data.concept().id(46).name("Acceleration").description("The rate of change of velocity.").insert();

        given()
                .when()
                .get(CONCEPTS_PATH + "/" + id)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) id))
                .body("name", equalTo(name))
                .body("description", equalTo(description))
                .body("createdAt", matchesPattern(data.getInstantPattern()));
    }

    @Test
    void getConceptsReturnsNotFoundIfNoConceptForId() {
        data.concept().id(45).name("Diffraction").description("desc 1").insert();
        data.concept().id(46).name("Acceleration").description("desc 2").insert();

        // 47 is neither of the two rows present.
        String path = CONCEPTS_PATH + "/" + 47L;
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }
}
