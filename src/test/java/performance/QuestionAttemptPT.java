package performance;

import com.practiq.domain.types.QuestionStatus;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.PerformanceTest;
import utils.StatementCounter;
import utils.data.QuestionTestData;

import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.data.TestData.SESSION_TOKEN_HEADER;

// Pins the JDBC statement count for serving a session's question attempts — a row-scaling path. Beyond the
// fixed happy-path count it asserts the count does NOT grow with the number of attempts, which is the
// property an eager association (N+1) would break.
@PerformanceTest
public class QuestionAttemptPT {

    private static final String QUESTION_ATTEMPTS_PATH = "/api/v1/questions/%s/attempts";

    // The exists(spec) visibility gate + the findAll(spec, sort) attempt fetch.
    private static final long EXPECTED_STATEMENTS = 2L;

    @Inject
    private QuestionTestData data;

    @Inject
    private EmbeddedServer embeddedServer;

    @Inject
    private EntityManagerFactory entityManagerFactory;

    private StatementCounter statements;

    @BeforeEach
    void setUp() {
        data.clear();
        RestAssured.port = embeddedServer.getPort();
        statements = new StatementCounter(entityManagerFactory);
    }

    @Test
    void servingQuestionAttemptsFiresAConstantNumberOfStatements() {
        String sessionToken = "session-token";
        long conceptId = 10L;
        long questionId = 2L;

        data.concept(conceptId).insert();
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.link(questionId, conceptId).insert();
        data.questionAttempt(questionId, sessionToken, "attempt 1").insert();
        data.questionAttempt(questionId, sessionToken, "attempt 2").insert();

        long count = statements.countDuring(() ->
                given()
                        .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                        .when()
                        .get(QUESTION_ATTEMPTS_PATH.formatted(questionId))
                        .then()
                        .statusCode(OK.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }

    @Test
    void servingMoreAttemptsDoesNotFireMoreStatements() {
        String sessionToken = "session-token";
        long conceptId = 10L;
        long questionId = 2L;

        data.concept(conceptId).insert();
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.link(questionId, conceptId).insert();
        data.questionAttempt(questionId, sessionToken, "attempt 1").insert();
        data.questionAttempt(questionId, sessionToken, "attempt 2").insert();

        long fewer = statements.countDuring(() ->
                given()
                        .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                        .when()
                        .get(QUESTION_ATTEMPTS_PATH.formatted(questionId))
                        .then()
                        .statusCode(OK.getCode()));

        data.questionAttempt(questionId, sessionToken, "attempt 3").insert();
        data.questionAttempt(questionId, sessionToken, "attempt 4").insert();
        data.questionAttempt(questionId, sessionToken, "attempt 5").insert();
        data.questionAttempt(questionId, sessionToken, "attempt 6").insert();

        long more = statements.countDuring(() ->
                given()
                        .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                        .when()
                        .get(QUESTION_ATTEMPTS_PATH.formatted(questionId))
                        .then()
                        .statusCode(OK.getCode()));

        // The count is a property of the query plan, not the row count: more attempts, same statements.
        assertThat(more, equalTo(fewer));
    }
}
