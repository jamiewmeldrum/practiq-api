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
public class QuestionCataloguePT {

    private static final String QUESTIONS_PATH = "/api/v1/questions";

    private static final long EXPECTED_STATEMENTS = 3L;

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
    void servingTheCatalogueFiresAConstantNumberOfStatements() {
        long conceptId = 100L;
        data.concept(conceptId).insert();
        for (long id = 1; id <= 3; id++) {
            data.question(id)
                    .status(QuestionStatus.APPROVED)
                    .body("Question " + id)
                    .insert();
            data.link(id, conceptId).insert();
        }

        long count = statements.countDuring(
                () -> given().when().get(QUESTIONS_PATH).then().statusCode(OK.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }

    @Test
    void servingMoreRowsDoesNotFireMoreStatements() {
        long conceptId = 100L;
        data.concept(conceptId).insert();
        for (long id = 1; id <= 2; id++) {
            data.question(id)
                    .status(QuestionStatus.APPROVED)
                    .body("Question " + id)
                    .insert();
            data.link(id, conceptId).insert();
        }

        long fewer = statements.countDuring(
                () -> given().when().get(QUESTIONS_PATH + "?size=50").then().statusCode(OK.getCode()));

        data.clear();
        data.concept(conceptId).insert();
        for (long id = 1; id <= 6; id++) {
            data.question(id)
                    .status(QuestionStatus.APPROVED)
                    .body("Question " + id)
                    .insert();
            data.link(id, conceptId).insert();
        }

        long more = statements.countDuring(
                () -> given().when().get(QUESTIONS_PATH + "?size=50").then().statusCode(OK.getCode()));

        assertThat(more, equalTo(fewer));
    }
}
