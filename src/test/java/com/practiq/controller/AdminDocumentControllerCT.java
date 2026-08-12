package com.practiq.controller;

import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static utils.data.TestData.ADMIN_KEY;
import static utils.data.TestData.ADMIN_KEY_HEADER;
import static utils.data.TestData.MAX_UPLOAD_CONTENT_LENGTH;
import static utils.data.TestData.UPLOAD_URL_EXPIRY;

import com.practiq.domain.Document;
import com.practiq.domain.types.DocumentStatus;
import com.practiq.repository.DocumentRepository;
import com.practiq.storage.PresignedUploadRequest;
import com.practiq.storage.S3DocumentStorage;
import io.micronaut.objectstorage.aws.AwsS3Operations;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import utils.ComponentTest;
import utils.RegexUtils;
import utils.TestReflection;

@ComponentTest
public class AdminDocumentControllerCT {

    private static final String ADMIN_DOCUMENTS_PATH = "/api/v1/admin/documents";

    private static final String DOCUMENTS_BUCKET = "documents";

    @Inject
    private EmbeddedServer embeddedServer;

    @Inject
    private DocumentRepository documentRepository;

    @Inject
    private S3DocumentStorage documentStorage;

    // Persistence is the one thing the DB-less CT tier cannot supply, so the repository is a mock.
    @MockBean(DocumentRepository.class)
    DocumentRepository documentRepository() {
        return mock(DocumentRepository.class);
    }

    // A spy, not a mock: it presigns for real unless a test says otherwise, so the URL read back below
    // is genuinely signed while the presign-failure path stays reachable from this same class.
    @MockBean(S3DocumentStorage.class)
    S3DocumentStorage documentStorage(@Named(DOCUMENTS_BUCKET) AwsS3Operations documentsBucket) {
        return spy(new S3DocumentStorage(documentsBucket));
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void postDocumentSavesAnAwaitingUploadDocumentAndReturnsAPresignedUrlForItsKey() {
        String filename = "practiq-presign.txt";
        String sourceSpec = "AQA 2007";
        long savedId = 99L;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filename", filename);
        requestBody.put("contentType", "text/plain");
        requestBody.put("contentLength", 2048);
        requestBody.put("sourceSpec", sourceSpec);

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> TestReflection.setField(invocation.getArgument(0), "id", savedId));

        Response response = given().when()
                .body(requestBody)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(CREATED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("id", "url", "expiresAt"))
                .extract()
                .response();

        ArgumentCaptor<Document> savedDocument = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(savedDocument.capture());

        Document document = savedDocument.getValue();
        assertThat(document.getS3Key(), matchesPattern(RegexUtils.uuidWithExtension("txt")));
        assertThat(document.getFilename(), equalTo(filename));
        assertThat(document.getSourceSpec(), equalTo(sourceSpec));
        assertThat(document.getStatus(), equalTo(DocumentStatus.AWAITING_UPLOAD));

        // The id returned is the one persistence assigned to the row that was saved, not a value the
        // controller invented, so it is asserted against the saved document rather than the stub.
        assertThat(response.path("id"), equalTo((int) document.getId()));
        assertThat(document.getId(), equalTo(savedId));

        // The URL handed back must address the very object the saved row names, or the row and the
        // artefact drift apart and the reconcile has nothing to match on.
        URI presignedUrl = URI.create(response.path("url"));

        assertThat(presignedUrl.getPath(), equalTo("/" + document.getS3Key()));
        assertThat(presignedUrl.getQuery(), containsString("X-Amz-Signature="));

        // The expiry is the presigner's own, so the caller knows how long the URL it was handed lasts.
        Instant expiresAt = Instant.parse(response.path("expiresAt"));

        assertThat(expiresAt.isAfter(Instant.now()), equalTo(true));
        assertThat(expiresAt.isBefore(Instant.now().plus(UPLOAD_URL_EXPIRY)), equalTo(true));
    }

    @Test
    void postDocumentSavesADocumentWithoutASourceSpecWhenNoneIsSupplied() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filename", "practiq-presign.txt");
        requestBody.put("contentType", "text/plain");
        requestBody.put("contentLength", 2048);

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> TestReflection.setField(invocation.getArgument(0), "id", 99L));

        given().when()
                .body(requestBody)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(CREATED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("id", "url", "expiresAt"));

        ArgumentCaptor<Document> savedDocument = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(savedDocument.capture());

        assertThat(savedDocument.getValue().getSourceSpec(), nullValue());
    }

    @Test
    void postDocumentErrorsAndSavesNothingWhenThePresignFails() {
        doThrow(new IllegalStateException("presigner unavailable"))
                .when(documentStorage)
                .presignUpload(any(PresignedUploadRequest.class));

        given().when()
                .body(aValidRequestBody())
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(INTERNAL_SERVER_ERROR.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("An unspecified error occurred."))
                .body("status", equalTo(500));

        // The presign runs before the save, so a failing presigner must leave no row behind.
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void postDocumentErrorsAndReturnsNoUrlWhenTheSaveFails() {
        when(documentRepository.save(any(Document.class))).thenThrow(new IllegalStateException("database unavailable"));

        given().when()
                .body(aValidRequestBody())
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(INTERNAL_SERVER_ERROR.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("An unspecified error occurred."))
                .body("status", equalTo(500));
    }

    @Test
    void postDocumentErrorsIfContentLengthExceedsTheMaximum() {
        Map<String, Object> requestBody = aValidRequestBody();
        requestBody.put("contentLength", MAX_UPLOAD_CONTENT_LENGTH + 1);

        given().when()
                .body(requestBody)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(REQUEST_ENTITY_TOO_LARGE.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("contentLength: must not be greater than " + MAX_UPLOAD_CONTENT_LENGTH))
                .body("status", equalTo(413));

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void postDocumentErrorsIfContentTypeDoesNotMatchTheFilenameExtension() {
        Map<String, Object> requestBody = aValidRequestBody();
        requestBody.put("contentType", "application/pdf");

        given().when()
                .body(requestBody)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("contentType: 'application/pdf' does not match the '.txt' file extension"))
                .body("status", equalTo(422));

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void postDocumentErrorsIfAdminKeyOmitted() {
        given().when()
                .body(aValidRequestBody())
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNAUTHORIZED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                // Byte-for-byte the response a wrong key gets, so an absent header is not a distinguishable
                // signal to an unauthenticated caller.
                .body("error", equalTo("Unauthorised to access " + ADMIN_DOCUMENTS_PATH))
                .body("status", equalTo(401));

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void postDocumentErrorsIfAdminKeyBlank() {
        // Empty
        given().when()
                .body(aValidRequestBody())
                .header(new Header(ADMIN_KEY_HEADER, ""))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNAUTHORIZED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Unauthorised to access " + ADMIN_DOCUMENTS_PATH))
                .body("status", equalTo(401));

        // Blank
        given().when()
                .body(aValidRequestBody())
                .header(new Header(ADMIN_KEY_HEADER, "  "))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNAUTHORIZED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Unauthorised to access " + ADMIN_DOCUMENTS_PATH))
                .body("status", equalTo(401));
    }

    @Test
    void postDocumentErrorsIfAdminKeyMismatches() {
        given().when()
                .body(aValidRequestBody())
                .header(new Header(ADMIN_KEY_HEADER, "admin"))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNAUTHORIZED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Unauthorised to access " + ADMIN_DOCUMENTS_PATH))
                .body("status", equalTo(401));
    }

    @Test
    void postDocumentErrorsIfFilenameIsBlank() {
        Map<String, Object> nullFilename = aValidRequestBody();
        nullFilename.put("filename", null);
        given().when()
                .body(nullFilename)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("filename: must not be blank"))
                .body("status", equalTo(422));

        Map<String, Object> emptyFilename = aValidRequestBody();
        emptyFilename.put("filename", "");
        given().when()
                .body(emptyFilename)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("filename: must not be blank"))
                .body("status", equalTo(422));

        Map<String, Object> whitespaceFilename = aValidRequestBody();
        whitespaceFilename.put("filename", "   ");
        given().when()
                .body(whitespaceFilename)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("filename: must not be blank"))
                .body("status", equalTo(422));
    }

    @Test
    void postDocumentErrorsIfContentLengthIsMissingOrBelowOne() {
        Map<String, Object> nullContentLength = aValidRequestBody();
        nullContentLength.put("contentLength", null);
        given().when()
                .body(nullContentLength)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("contentLength: must not be null"))
                .body("status", equalTo(422));

        Map<String, Object> zeroContentLength = aValidRequestBody();
        zeroContentLength.put("contentLength", 0);
        given().when()
                .body(zeroContentLength)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("contentLength: must be greater than or equal to 1"))
                .body("status", equalTo(422));
    }

    @Test
    void postDocumentErrorsIfContentTypeIsBlank() {
        Map<String, Object> nullContentType = aValidRequestBody();
        nullContentType.put("contentType", null);
        given().when()
                .body(nullContentType)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("contentType: must not be blank"))
                .body("status", equalTo(422));

        Map<String, Object> emptyContentType = aValidRequestBody();
        emptyContentType.put("contentType", "");
        given().when()
                .body(emptyContentType)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("contentType: must not be blank"))
                .body("status", equalTo(422));

        Map<String, Object> whitespaceContentType = aValidRequestBody();
        whitespaceContentType.put("contentType", "   ");
        given().when()
                .body(whitespaceContentType)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("contentType: must not be blank"))
                .body("status", equalTo(422));
    }

    @Test
    void postDocumentErrorsIfSourceSpecExceedsTwoHundredAndFiftyFiveCharacters() {
        Map<String, Object> longSourceSpec = aValidRequestBody();
        longSourceSpec.put("sourceSpec", "a".repeat(256));

        given().when()
                .body(longSourceSpec)
                .header(new Header(ADMIN_KEY_HEADER, ADMIN_KEY))
                .contentType(ContentType.JSON)
                .post(ADMIN_DOCUMENTS_PATH)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("sourceSpec: size must be between 0 and 255"))
                .body("status", equalTo(422));
    }

    private Map<String, Object> aValidRequestBody() {
        String filename = "practiq-presign.txt";
        String sourceSpec = "AQA 2007";
        byte[] content = "uploaded straight to S3 through a presigned URL".getBytes(StandardCharsets.UTF_8);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filename", filename);
        requestBody.put("contentType", "text/plain");
        requestBody.put("contentLength", content.length);
        requestBody.put("sourceSpec", sourceSpec);
        return requestBody;
    }
}
