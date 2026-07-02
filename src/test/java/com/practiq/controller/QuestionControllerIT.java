package com.practiq.controller;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.test.TestDatabase;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@MicronautTest(transactional = false)
class QuestionControllerIT {

    private static final String QUESTION_TABLE = "question";
    private static final String QUESTION_CONCEPT_TABLE = "question_concept";
    private static final String CONCEPT_TABLE = "concept";

    private static final String QUESTIONS_PATH = "/api/v1/questions";

    private static final String CREATED_AT_PATTERN = "\\d{4}-\\d{2}-\\d{2}T.*Z";

    @Inject
    private TestDatabase testDatabase;

    @Inject
    private EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        // Truncate the link table first so its rows go before the questions/concepts they reference.
        testDatabase.clear(QUESTION_CONCEPT_TABLE);
        testDatabase.clear(QUESTION_TABLE);
        testDatabase.clear(CONCEPT_TABLE);
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void getQuestionsReturnsEmptyArrayWhenNoneExist() {
        given()
                .when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("$", empty());
    }

    @Test
    void getQuestionsReturnsNullableFieldsAsNull() {
        String bodyOne = "State Newton's first law.";
        String bodyTwo = "Define displacement.";
        String bodyThree = "What is a longitudinal wave?";
        // source is the only NOT NULL enum column with no default, so it must be set; everything
        // else nullable (difficulty, type, source_spec) is omitted so the DB leaves it NULL, and
        // status is omitted so the DB default (PENDING) applies.
        QuestionSource source = QuestionSource.SEED;

        question(1L, bodyOne, source).insert();
        question(2L, bodyTwo, source).insert();
        question(3L, bodyThree, source).insert();

        given()
                .when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("size()", equalTo(3))
                .body("[0].keySet()", containsInAnyOrder(
                        "id", "body", "difficulty", "type", "source", "status", "sourceSpec", "createdAt", "linkedConceptIds"))
                .body("body", containsInAnyOrder(bodyOne, bodyTwo, bodyThree))
                .body("id", everyItem(greaterThan(0)))
                .body("source", everyItem(equalTo(source.name())))
                .body("status", everyItem(equalTo(QuestionStatus.PENDING.name())))
                .body("createdAt", everyItem(matchesPattern(CREATED_AT_PATTERN)))
                .body("difficulty", everyItem(nullValue()))
                .body("type", everyItem(nullValue()))
                .body("sourceSpec", everyItem(nullValue()))
                .body("linkedConceptIds", everyItem(empty()));
    }

    @Test
    void getQuestionsReturnsPopulatedFieldsWithConceptLinks() {
        long diffractionId = 10L;
        long accelerationId = 11L;
        insertConcept(diffractionId, "Diffraction", "The spreading of waves through a gap or around an obstacle.");
        insertConcept(accelerationId, "Acceleration", "How the velocity of an object changes over time.");

        // Unlinked question.
        long unlinkedId = 1L;
        String unlinkedBody = "Explain what is meant by diffraction.";
        QuestionDifficulty unlinkedDifficulty = QuestionDifficulty.MEDIUM;
        QuestionType unlinkedType = QuestionType.EXTENDED;
        QuestionSource unlinkedSource = QuestionSource.SEED;
        QuestionStatus unlinkedStatus = QuestionStatus.APPROVED;
        String unlinkedSourceSpec = "AQA GCSE Physics";
        question(unlinkedId, unlinkedBody, unlinkedSource)
                .difficulty(unlinkedDifficulty)
                .type(unlinkedType)
                .status(unlinkedStatus)
                .sourceSpec(unlinkedSourceSpec)
                .insert();

        // Question linked to a single concept.
        long singleLinkId = 2L;
        String singleLinkBody = "Calculate the acceleration of a car.";
        QuestionDifficulty singleLinkDifficulty = QuestionDifficulty.EASY;
        QuestionType singleLinkType = QuestionType.SHORT_ANSWER;
        QuestionSource singleLinkSource = QuestionSource.EXTRACTED;
        QuestionStatus singleLinkStatus = QuestionStatus.APPROVED;
        String singleLinkSourceSpec = "AQA GCSE Physics";
        question(singleLinkId, singleLinkBody, singleLinkSource)
                .difficulty(singleLinkDifficulty)
                .type(singleLinkType)
                .status(singleLinkStatus)
                .sourceSpec(singleLinkSourceSpec)
                .insert();
        linkConcept(singleLinkId, accelerationId);

        // Synoptic question linked to two concepts.
        long doubleLinkId = 3L;
        String doubleLinkBody = "A wave passes through a gap and its speed changes. Discuss.";
        QuestionDifficulty doubleLinkDifficulty = QuestionDifficulty.VERY_HARD;
        QuestionType doubleLinkType = QuestionType.MCQ;
        QuestionSource doubleLinkSource = QuestionSource.GENERATED;
        QuestionStatus doubleLinkStatus = QuestionStatus.PENDING;
        String doubleLinkSourceSpec = "OCR A-Level Physics";
        question(doubleLinkId, doubleLinkBody, doubleLinkSource)
                .difficulty(doubleLinkDifficulty)
                .type(doubleLinkType)
                .status(doubleLinkStatus)
                .sourceSpec(doubleLinkSourceSpec)
                .insert();
        linkConcept(doubleLinkId, diffractionId);
        linkConcept(doubleLinkId, accelerationId);

        given()
                .when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("size()", equalTo(3))
                .body("id", containsInAnyOrder((int) unlinkedId, (int) singleLinkId, (int) doubleLinkId))
                .body("[0].keySet()", containsInAnyOrder(
                        "id", "body", "difficulty", "type", "source", "status", "sourceSpec", "createdAt", "linkedConceptIds"))

                .body("find { it.id == " + unlinkedId + " }.body", equalTo(unlinkedBody))
                .body("find { it.id == " + unlinkedId + " }.difficulty.value", equalTo(unlinkedDifficulty.value()))
                .body("find { it.id == " + unlinkedId + " }.difficulty.code", equalTo(unlinkedDifficulty.name()))
                .body("find { it.id == " + unlinkedId + " }.type", equalTo(unlinkedType.name()))
                .body("find { it.id == " + unlinkedId + " }.source", equalTo(unlinkedSource.name()))
                .body("find { it.id == " + unlinkedId + " }.status", equalTo(unlinkedStatus.name()))
                .body("find { it.id == " + unlinkedId + " }.sourceSpec", equalTo(unlinkedSourceSpec))
                .body("find { it.id == " + unlinkedId + " }.createdAt", matchesPattern(CREATED_AT_PATTERN))
                .body("find { it.id == " + unlinkedId + " }.linkedConceptIds", empty())

                .body("find { it.id == " + singleLinkId + " }.body", equalTo(singleLinkBody))
                .body("find { it.id == " + singleLinkId + " }.difficulty.value", equalTo(singleLinkDifficulty.value()))
                .body("find { it.id == " + singleLinkId + " }.difficulty.code", equalTo(singleLinkDifficulty.name()))
                .body("find { it.id == " + singleLinkId + " }.type", equalTo(singleLinkType.name()))
                .body("find { it.id == " + singleLinkId + " }.source", equalTo(singleLinkSource.name()))
                .body("find { it.id == " + singleLinkId + " }.status", equalTo(singleLinkStatus.name()))
                .body("find { it.id == " + singleLinkId + " }.sourceSpec", equalTo(singleLinkSourceSpec))
                .body("find { it.id == " + singleLinkId + " }.createdAt", matchesPattern(CREATED_AT_PATTERN))
                .body("find { it.id == " + singleLinkId + " }.linkedConceptIds", containsInAnyOrder((int) accelerationId))

                .body("find { it.id == " + doubleLinkId + " }.body", equalTo(doubleLinkBody))
                .body("find { it.id == " + doubleLinkId + " }.difficulty.value", equalTo(doubleLinkDifficulty.value()))
                .body("find { it.id == " + doubleLinkId + " }.difficulty.code", equalTo(doubleLinkDifficulty.name()))
                .body("find { it.id == " + doubleLinkId + " }.type", equalTo(doubleLinkType.name()))
                .body("find { it.id == " + doubleLinkId + " }.source", equalTo(doubleLinkSource.name()))
                .body("find { it.id == " + doubleLinkId + " }.status", equalTo(doubleLinkStatus.name()))
                .body("find { it.id == " + doubleLinkId + " }.sourceSpec", equalTo(doubleLinkSourceSpec))
                .body("find { it.id == " + doubleLinkId + " }.createdAt", matchesPattern(CREATED_AT_PATTERN))
                .body("find { it.id == " + doubleLinkId + " }.linkedConceptIds",
                        containsInAnyOrder((int) diffractionId, (int) accelerationId));
    }

    private void insertConcept(long id, String name, String description) {
        testDatabase.insert(
                CONCEPT_TABLE,
                Map.of(
                        "id", id,
                        "name", name,
                        "description", description
                )
        );
    }

    // Starts a question row. id, body and source are required (source is NOT NULL with no default);
    // the optional columns are added only when their setter is called, so an unset field means the
    // column is omitted and the DB applies NULL or its default — no nulls at the call site.
    private QuestionRow question(long id, String body, QuestionSource source) {
        return new QuestionRow(id, body, source);
    }

    // Accumulates the columns for one question row, then writes it. Enum-backed columns are stored
    // as the constant name (upper-case, per project convention); difficulty is stored as its integer
    // value, matching the app's QuestionDifficultyAttributeConverter.
    private final class QuestionRow {
        private final Map<String, Object> columns = new HashMap<>();

        private QuestionRow(long id, String body, QuestionSource source) {
            columns.put("id", id);
            columns.put("body", body);
            columns.put("source", source.name());
        }

        private QuestionRow difficulty(QuestionDifficulty difficulty) {
            columns.put("difficulty", difficulty.value());
            return this;
        }

        private QuestionRow type(QuestionType type) {
            columns.put("type", type.name());
            return this;
        }

        private QuestionRow status(QuestionStatus status) {
            columns.put("status", status.name());
            return this;
        }

        private QuestionRow sourceSpec(String sourceSpec) {
            columns.put("source_spec", sourceSpec);
            return this;
        }

        private void insert() {
            testDatabase.insert(QUESTION_TABLE, columns);
        }
    }

    private void linkConcept(long questionId, long conceptId) {
        testDatabase.insert(
                QUESTION_CONCEPT_TABLE,
                Map.of(
                        "question_id", questionId,
                        "concept_id", conceptId
                )
        );
    }
}
