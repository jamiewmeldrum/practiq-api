package integration.db;

import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
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
class QuestionDatabaseIT {

    @Inject
    private QuestionTestData data;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureQuestionCreatedWithDefaultFields() {
        String body = "Explain what is meant by diffraction.";
        QuestionSource source = QuestionSource.SEED;
        data.question().body(body).source(source).insert();

        List<DBRow> questions = data.retrieveQuestions();
        DBRow question = questions.getFirst();
        question.assertThat("id", greaterThan(0L));
        question.assertThat("version", equalTo(0));
        question.assertThat("body", equalTo(body));
        question.assertThat("difficulty", nullValue());
        question.assertThat("type", nullValue());
        question.assertThat("source", equalTo(source.name()));
        question.assertThat("status", equalTo(QuestionStatus.PENDING.name()));
        question.assertThat("source_spec", nullValue());
        question.assertThat("created_at", allOf(
                greaterThan(Instant.EPOCH),
                lessThanOrEqualTo(Instant.now())
        ));
        question.assertAllColumnsChecked();

        List<DBRow> questionConcepts = data.retrieveLinks();
        assertThat(questionConcepts, empty());
    }

    @Test
    void ensureQuestionCanBeCreatedWithQuestionConceptLinks() {
        long questionId = 1L;
        data.question(questionId).insert();

        long conceptId1 = 10L;
        data.concept(conceptId1).insert();
        data.link(questionId, conceptId1).insert();

        long conceptId2 = 20L;
        data.concept(conceptId2).insert();
        data.link(questionId, conceptId2).insert();

        List<DBRow> questionConcepts = data.retrieveLinks();
        assertThat(questionConcepts, hasSize(2));
        questionConcepts.forEach(c -> c.assertThat("question_id", equalTo(questionId)));

        List<Long> linkedConceptIds = questionConcepts.stream()
                .map(c -> c.<Long>get("concept_id"))
                .toList();
        assertThat(linkedConceptIds, containsInAnyOrder(conceptId1, conceptId2));
    }

    @Test
    void ensureQuestionConceptsDeletedWhenQuestionsDeleted() {
        long questionId = 1L;
        data.question(questionId).insert();

        long conceptId1 = 10L;
        data.concept(conceptId1).insert();
        data.link(questionId, conceptId1).insert();

        long conceptId2 = 20L;
        data.concept(conceptId2).insert();
        data.link(questionId, conceptId2).insert();

        List<DBRow> questions = data.retrieveQuestions();
        assertThat(questions, hasSize(1));

        List<DBRow> questionConcepts = data.retrieveLinks();
        assertThat(questionConcepts, hasSize(2));

        data.deleteQuestion(questionId);

        List<DBRow> questionsAfterDelete = data.retrieveQuestions();
        assertThat(questionsAfterDelete, empty());

        List<DBRow> questionConceptsAfterDelete = data.retrieveLinks();
        assertThat(questionConceptsAfterDelete, empty());
    }

    @Test
    void ensureThatBodyMustBeSet() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.question().source(QuestionSource.SEED).insert());

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"body\""));
    }

    @Test
    void ensureThatSourceMustBeSet() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.question().body("A question.").insert()
        );

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"source\""));
    }

    @Test
    void ensureThatDifficultyMustBeWithinRange() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.question()
                        .body("A question.")
                        .source(QuestionSource.SEED)
                        .difficulty(6)
                        .insert()
        );

        assertThat(sqlStateOf(thrown), equalTo(CHECK_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("difficulty"));
    }
}
