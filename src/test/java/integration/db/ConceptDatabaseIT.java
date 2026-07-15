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
public class ConceptDatabaseIT {

    @Inject
    private QuestionTestData data;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureConceptCreatedWithDefaultFields() {
        String name = "Diffraction";
        String description = "The spreading of waves through a gap or around an obstacle.";

        data.concept().name(name).description(description).insert();

        List<DBRow> concepts = data.retrieveConcepts();
        DBRow concept = concepts.getFirst();
        concept.assertThat("id", greaterThan(0L));
        concept.assertThat("version", equalTo(0));
        concept.assertThat("name", equalTo(name));
        concept.assertThat("description", equalTo(description));
        concept.assertThat("created_at", allOf(
                greaterThan(Instant.EPOCH),
                lessThanOrEqualTo(Instant.now())
        ));
        concept.assertAllColumnsChecked();
    }

    @Test
    void ensureConceptCanBeCreatedWithQuestionConceptLinks() {
        long conceptId = 1L;
        data.concept(conceptId).insert();

        long questionId1 = 10L;
        data.question(questionId1).insert();
        data.link(questionId1, conceptId).insert();

        long questionId2 = 20L;
        data.question(questionId2).insert();
        data.link(questionId2, conceptId).insert();

        List<DBRow> questionConcepts = data.retrieveLinks();
        assertThat(questionConcepts, hasSize(2));
        questionConcepts.forEach(c -> c.assertThat("concept_id", equalTo(conceptId)));

        List<Long> linkedConceptIds = questionConcepts.stream()
                .map(c -> c.<Long>get("question_id"))
                .toList();
        assertThat(linkedConceptIds, containsInAnyOrder(questionId1, questionId2));
    }

    @Test
    void ensureQuestionConceptsDeletedWhenConceptsDeleted() {
        long conceptId = 1L;
        data.concept(conceptId).insert();

        long questionId1 = 10L;
        data.question(questionId1).insert();
        data.link(questionId1, conceptId).insert();

        long questionId2 = 20L;
        data.question(questionId2).insert();
        data.link(questionId2, conceptId).insert();

        List<DBRow> concepts = data.retrieveConcepts();
        assertThat(concepts, hasSize(1));

        List<DBRow> questionConcepts = data.retrieveLinks();
        assertThat(questionConcepts, hasSize(2));

        data.deleteConcept(conceptId);

        List<DBRow> conceptsAfterDelete = data.retrieveConcepts();
        assertThat(conceptsAfterDelete, empty());

        List<DBRow> questionConceptsAfterDelete = data.retrieveLinks();
        assertThat(questionConceptsAfterDelete, empty());
    }

    @Test
    void ensureThatNameMustBeSet() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.concept().description("Test").insert()
        );

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"name\""));
    }

    @Test
    void ensureThatDescriptionMustBeSet() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.concept().name("Test").insert()
        );

        assertThat(sqlStateOf(thrown), equalTo(NOT_NULL_VIOLATION));
        assertThat(thrown.getCause().getMessage(), containsString("\"description\""));
    }

    @Test
    void ensureThatNameMustBeUnique() {
        String name = "Test";
        data.concept().name(name).description("description").insert();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                data.concept().name(name).description("description").insert()
        );

        // SQLState alone is conclusive here: name is the only unique constraint on the table. (Unlike the
        // not-null messages, a unique-violation message names the constraint, not a quoted column.)
        assertThat(sqlStateOf(thrown), equalTo(UNIQUE_VIOLATION));
    }
}
