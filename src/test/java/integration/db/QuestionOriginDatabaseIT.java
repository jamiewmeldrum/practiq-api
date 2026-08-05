package integration.db;

import static org.hamcrest.Matchers.*;

import com.practiq.domain.types.QuestionOriginSource;
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

        QuestionOriginSource source = QuestionOriginSource.EXTRACTED;
        data.questionOrigin(questionId, source).insert();

        List<DBRow> questionOrigins = data.retrieveQuestionOrigins();
        DBRow questionOrigin = questionOrigins.getFirst();
        questionOrigin.assertThat("id", greaterThan(0L));
        questionOrigin.assertThat("question_id", equalTo(questionId));
        questionOrigin.assertThat("source", equalTo(source.name()));
        questionOrigin.assertThat("document_id", nullValue());
        questionOrigin.assertThat("created_at", allOf(greaterThan(Instant.EPOCH), lessThanOrEqualTo(Instant.now())));
        questionOrigin.assertAllColumnsChecked();
    }
}
