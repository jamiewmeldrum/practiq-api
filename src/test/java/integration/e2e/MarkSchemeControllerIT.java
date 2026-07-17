package integration.e2e;

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

// Every test here carries a second, fully-servable question with its own mark scheme. Without it a handler
// that returned *a* mark scheme rather than *the* one asked for would pass every case below.
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
        long conceptId = 10L;
        data.concept(conceptId).insert();
        servableQuestionWithMarkScheme(7L, conceptId, "Mark scheme for seven.");
        servableQuestionWithMarkScheme(8L, conceptId, "Mark scheme for eight.");

        // 9 is neither of the two rows present, so a handler returning any row is caught.
        String path = MARK_SCHEME_PATH.formatted(9L);
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getMarkSchemeForQuestionIdReturnsNotFoundIfNoApprovedQuestionExistsForId() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long rejectedId = 7L;
        data.question(rejectedId).status(QuestionStatus.REJECTED).insert();
        data.link(rejectedId, conceptId).insert();
        data.markScheme(rejectedId, "Mark scheme for the rejected question.").insert();

        servableQuestionWithMarkScheme(8L, conceptId, "Mark scheme for eight.");

        String path = MARK_SCHEME_PATH.formatted(rejectedId);
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getMarkSchemeForQuestionIdReturnsNotFoundIfNoLinkExistsForId() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        // Approved and holds a mark scheme, but is never linked to the concept — the link is the only
        // thing disqualifying it.
        long unlinkedId = 7L;
        data.question(unlinkedId).status(QuestionStatus.APPROVED).insert();
        data.markScheme(unlinkedId, "Mark scheme for the unlinked question.").insert();

        servableQuestionWithMarkScheme(8L, conceptId, "Mark scheme for eight.");

        String path = MARK_SCHEME_PATH.formatted(unlinkedId);
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getMarkSchemeForQuestionIdReturnsNotFoundIfQuestionValidButMarkSchemeDoesNotExist() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        // Fully servable, simply has no mark scheme yet. The second question does have one, so returning
        // the wrong question's scheme fails here rather than passing on an empty table.
        long noSchemeId = 7L;
        data.question(noSchemeId).status(QuestionStatus.APPROVED).insert();
        data.link(noSchemeId, conceptId).insert();

        servableQuestionWithMarkScheme(8L, conceptId, "Mark scheme for eight.");

        String path = MARK_SCHEME_PATH.formatted(noSchemeId);
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getMarkSchemeForQuestionIdReturnsMarkScheme() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long questionId = 7L;
        long markSchemeId = 70L;
        String body = "Mark scheme for seven.";
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.link(questionId, conceptId).insert();
        data.markScheme(questionId, body).id(markSchemeId).insert();

        long otherQuestionId = 8L;
        data.question(otherQuestionId).status(QuestionStatus.APPROVED).insert();
        data.link(otherQuestionId, conceptId).insert();
        data.markScheme(otherQuestionId, "Mark scheme for eight.").id(80L).insert();

        given()
                .when()
                .get(MARK_SCHEME_PATH.formatted(questionId))
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) markSchemeId))
                .body("questionId", equalTo((int) questionId))
                .body("body", equalTo(body))
                .body("createdAt", matchesPattern(data.getInstantPattern()));
    }

    private void servableQuestionWithMarkScheme(long questionId, long conceptId, String markSchemeBody) {
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.link(questionId, conceptId).insert();
        data.markScheme(questionId, markSchemeBody).insert();
    }
}
