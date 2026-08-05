package integration.db;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.practiq.domain.types.DocumentStatus;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.DBRow;
import utils.data.TestData;

@IntegrationTest
public class QuestionOriginDatabaseIT {

    @Inject
    private TestData data;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureQuestionOriginCreatedWithDefaultFields() {
        long questionId = 4L;
        data.question(questionId).insert();

        data.questionOrigin(questionId)


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
}
