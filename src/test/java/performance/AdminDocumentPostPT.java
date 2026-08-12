package performance;

import static io.micronaut.http.HttpStatus.CREATED;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.data.TestData.ADMIN_KEY;
import static utils.data.TestData.ADMIN_KEY_HEADER;

import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.PerformanceTest;
import utils.StatementCounter;
import utils.data.TestData;

@PerformanceTest
public class AdminDocumentPostPT {

    private static final String ADMIN_DOCUMENTS_PATH = "/api/v1/admin/documents";

    // One INSERT. The @Generated read-back of id/created_at rides Postgres's `insert … returning`, so a
    // second statement appearing means that mechanism broke. Presigning is local and touches no database.
    private static final long EXPECTED_STATEMENTS = 1L;

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
    void postingDocumentFiresAConstantNumberOfStatements() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filename", "practiq-presign.txt");
        requestBody.put("contentType", "text/plain");
        requestBody.put("contentLength", 2048);
        requestBody.put("sourceSpec", "AQA 2007");

        long count = statements.countDuring(() -> given().contentType(ContentType.JSON)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .body(requestBody)
                .when()
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(CREATED.getCode()));

        assertThat(count, equalTo(EXPECTED_STATEMENTS));
    }
}
