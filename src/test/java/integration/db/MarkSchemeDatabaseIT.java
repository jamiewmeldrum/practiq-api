package integration.db;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.DBRow;
import utils.data.QuestionTestData;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.data.TestDatabase.*;

@IntegrationTest
public class MarkSchemeDatabaseIT {

    @Inject
    private QuestionTestData data;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureMarkSchemeCreatedWithDefaultFields() {
        long questionId = 1L;
        data.question(questionId).insert();

        String body = "Mark scheme body";
        data.markScheme(questionId, body).insert();

        List<DBRow> markSchemes = data.retrieveMarkSchemes();
        DBRow markScheme = markSchemes.getFirst();
        markScheme.assertThat("id", greaterThan(0L));
        markScheme.assertThat("question_id", equalTo(questionId));
        markScheme.assertThat("version", equalTo(0));
        markScheme.assertThat("body", equalTo(body));
        markScheme.assertThat("created_at", allOf(
                greaterThan(Instant.EPOCH),
                lessThanOrEqualTo(Instant.now())
        ));
        markScheme.assertAllColumnsChecked();
    }

    @Test
    void ensureMarkSchemeDeletedWhenQuestionsDeleted() {
        long questionId = 1L;
        data.question(questionId).insert();

        data.markScheme(questionId, "body").insert();

        List<DBRow> questions = data.retrieveQuestions();
        assertThat(questions, hasSize(1));

        List<DBRow> markSchemes = data.retrieveMarkSchemes();
        assertThat(markSchemes, hasSize(1));

        data.deleteQuestion(questionId);

        List<DBRow> questionsAfterDelete = data.retrieveQuestions();
        assertThat(questionsAfterDelete, empty());

        List<DBRow> markSchemesAfterDelete = data.retrieveMarkSchemes();
        assertThat(markSchemesAfterDelete, empty());
    }

    @Test
    void ensureThatQuestionMustExist() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.markScheme().questionId(1L).body("body").insert());

        assertThat(sqlStateOf(thrown), equalTo(FOREIGN_KEY_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("question_id"));
    }

    @Test
    void ensureThatThereCanOnlyBeOneMarkSchemeForAQuestion() {
        long questionId = 1L;
        data.question(questionId).insert();

        data.markScheme().questionId(questionId).body("body").insert();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.markScheme().questionId(questionId).body("body").insert());

        assertThat(sqlStateOf(thrown), equalTo(UNIQUE_VIOLATION));
    }

    @Test
    void ensureThatQuestionIdMustBeSet() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.markScheme().body("Test").insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"question_id\""));
    }

    @Test
    void ensureThatBodyMustBeSet() {
        long questionId = 1L;
        data.question(questionId).insert();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.markScheme().questionId(1L).insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"body\""));
    }
}
