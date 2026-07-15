package performance;

import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
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

// Pins the JDBC statement count for serving a single question by id.
@PerformanceTest
public class QuestionByIdPT {

    private static final String QUESTIONS_PATH = "/api/v1/questions";
    private static final long CONCEPT_ID = 100L;
    private static final long QUESTION_ID = 1L;

    // findOne(spec) + the concept-link stitch.
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
        data.concept(CONCEPT_ID).insert();
        RestAssured.port = embeddedServer.getPort();
        statements = new StatementCounter(entityManagerFactory);
    }

    @Test
    void servingAQuestionByIdFiresAConstantNumberOfStatements() {
        data.question(QUESTION_ID)
                .status(QuestionStatus.APPROVED)
                .body("State Newton's first law.")
                .source(QuestionSource.SEED)
                .insert();
        data.link(QUESTION_ID, CONCEPT_ID).insert();

        long count = statements.countDuring(() ->
                given().when().get(QUESTIONS_PATH + "/" + QUESTION_ID).then().statusCode(OK.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }
}
