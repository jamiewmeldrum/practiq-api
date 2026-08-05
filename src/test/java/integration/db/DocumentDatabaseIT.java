package integration.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.data.TestDatabase.*;

import com.practiq.domain.types.DocumentStatus;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.DBRow;
import utils.data.TestData;

@IntegrationTest
class DocumentDatabaseIT {

    @Inject
    private TestData data;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureDocumentCreatedWithDefaultFields() {
        String s3Key = "s3:path";
        String filename = "file:path";
        data.document(s3Key, filename).insert();

        List<DBRow> documents = data.retrieveDocuments();
        DBRow document = documents.getFirst();
        document.assertThat("id", greaterThan(0L));
        document.assertThat("version", equalTo(0));
        document.assertThat("s3_key", equalTo(s3Key));
        document.assertThat("filename", equalTo(filename));
        document.assertThat("source_spec", nullValue());
        document.assertThat("status", equalTo(DocumentStatus.UNAPPROVED.name()));
        document.assertThat("created_at", allOf(greaterThan(Instant.EPOCH), lessThanOrEqualTo(Instant.now())));
        document.assertAllColumnsChecked();
    }

    @Test
    void ensureThatS3KeyMustBeSet() {
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> data.document().filename("filename").insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"s3_key\""));
    }

    @Test
    void ensureThatFilenameMustBeSet() {
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> data.document().s3Key("s3_key").insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"filename\""));
    }

    @Test
    void ensureThatStatusMustBeValid() {
        data.document("s3:path1", "file:path").status(DocumentStatus.APPROVED).insert();

        List<DBRow> documents = data.retrieveDocuments();
        DBRow document = documents.getFirst();
        document.assertThat("status", equalTo(DocumentStatus.APPROVED.name()));

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> data.document("s3:path2", "file:path").status("INVALID").insert());

        assertThat(sqlStateOf(thrown), equalTo(CHECK_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("status"));
    }

    @Test
    void ensureThatS3KeyMustBeUnique() {
        String s3Key = "s3:path";
        data.document(s3Key, "file:path1").insert();

        List<DBRow> documents = data.retrieveDocuments();
        DBRow document = documents.getFirst();
        document.assertThat("s3_key", equalTo(s3Key));

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> data.document(s3Key, "file:path2")
                        .insert());

        assertThat(sqlStateOf(thrown), equalTo(UNIQUE_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("s3_key"));
    }

    @Test
    void ensureThatSourceSpecMustNotBeTooLong() {
        String sourceSpec = RandomStringUtils.insecure().nextAlphanumeric(255);
        data.document("s3:path1", "file:path1").sourceSpec(sourceSpec).insert();

        List<DBRow> documents = data.retrieveDocuments();
        DBRow document = documents.getFirst();
        document.assertThat("source_spec", equalTo(sourceSpec));

        String sourceSpecTooLong = RandomStringUtils.insecure().nextAlphanumeric(256);
        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> data.document("s3:path2", "file:path2")
                        .sourceSpec(sourceSpecTooLong)
                        .insert());

        assertThat(sqlStateOf(thrown), equalTo(VALUE_TOO_LONG));
        assertThat(
                thrown.getCause().getMessage(),
                containsString("ERROR: value too long for type character varying(255)"));
    }
}
