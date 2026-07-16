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

// Pins the JDBC statement count for serving the concept list. This endpoint is unpaged — it returns every
// concept row — so it is the one place where a per-row query has no page size to blunt it. Concept has no
// associations today; the row-count invariance assertion is the tripwire for the day one is added.
@PerformanceTest
public class ConceptCataloguePT {

    private static final String CONCEPTS_PATH = "/api/v1/concepts";

    // A single unpaged SELECT — no count query, and no associations to resolve.
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
    void servingTheConceptListFiresAConstantNumberOfStatements() {
        insertConcepts(3);

        long count = statements.countDuring(() ->
                given().when().get(CONCEPTS_PATH).then().statusCode(OK.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }

    @Test
    void servingMoreConceptsDoesNotFireMoreStatements() {
        insertConcepts(2);

        long fewer = statements.countDuring(() ->
                given().when().get(CONCEPTS_PATH).then().statusCode(OK.getCode()));

        data.clear();
        insertConcepts(6);

        long more = statements.countDuring(() ->
                given().when().get(CONCEPTS_PATH).then().statusCode(OK.getCode()));

        // Nothing caps this endpoint's rows, so cost must be independent of how many concepts exist.
        assertThat(more, equalTo(fewer));
    }

    private void insertConcepts(int count) {
        for (long id = 1; id <= count; id++) {
            data.concept(id).insert();
        }
    }
}
