package performance;

import com.practiq.domain.types.QuestionStatus;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.PerformanceTest;
import utils.StatementCounter;
import utils.data.QuestionTestData;

import java.util.HashMap;
import java.util.Map;

import static io.micronaut.http.HttpStatus.CREATED;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.data.TestData.SESSION_TOKEN_HEADER;

@PerformanceTest
public class QuestionAttemptPostPT {
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
    void postingQuestionAttemptFiresAConstantNumberOfStatements() {
        long conceptId = 10L;
        long questionId = 2L;

        data.concept(conceptId).insert();
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.link(questionId, conceptId).insert();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", "body 1");

        String sessionToken = "865726f9-2f79-4789-940f-412db1fb5be1";

        long count = statements.countDuring(() ->
                given()
                        .contentType(ContentType.JSON)
                        .header(new Header(SESSION_TOKEN_HEADER, "865726f9-2f79-4789-940f-412db1fb5be1"))
                        .body(requestBody)
                        .when()
                        .post(QUESTION_ATTEMPTS_PATH.formatted(questionId))
                        .then()
                        .statusCode(CREATED.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }
}
