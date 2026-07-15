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

// Pins the JDBC statement count for serving the catalogue — the hot, row-scaling path. As well as a fixed
// happy-path count it asserts the count does NOT grow with the number of rows, which is the property an
// eager association (N+1) would break.
@PerformanceTest
public class QuestionCataloguePT {

    private static final String QUESTIONS_PATH = "/api/v1/questions";
    private static final long CONCEPT_ID = 100L;

    // Paged content SELECT + the page COUNT + the single concept-link stitch query.
    private static final long EXPECTED_STATEMENTS = 3L;

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
    void servingTheCatalogueFiresAConstantNumberOfStatements() {
        insertApprovedLinkedQuestions(3);

        long count = statements.countDuring(() ->
                given().when().get(QUESTIONS_PATH).then().statusCode(OK.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }

    @Test
    void servingMoreRowsDoesNotFireMoreStatements() {
        insertApprovedLinkedQuestions(2);
        long fewer = statements.countDuring(() ->
                given().when().get(QUESTIONS_PATH + "?size=50").then().statusCode(OK.getCode()));

        data.clear();
        data.concept(CONCEPT_ID).insert();
        insertApprovedLinkedQuestions(6);
        long more = statements.countDuring(() ->
                given().when().get(QUESTIONS_PATH + "?size=50").then().statusCode(OK.getCode()));

        // The count is a property of the query plan, not the row count: three times the rows, same statements.
        assertThat(more, equalTo(fewer));
    }

    private void insertApprovedLinkedQuestions(int count) {
        for (long id = 1; id <= count; id++) {
            data.question(id)
                    .status(QuestionStatus.APPROVED)
                    .body("Question " + id)
                    .source(QuestionSource.SEED)
                    .insert();
            data.link(id, CONCEPT_ID).insert();
        }
    }
}
