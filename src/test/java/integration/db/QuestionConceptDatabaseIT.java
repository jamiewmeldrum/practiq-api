package integration.db;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.DBRow;
import utils.data.QuestionTestData;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.data.DBRow.hasColumn;
import static utils.data.TestDatabase.*;

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

    @Test
    void ensureQuestionConceptsDeletedWhenQuestionsDeleted() {
        //Data linked to question that will be deleted
        long questionId = 1L;
        data.question(questionId).insert();

        long conceptId1 = 10L;
        data.concept(conceptId1).insert();
        data.link(questionId, conceptId1).insert();

        long conceptId2 = 20L;
        data.concept(conceptId2).insert();
        data.link(questionId, conceptId2).insert();

        //Data linked to question that will not be deleted
        long otherQuestionId = 2L;
        data.question(otherQuestionId).insert();

        long otherConceptId = 30L;
        data.concept(otherConceptId).insert();
        data.link(otherQuestionId, otherConceptId).insert();

        //Assert before
        List<DBRow> questions = data.retrieveQuestions();
        assertThat(questions, hasSize(2));
        assertThat(DBRow.collectColumn(questions, "id"), contains(questionId, otherQuestionId));

        List<DBRow> concepts = data.retrieveConcepts();
        assertThat(concepts, hasSize(3));
        assertThat(DBRow.collectColumn(concepts, "id"), contains(conceptId1, conceptId2, otherConceptId));

        List<DBRow> questionConcepts = data.retrieveLinks();
        assertThat(questionConcepts, hasSize(3));
        assertThat(questionConcepts, contains(
                allOf(
                        hasColumn("question_id", equalTo(questionId)),
                        hasColumn("concept_id", equalTo(conceptId1))
                ),
                allOf(
                        hasColumn("question_id", equalTo(questionId)),
                        hasColumn("concept_id", equalTo(conceptId2))
                ),
                allOf(
                        hasColumn("question_id", equalTo(otherQuestionId)),
                        hasColumn("concept_id", equalTo(otherConceptId))
                )
        ));

        data.deleteQuestion(questionId);

        //Assert after
        List<DBRow> questionsAfterDelete = data.retrieveQuestions();
        assertThat(questionsAfterDelete, hasSize(1));
        assertThat(DBRow.collectColumn(questionsAfterDelete, "id"), contains(otherQuestionId));

        List<DBRow> conceptsAfterDelete = data.retrieveConcepts();
        assertThat(conceptsAfterDelete, hasSize(3));
        assertThat(DBRow.collectColumn(conceptsAfterDelete, "id"), contains(conceptId1, conceptId2, otherConceptId));

        List<DBRow> questionConceptsAfterDelete = data.retrieveLinks();
        assertThat(questionConceptsAfterDelete, hasSize(1));
        assertThat(questionConceptsAfterDelete, contains(
                allOf(
                        hasColumn("question_id", equalTo(otherQuestionId)),
                        hasColumn("concept_id", equalTo(otherConceptId))
                )
        ));
    }

    @Test
    void ensureQuestionConceptsDeletedWhenConceptsDeleted() {
        //Data linked to concept that will be deleted
        long questionId1 = 1L;
        data.question(questionId1).insert();

        long questionId2 = 2L;
        data.question(questionId2).insert();

        long conceptId = 10L;
        data.concept(conceptId).insert();
        data.link(questionId1, conceptId).insert();
        data.link(questionId2, conceptId).insert();

        //Data linked to question that will not be deleted
        long questionId3 = 3L;
        data.question(questionId3).insert();

        long otherConceptId = 20L;
        data.concept(otherConceptId).insert();
        data.link(questionId3, otherConceptId).insert();

        //Assert before
        List<DBRow> questions = data.retrieveQuestions();
        assertThat(questions, hasSize(3));
        assertThat(DBRow.collectColumn(questions, "id"), contains(questionId1, questionId2, questionId3));

        List<DBRow> concepts = data.retrieveConcepts();
        assertThat(concepts, hasSize(2));
        assertThat(DBRow.collectColumn(concepts, "id"), contains(conceptId, otherConceptId));

        List<DBRow> questionConcepts = data.retrieveLinks();
        assertThat(questionConcepts, hasSize(3));
        assertThat(questionConcepts, contains(
                allOf(
                        hasColumn("question_id", equalTo(questionId1)),
                        hasColumn("concept_id", equalTo(conceptId))
                ),
                allOf(
                        hasColumn("question_id", equalTo(questionId2)),
                        hasColumn("concept_id", equalTo(conceptId))
                ),
                allOf(
                        hasColumn("question_id", equalTo(questionId3)),
                        hasColumn("concept_id", equalTo(otherConceptId))
                )
        ));

        data.deleteConcept(conceptId);

        //Assert after
        List<DBRow> questionsAfterDelete = data.retrieveQuestions();
        assertThat(questionsAfterDelete, hasSize(3));
        assertThat(DBRow.collectColumn(questionsAfterDelete, "id"), contains(questionId1, questionId2, questionId3));

        List<DBRow> conceptsAfterDelete = data.retrieveConcepts();
        assertThat(conceptsAfterDelete, hasSize(1));
        assertThat(DBRow.collectColumn(conceptsAfterDelete, "id"), contains(otherConceptId));

        List<DBRow> questionConceptsAfterDelete = data.retrieveLinks();
        assertThat(questionConceptsAfterDelete, hasSize(1));
        assertThat(questionConceptsAfterDelete, contains(
                allOf(
                        hasColumn("question_id", equalTo(questionId3)),
                        hasColumn("concept_id", equalTo(otherConceptId))
                )
        ));

    }
}
