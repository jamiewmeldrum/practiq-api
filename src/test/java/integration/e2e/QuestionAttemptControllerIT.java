package integration.e2e;

import com.practiq.domain.types.QuestionStatus;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.DBRow;
import utils.data.QuestionTestData;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static utils.data.TestData.SESSION_TOKEN_HEADER;

@IntegrationTest
public class QuestionAttemptControllerIT {

    private static final String QUESTION_ATTEMPTS_PATH = "/api/v1/questions/%s/attempts";

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
    void get404ResponseWhenApprovedAndLinkedQuestionDoesNotExistForIdGETAttempt() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long question1Id = 1L;
        data.question(question1Id).insert();
        data.questionAttempt(question1Id, "session token", "body").insert();

        long question2Id = 2L;
        data.question(question2Id).status(QuestionStatus.APPROVED).insert();
        data.questionAttempt(question2Id, "session token", "body").insert();

        long question3Id = 3L;
        data.question(question3Id).status(QuestionStatus.APPROVED).insert();
        data.link(question3Id, conceptId).insert();
        data.questionAttempt(question3Id, "session token", "body").insert();

        String path = QUESTION_ATTEMPTS_PATH.formatted(9L);
        given()
                .header(new Header(SESSION_TOKEN_HEADER,"test"))
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void get404ResponseWhenApprovedAndUnlinkedQuestionExistsForIdGETAttempt() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long questionId = 2L;
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.questionAttempt(questionId, "session token", "body").insert();

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given()
                .header(new Header(SESSION_TOKEN_HEADER,"test"))
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void get404ResponseWhenPendingAndLinkedQuestionExistsForIdGETAttempt() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long questionId = 2L;
        data.question(questionId).status(QuestionStatus.PENDING).insert();
        data.link(questionId, conceptId).insert();
        data.questionAttempt(questionId, "session token", "body").insert();

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given()
                .header(new Header(SESSION_TOKEN_HEADER,"test"))
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getEmptyResponseWhenQuestionExistsForIdButNoAttemptsForSessionTokenGETAttempt() {
        String sessionToken = "test";

        long conceptId = 10L;
        data.concept(conceptId).insert();

        long question1Id = 1L;
        data.question(question1Id).status(QuestionStatus.APPROVED).insert();
        data.link(question1Id, conceptId).insert();
        data.questionAttempt(question1Id, "session token", "body").insert();

        long question2Id = 2L;
        data.question(question2Id).status(QuestionStatus.APPROVED).insert();
        data.link(question2Id, conceptId).insert();
        data.questionAttempt(question2Id, sessionToken, "body").insert();

        String path = QUESTION_ATTEMPTS_PATH.formatted(question1Id);
        given()
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("$", empty());
    }

    @Test
    void getResponseWhenLinkedAndApprovedQuestionHasAttemptsGETAttempt() {
        String sessionToken = "865726f9-2f79-4789-940f-412db1fb5be1";

        long conceptId = 10L;
        data.concept(conceptId).insert();

        long question1Id = 1L;
        data.question(question1Id).status(QuestionStatus.APPROVED).insert();
        data.link(question1Id, conceptId).insert();
        data.questionAttempt(question1Id, "session token", "body").insert();

        long question2Id = 2L;
        data.question(question2Id).status(QuestionStatus.APPROVED).insert();
        data.link(question2Id, conceptId).insert();

        String attemptBody1 = "attempt 1";
        data.questionAttempt(question2Id, sessionToken, attemptBody1).insert();

        String attemptBody2 = "attempt 2";
        data.questionAttempt(question2Id, sessionToken, attemptBody2).insert();

        String attemptBody3 = "attempt 3";
        data.questionAttempt(question2Id, "not matching token", attemptBody3).insert();

        String path = QUESTION_ATTEMPTS_PATH.formatted(question2Id);
        given()
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("questionId", contains((int) question2Id, (int) question2Id))
                .body("body", containsInAnyOrder(attemptBody1, attemptBody2));
    }

    @Test
    void getResponseEntriesOrderedByCreatedAtDescGETAttempt() {
        String sessionToken = "865726f9-2f79-4789-940f-412db1fb5be1";

        long conceptId = 10L;
        data.concept(conceptId).insert();

        long questionId = 1L;
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.link(questionId, conceptId).insert();

        String attemptBody1 = "attempt 1";
        data.questionAttempt(questionId, sessionToken, attemptBody1).insert();

        String attemptBody2 = "attempt 2";
        data.questionAttempt(questionId, sessionToken, attemptBody2).insert();

        String attemptBody3 = "attempt 3";
        data.questionAttempt(questionId, sessionToken, attemptBody3).insert();

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        Response response = given()
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("body", contains(attemptBody3, attemptBody2, attemptBody1))
                .extract()
                .response();

        long secondId = ((Number) response.path("[1].id")).longValue();

        //Now reorder and re check for created order
        OffsetDateTime fixedNow = OffsetDateTime.now();
        data.updateQuestionAttempt(secondId, "created_at", fixedNow);
        given()
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("body", contains(attemptBody2, attemptBody3, attemptBody1));

        //And for created at clashes
        String attemptBody4 = "attempt 4";
        data.questionAttempt(questionId, sessionToken, attemptBody4).id(secondId + 100).createdAt(fixedNow).insert();
        given()
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("body", contains(attemptBody2, attemptBody4, attemptBody3, attemptBody1));
    }

    @Test
    void get404ResponseWhenApprovedAndLinkedQuestionDoesNotExistForIdPOSTAttempt() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long question1Id = 1L;
        data.question(question1Id).insert();
        data.questionAttempt(question1Id, "session token", "body").insert();

        long question2Id = 2L;
        data.question(question2Id).status(QuestionStatus.APPROVED).insert();
        data.questionAttempt(question2Id, "session token", "body").insert();

        long question3Id = 3L;
        data.question(question3Id).status(QuestionStatus.APPROVED).insert();
        data.link(question3Id, conceptId).insert();
        data.questionAttempt(question3Id, "session token", "body").insert();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", "attempt");

        String path = QUESTION_ATTEMPTS_PATH.formatted(9L);
        given()
                .contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER,"test"))
                .body(requestBody)
                .when()
                .post(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void get404ResponseWhenApprovedAndUnlinkedQuestionExistsForIdPOSTAttempt() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long questionId = 2L;
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.questionAttempt(questionId, "session token", "body").insert();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", "attempt");

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given()
                .contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER,"test"))
                .body(requestBody)
                .when()
                .post(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void get404ResponseWhenPendingAndLinkedQuestionExistsForIdPOSTAttempt() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long questionId = 2L;
        data.question(questionId).status(QuestionStatus.PENDING).insert();
        data.link(questionId, conceptId).insert();
        data.questionAttempt(questionId, "session token", "body").insert();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", "attempt");

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given()
                .contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER,"test"))
                .body(requestBody)
                .when()
                .post(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getCreatedResponseWithResponseBodyWhenPostingValidQuestion() {
        String sessionToken = "865726f9-2f79-4789-940f-412db1fb5be1";

        long conceptId = 10L;
        data.concept(conceptId).insert();

        long questionId = 1L;
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.link(questionId, conceptId).insert();

        List<DBRow> questionAttemptsBeforePost = data.retrieveQuestionAttempts();
        assertThat(questionAttemptsBeforePost.size(), is(0));

        //Test first post
        String attemptBody = "attempt 1";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", attemptBody);
        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given()
                .contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .body(requestBody)
                .when()
                .post(path)
                .then()
                .statusCode(CREATED.getCode())
                .contentType(ContentType.JSON)
                .body("id", instanceOf(Integer.class))
                .body("questionId", equalTo((int) questionId))
                .body("body", equalTo(attemptBody))
                .body("createdAt", matchesPattern(data.getInstantPattern()));

        List<DBRow> questionAttemptsAfterFirstPost = data.retrieveQuestionAttempts();
        assertThat(questionAttemptsAfterFirstPost.size(), is(1));

        DBRow savedAttempt = questionAttemptsAfterFirstPost.getFirst();
        assertThat(savedAttempt.get("id"), instanceOf(Long.class));
        assertThat(savedAttempt.get("question_id"), equalTo(questionId));
        assertThat(savedAttempt.get("body"), is(attemptBody));
        assertThat(savedAttempt.get("created_at").toString(), matchesPattern(data.getInstantPattern()));
    }

    @Test
    void canPostValidQuestionAttemptTwice() {
        String sessionToken = "865726f9-2f79-4789-940f-412db1fb5be1";

        long conceptId = 10L;
        data.concept(conceptId).insert();

        long questionId = 1L;
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.link(questionId, conceptId).insert();

        List<DBRow> questionAttemptsBeforePost = data.retrieveQuestionAttempts();
        assertThat(questionAttemptsBeforePost.size(), is(0));

        //Test first post
        String attemptBody1 = "attempt 1";
        Map<String, Object> requestBody1 = new HashMap<>();
        requestBody1.put("body", attemptBody1);
        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given()
                .contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .body(requestBody1)
                .when()
                .post(path)
                .then()
                .statusCode(CREATED.getCode())
                .contentType(ContentType.JSON)
                .body("body", equalTo(attemptBody1));

        List<DBRow> questionAttemptsAfterFirstPost = data.retrieveQuestionAttempts();
        assertThat(questionAttemptsAfterFirstPost.size(), is(1));

        //Test second post
        String attemptBody2 = "attempt 2";
        Map<String, Object> requestBody2 = new HashMap<>();
        requestBody2.put("body", attemptBody2);
        given()
                .contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .body(requestBody2)
                .when()
                .post(path)
                .then()
                .statusCode(CREATED.getCode())
                .contentType(ContentType.JSON)
                .body("body", equalTo(attemptBody2));

        List<DBRow> questionAttemptsAfterSecondPost = data.retrieveQuestionAttempts();
        assertThat(questionAttemptsAfterSecondPost.size(), is(2));
        assertThat(DBRow.collectColumn(questionAttemptsAfterSecondPost, "body"), containsInAnyOrder(attemptBody1, attemptBody2));
    }
}
