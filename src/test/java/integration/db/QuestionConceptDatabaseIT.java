package integration.db;

import utils.IntegrationTest;
import utils.data.DBRow;
import utils.data.QuestionTestData;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static utils.data.TestDatabase.FOREIGN_KEY_VIOLATION;
import static utils.data.TestDatabase.UNIQUE_VIOLATION;
import static utils.data.TestDatabase.sqlStateOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@IntegrationTest
class QuestionConceptDatabaseIT {

    @Inject
    private QuestionTestData data;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureAQuestionCannotBeLinkedToTheSameConceptTwice() {
        long questionId = 1L;
        data.question(questionId).insert();

        long conceptId = 10L;
        data.concept(conceptId).insert();
        data.link(questionId, conceptId).insert();

        List<DBRow> questionConcepts = data.retrieveLinks();
        assertThat(questionConcepts, hasSize(1));
        DBRow questionConcept = questionConcepts.getFirst();
        questionConcept.assertThat("question_id", equalTo(questionId));
        questionConcept.assertThat("concept_id", equalTo(conceptId));

        // The composite (questionId, conceptId) primary key rejects a duplicate link.
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.link(questionId, conceptId).insert());

        assertThat(sqlStateOf(thrown), equalTo(UNIQUE_VIOLATION));
    }

    @Test
    void ensureALinkRequiresAnExistingQuestion() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.link(999L, conceptId).insert());

        assertThat(sqlStateOf(thrown), equalTo(FOREIGN_KEY_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("question_id"));
    }

    @Test
    void ensureALinkRequiresAnExistingConcept() {
        long questionId = 1L;
        data.question(questionId).insert();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.link(questionId, 999L).insert());

        assertThat(sqlStateOf(thrown), equalTo(FOREIGN_KEY_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("concept_id"));
    }
}
