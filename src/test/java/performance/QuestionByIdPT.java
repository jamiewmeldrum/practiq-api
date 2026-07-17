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
        servableQuestion(questionId, conceptId, "State Newton's first law.");
        servableQuestion(8L, conceptId, "Servable question eight.");

        long count = statements.countDuring(() ->
                given().when().get(QUESTIONS_PATH + "/" + questionId).then().statusCode(OK.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }

    private void servableQuestion(long id, long conceptId, String body) {
        data.question(id)
                .status(QuestionStatus.APPROVED)
                .body(body)
                .source(QuestionSource.SEED)
                .insert();
        data.link(id, conceptId).insert();
    }
}
