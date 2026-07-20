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
public class QuestionAttemptDatabaseIT {

    @Inject
    private QuestionTestData data;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureQuestionAttemptCreatedWithDefaultFields() {
        long questionId = 1L;
        data.question(questionId).insert();

        String sessionToken = "sessionToken";
        String body = "Mark scheme body";
        data.questionAttempt(questionId, sessionToken, body).insert();

        List<DBRow> questionAttempts = data.retrieveQuestionAttempts();
        DBRow questionAttempt = questionAttempts.getFirst();
        questionAttempt.assertThat("id", greaterThan(0L));
        questionAttempt.assertThat("question_id", equalTo(questionId));
        questionAttempt.assertThat("session_token", equalTo(sessionToken));
        questionAttempt.assertThat("body", equalTo(body));
        questionAttempt.assertThat("created_at", allOf(
                greaterThan(Instant.EPOCH),
                lessThanOrEqualTo(Instant.now())
        ));
        questionAttempt.assertAllColumnsChecked();
    }

    @Test
    void ensureQuestionAttemptDeletedWhenQuestionsDeleted() {
        long questionId = 1L;
        data.question(questionId).insert();
        data.questionAttempt(questionId, "sessionToken", "body").insert();
        data.questionAttempt(questionId, "sessionToken", "body").insert();

        //Add another question and attempt
        long otherQuestionId = 2L;
        data.question(otherQuestionId).insert();
        data.questionAttempt(otherQuestionId, "sessionToken", "body").insert();

        List<DBRow> questions = data.retrieveQuestions();
        assertThat(questions, hasSize(2));
        assertThat(DBRow.collectColumn(questions, "id"), contains(questionId, otherQuestionId));

        List<DBRow> questionAttempts = data.retrieveQuestionAttempts();
        assertThat(questionAttempts, hasSize(3));
        assertThat(DBRow.collectColumn(questionAttempts, "question_id"), contains(questionId, questionId, otherQuestionId));

        data.deleteQuestion(questionId);

        List<DBRow> questionsAfterDelete = data.retrieveQuestions();
        assertThat(questionsAfterDelete, hasSize(1));
        assertThat(DBRow.collectColumn(questionsAfterDelete, "id"), contains(otherQuestionId));

        List<DBRow> questionAttemptsAfterDelete = data.retrieveQuestionAttempts();
        assertThat(questionAttemptsAfterDelete, hasSize(1));
        assertThat(DBRow.collectColumn(questionAttemptsAfterDelete, "question_id"), contains(otherQuestionId));
    }

    @Test
    void ensureThatQuestionMustExist() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.questionAttempt(1L, "sessionToken", "body").insert());

        assertThat(sqlStateOf(thrown), equalTo(FOREIGN_KEY_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("question_id"));
    }

    @Test
    void ensureThatQuestionIdMustBeSet() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.questionAttempt().sessionToken("sessionToken").body("Test").insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"question_id\""));
    }

    @Test
    void ensureThatBodyMustBeSet() {
        long questionId = 1L;
        data.question(questionId).insert();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.questionAttempt().questionId(1L).sessionToken("sessionToken").insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"body\""));
    }

    @Test
    void ensureThatSessionTokenMustBeSet() {
        long questionId = 1L;
        data.question(questionId).insert();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.questionAttempt().questionId(1L).body("body").insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"session_token\""));
    }
}
