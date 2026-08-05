package integration.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.data.TestDatabase.*;

import com.practiq.domain.types.QuestionAuthorship;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.DBRow;
import utils.data.TestData;

@IntegrationTest
class QuestionOriginDatabaseIT {

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

        QuestionAuthorship authorship = QuestionAuthorship.EXTRACTED;
        data.questionOrigin(questionId, authorship).insert();

        List<DBRow> questionOrigins = data.retrieveQuestionOrigins();
        DBRow questionOrigin = questionOrigins.getFirst();
        questionOrigin.assertThat("id", greaterThan(0L));
        questionOrigin.assertThat("question_id", equalTo(questionId));
        questionOrigin.assertThat("authorship", equalTo(authorship.name()));
        questionOrigin.assertThat("document_id", nullValue());
        questionOrigin.assertThat("created_at", allOf(greaterThan(Instant.EPOCH), lessThanOrEqualTo(Instant.now())));
        questionOrigin.assertAllColumnsChecked();
    }

    @Test
    void ensureQuestionOriginCanBeLinkedToDocument() {
        long questionId = 3L;
        data.question(questionId).insert();

        long documentId = 4L;
        data.document("s3Key", "filename").id(documentId).insert();

        QuestionAuthorship authorship = QuestionAuthorship.GENERATED;
        data.questionOrigin(questionId, authorship).documentId(documentId).insert();

        List<DBRow> questionOrigins = data.retrieveQuestionOrigins();
        DBRow questionOrigin = questionOrigins.getFirst();
        questionOrigin.assertThat("id", greaterThan(0L));
        questionOrigin.assertThat("question_id", equalTo(questionId));
        questionOrigin.assertThat("authorship", equalTo(authorship.name()));
        questionOrigin.assertThat("document_id", equalTo(documentId));
        questionOrigin.assertThat("created_at", allOf(greaterThan(Instant.EPOCH), lessThanOrEqualTo(Instant.now())));
        questionOrigin.assertAllColumnsChecked();
    }

    @Test
    void ensureQuestionOriginDeletedWhenQuestionDeleted() {
        long questionId = 5L;
        data.question(questionId).insert();

        QuestionAuthorship authorship = QuestionAuthorship.EXTRACTED;
        data.questionOrigin(questionId, authorship).insert();

        long otherQuestionId = 6L;
        data.question(otherQuestionId).insert();
        data.questionOrigin(otherQuestionId, authorship).insert();

        List<DBRow> questionOriginsBeforeDelete = data.retrieveQuestionOrigins();
        assertThat(questionOriginsBeforeDelete, hasSize(2));
        assertThat(
                DBRow.collectColumn(questionOriginsBeforeDelete, "question_id"), contains(questionId, otherQuestionId));

        data.deleteQuestion(questionId);

        List<DBRow> questionOriginsAfterDelete = data.retrieveQuestionOrigins();
        assertThat(questionOriginsAfterDelete, hasSize(1));
        assertThat(DBRow.collectColumn(questionOriginsAfterDelete, "question_id"), contains(otherQuestionId));
    }

    @Test
    void ensureThatQuestionMustExist() {
        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> data.questionOrigin(3L, QuestionAuthorship.EXTRACTED)
                        .insert());

        assertThat(sqlStateOf(thrown), equalTo(FOREIGN_KEY_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("question_id"));
    }

    @Test
    void ensureThatDocumentMustExistWhenLinkedToDocument() {
        long questionId = 5L;
        data.question(questionId).insert();

        QuestionAuthorship authorship = QuestionAuthorship.EXTRACTED;

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> data.questionOrigin(questionId, authorship)
                        .documentId(56L)
                        .insert());

        assertThat(sqlStateOf(thrown), equalTo(FOREIGN_KEY_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("document_id"));
    }

    @Test
    void ensureThatAuthorshipMustBeValid() {
        long questionId = 5L;
        data.question(questionId).insert();

        QuestionAuthorship authorship = QuestionAuthorship.EXTRACTED;
        data.questionOrigin(questionId, authorship).insert();

        List<DBRow> questionOrigins = data.retrieveQuestionOrigins();
        DBRow questionOrigin = questionOrigins.getFirst();
        questionOrigin.assertThat("authorship", equalTo(authorship.name()));

        long otherQuestionId = 9L;
        data.question(otherQuestionId).insert();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> data.questionOrigin()
                .questionId(otherQuestionId)
                .authorship("RANDOM")
                .insert());

        assertThat(sqlStateOf(thrown), equalTo(CHECK_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("authorship"));
    }

    @Test
    void ensureThatQuestionIdMustExist() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> data.questionOrigin()
                .authorship(QuestionAuthorship.EXTRACTED)
                .insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("question_id"));
    }

    @Test
    void ensureThatAuthorshipMustExist() {
        long questionId = 12L;
        data.question(questionId).insert();

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> data.questionOrigin().questionId(questionId).insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("authorship"));
    }

    @Test
    void ensureThatQuestionCanOnlyHaveOneOrigin() {
        long questionId = 16L;
        data.question(questionId).insert();
        data.questionOrigin(questionId, QuestionAuthorship.AUTHORED).insert();

        List<DBRow> questionOrigins = data.retrieveQuestionOrigins();
        assertThat(questionOrigins, hasSize(1));
        DBRow questionOrigin = questionOrigins.getFirst();
        questionOrigin.assertThat("question_id", equalTo(questionId));

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class, () -> data.questionOrigin(questionId, QuestionAuthorship.AUTHORED)
                        .insert());

        assertThat(sqlStateOf(thrown), equalTo(UNIQUE_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("question_id"));
    }
}
