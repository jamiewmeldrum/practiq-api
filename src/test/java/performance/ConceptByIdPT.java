package performance;

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

// Pins the JDBC statement count for serving a single concept by id.
@PerformanceTest
public class ConceptByIdPT {

    private static final String CONCEPTS_PATH = "/api/v1/concepts";

    // A single findById.
    private static final long EXPECTED_STATEMENTS = 1L;

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
    void servingAConceptByIdFiresAConstantNumberOfStatements() {
        // Two rows: the statement count must be a property of the query plan, not of the target happening
        // to be the only row in the table.
        long conceptId = 7L;
        data.concept(conceptId).insert();
        data.concept(8L).insert();

        long count = statements.countDuring(() ->
                given().when().get(CONCEPTS_PATH + "/" + conceptId).then().statusCode(OK.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }
}
