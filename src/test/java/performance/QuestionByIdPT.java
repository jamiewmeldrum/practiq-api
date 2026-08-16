package performance;

import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.practiq.foundation.types.QuestionStatus;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.PerformanceTest;
import utils.StatementCounter;
import utils.data.TestData;

@PerformanceTest
public class QuestionByIdPT {

    private static final String QUESTIONS_PATH = "/api/v1/questions";

    private static final long EXPECTED_STATEMENTS = 2L;

    @Inject
    private TestData data;

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
    void servingAQuestionByIdFiresAConstantNumberOfStatements() {
        long conceptId = 100L;
        data.concept(conceptId).insert();

        // Two servable rows: the statement count must be a property of the query plan, not of the target
        // happening to be the only row in the table.
        long questionId = 7L;
        data.question(questionId)
                .status(QuestionStatus.APPROVED)
                .body("State Newton's first law.")
                .insert();
        data.link(questionId, conceptId).insert();
        data.question(8L)
                .status(QuestionStatus.APPROVED)
                .body("Servable question eight.")
                .insert();
        data.link(8L, conceptId).insert();

        long count = statements.countDuring(() ->
                given().when().get(QUESTIONS_PATH + "/" + questionId).then().statusCode(OK.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }
}
