package integration.e2e;

import static io.micronaut.http.HttpStatus.CREATED;
import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static utils.data.TestData.ADMIN_KEY_HEADER;

import com.practiq.domain.types.DocumentStatus;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import utils.IntegrationTest;
import utils.aws.S3TestUtils;
import utils.data.DBRow;
import utils.data.TestData;

@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminDocumentControllerIT {

    private static final String TARGET_BUCKET = "documents";

    private static final String DOCUMENTS_PATH = "/api/v1/admin/documents";

    // TODO - I need to think about this more. I'm not sure I get it
    // The content type the presign signs for a .txt upload, spelled out rather than read from FileType so a
    // change to that mapping fails here instead of moving in lockstep with it. The client must send this
    // header byte-for-byte: it is inside X-Amz-SignedHeaders, so any deviation (including a charset suffix)
    // invalidates the signature against real S3.
    private static final String SIGNED_CONTENT_TYPE = "text/plain";

    @Inject
    private TestData data;

    @Inject
    private S3TestUtils s3TestUtils;

    @Inject
    private EmbeddedServer embeddedServer;

    @BeforeAll
    void setup() {
        s3TestUtils.ensureBucketExistsAndIsEmpty(TARGET_BUCKET);
    }

    @BeforeEach
    void setUp() {
        data.clear();
        s3TestUtils.emptyBucket(TARGET_BUCKET);
        RestAssured.port = embeddedServer.getPort();
    }

    // TODO - review this and decide what to extract/improve - some checks want to be a broader e.g. make sure s3 key is
    // uuid
    @Test
    void canPostDocumentThenPutDocumentInS3UsingPresignResponse() throws IOException {
        String filename = "practiq-presign.txt";
        String sourceSpec = "AQA 2007";
        byte[] content = "uploaded straight to S3 through a presigned URL".getBytes(StandardCharsets.UTF_8);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filename", filename);
        requestBody.put("contentType", "txt");
        requestBody.put("contentLength", content.length);
        requestBody.put("sourceSpec", sourceSpec);

        Response response = given().when()
                .body(requestBody)
                .header(new Header(ADMIN_KEY_HEADER, "admin"))
                .contentType(ContentType.JSON)
                .post(DOCUMENTS_PATH)
                .then()
                .statusCode(CREATED.getCode())
                .contentType(ContentType.JSON)
                .extract()
                .response();

        Number documentId = response.path("id");
        String presignedUrl = response.path("url");

        List<DBRow> documents = data.retrieveDocuments();
        assertThat(documents.size(), equalTo(1));

        DBRow document = documents.getFirst();
        String s3Key = document.get("s3_key");
        document.assertThat("id", equalTo(documentId.longValue()));
        document.assertThat("filename", equalTo(filename));
        document.assertThat("source_spec", equalTo(sourceSpec));
        document.assertThat("status", equalTo(DocumentStatus.AWAITING_UPLOAD.name()));

        // The key is server-minted, never the client's filename: two admins uploading "paper.pdf" must not
        // collide, and a caller must not be able to name an object it does not own.
        assertThat(s3Key, not(equalTo(filename)));
        assertThat(s3Key, equalTo(UUID.fromString(s3Key.substring(0, s3Key.lastIndexOf('.'))) + ".txt"));

        // The URL the caller is handed must address the very object the row records — otherwise the row and
        // the artefact drift apart and the reconcile has nothing to match on.
        assertThat(URI.create(presignedUrl).getPath(), endsWith("/" + s3Key));

        given().urlEncodingEnabled(false)
                // RestAssured appends "; charset=..." to text/* by default, which would not match the signed
                // Content-Type. LocalStack does not verify signatures so it would pass here regardless; real
                // S3 would reject it, so the test sends what a real client has to send.
                .config(config().encoderConfig(
                                encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .when()
                .body(content)
                .contentType(SIGNED_CONTENT_TYPE)
                .put(presignedUrl)
                .then()
                .statusCode(OK.getCode());

        assertThat(
                s3TestUtils.getFileContent(TARGET_BUCKET, s3Key),
                equalTo(Optional.of(new String(content, StandardCharsets.UTF_8))));
        assertThat(s3TestUtils.getContentType(TARGET_BUCKET, s3Key), equalTo(Optional.of(SIGNED_CONTENT_TYPE)));
    }
}
