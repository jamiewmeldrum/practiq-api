package integration.e2e;

import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@IntegrationTest
public class MarkSchemeControllerIT {

    private static final String MARK_SCHEME_PATH = "/api/v1/questions/%s/mark-scheme";

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
    void getMarkSchemeForQuestionIdReturnsNotFoundIfNoQuestionExistsForId() {
        String path = MARK_SCHEME_PATH.formatted(1L);
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

    @Test
    void getMarkSchemeForQuestionIdReturnsNotFoundIfNoApprovedQuestionExistsForId() {
        long questionId = 1L;
        data.question(questionId)
                .status(QuestionStatus.REJECTED)
                .body("State Newton's first law.")
                .source(QuestionSource.SEED)
                .insert();

        long conceptId = 10L;
        data.concept(conceptId).insert();
        data.link(questionId, conceptId).insert();

        data.markScheme(questionId, "body").insert();

        String path = MARK_SCHEME_PATH.formatted(questionId);
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

    @Test
    void getMarkSchemeForQuestionIdReturnsNotFoundIfNoLinkExistsForId() {
        long questionId = 1L;
        data.question(questionId)
                .status(QuestionStatus.APPROVED)
                .body("State Newton's first law.")
                .source(QuestionSource.SEED)
                .insert();

        data.markScheme(questionId, "body").insert();

        String path = MARK_SCHEME_PATH.formatted(questionId);
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

    @Test
    void getMarkSchemeForQuestionIdReturnsNotFoundIfQuestionValidButMarkSchemeDoesNotExist() {
        long questionId = 1L;
        data.question(questionId)
                .status(QuestionStatus.APPROVED)
                .body("State Newton's first law.")
                .source(QuestionSource.SEED)
                .insert();

        long conceptId = 10L;
        data.concept(conceptId).insert();
        data.link(questionId, conceptId).insert();

        String path = MARK_SCHEME_PATH.formatted(questionId);
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

    @Test
    void getMarkSchemeForQuestionIdReturnsMarkScheme() {
        long questionId = 1L;
        data.question(questionId)
                .status(QuestionStatus.APPROVED)
                .body("State Newton's first law.")
                .source(QuestionSource.SEED)
                .insert();

        long conceptId = 10L;
        data.concept(conceptId).insert();
        data.link(questionId, conceptId).insert();

        long markSchemeId = 100L;
        String body = "Mark scheme body";
        data.markScheme(questionId, body).id(markSchemeId).insert();

        String path = MARK_SCHEME_PATH.formatted(questionId);
        given()
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("id", "questionId", "body", "createdAt"))
                .body("id", equalTo((int) markSchemeId))
                .body("questionId", equalTo((int) questionId))
                .body("body", equalTo(body))
                .body("createdAt", matchesPattern(data.getInstantPattern()));
    }
}
