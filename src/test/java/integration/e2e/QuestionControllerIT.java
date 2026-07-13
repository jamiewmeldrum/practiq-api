package integration.e2e;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import utils.IntegrationTest;
import utils.data.QuestionTestData;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

// End-to-end over real Postgres. Two rules shape every expectation here: only APPROVED questions are
// served, and a question with no concept link is unprocessed and never served — so every question that
// should appear is linked to a concept, and an unlinked one is used only to prove exclusion.
// The response carries exactly six fields; source/status/source_spec are deliberately not exposed.
@IntegrationTest
class QuestionControllerIT {

    private static final String QUESTIONS_PATH = "/api/v1/questions";
    private static final long CONCEPT_ID = 100L;

    @Inject
    private QuestionTestData data;

    @Inject
    private EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        data.clear();
        data.concept(CONCEPT_ID).insert();
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void getReturnsEmptyArrayWhenNoQuestionsExist() {
        given()
                .when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("content", "page", "size", "totalCount"))
                .body("content", empty())
                .body("totalCount", equalTo(0));
    }

    @Test
    void getReturnsOnlyApprovedQuestionsWithConceptLinks() {
        String approvedBody = "State Newton's first law.";
        long approvedId = 1L;
        data.question(approvedId)
                .status(QuestionStatus.APPROVED)
                .body(approvedBody)
                .source(QuestionSource.SEED)
                .insert();
        data.link(approvedId, CONCEPT_ID).insert();

        data.question(2L)
                .status(QuestionStatus.PENDING)
                .body("A pending question.")
                .source(QuestionSource.SEED)
                .insert();
        data.link(2L, CONCEPT_ID).insert();

        data.question(3L)
                .status(QuestionStatus.APPROVED)
                .body("An approved question without a link.")
                .source(QuestionSource.SEED)
                .insert();

        given()
                .when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("page", equalTo(0))
                .body("size", equalTo(10))
                .body("totalCount", equalTo(1))
                .body("content.size()", equalTo(1))
                .body("content[0].body", equalTo(approvedBody));
    }

    @Test
    void returnsOnlyQuestionsMatchingAllFilters() {
        String matchingBody1 = "State Newton's first law.";
        String matchingBody2 = "State Newton's second law.";
        String matchingBody3 = "State Newton's third law.";

        /*
         * MATCHING CRITERIA
         * Status = APPROVED
         * Concept = CONCEPT_ID
         * Difficult = MEDIUM, HARD
         * Type = SHORT_ANSWER, MCQ
         */
        // Matches all, difficulty MEDIUM, type SHORT_ANSWER
        data.question(1L)
                .status(QuestionStatus.APPROVED)
                .body(matchingBody1)
                .source(QuestionSource.SEED)
                .type(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.MEDIUM)
                .insert();
        data.link(1L, CONCEPT_ID).insert();

        //Matches all, difficulty HARD, type SHORT_ANSWER. Source changed as irrelevant
        data.question(2L)
                .status(QuestionStatus.APPROVED)
                .body(matchingBody2)
                .source(QuestionSource.EXTRACTED)
                .type(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.HARD)
                .insert();
        data.link(2L, CONCEPT_ID).insert();

        //Matches all, difficulty MEDIUM, type MCQ. Source changed as irrelevant
        data.question(3L)
                .status(QuestionStatus.APPROVED)
                .body(matchingBody3)
                .source(QuestionSource.GENERATED)
                .type(QuestionType.MCQ)
                .difficulty(QuestionDifficulty.MEDIUM)
                .insert();
        data.link(3L, CONCEPT_ID).insert();

        // Wrong type
        data.question(4L)
                .status(QuestionStatus.APPROVED)
                .body("Wrong type")
                .source(QuestionSource.SEED)
                .type(QuestionType.EXTENDED)
                .difficulty(QuestionDifficulty.MEDIUM)
                .insert();
        data.link(4L, CONCEPT_ID).insert();

        // Wrong difficulty
        data.question(5L)
                .status(QuestionStatus.APPROVED)
                .body("Wrong difficulty")
                .source(QuestionSource.SEED)
                .type(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.VERY_HARD)
                .insert();
        data.link(5L, CONCEPT_ID).insert();

        // Wrong concept
        long additionalConceptId = CONCEPT_ID + 1;
        data.concept(additionalConceptId).insert();
        data.question(6L)
                .status(QuestionStatus.APPROVED)
                .body("Wrong concept")
                .source(QuestionSource.SEED)
                .type(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.HARD)
                .insert();
        data.link(6L, additionalConceptId).insert();

        given()
                .when()
                .get(QUESTIONS_PATH + "?types=SHORT_ANSWER,MCQ&difficulties=3,4&conceptId=" + CONCEPT_ID)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("page", equalTo(0))
                .body("size", equalTo(10))
                .body("totalCount", equalTo(3))
                .body("content.size()", equalTo(3))
                .body("content.body", containsInAnyOrder(matchingBody1, matchingBody2, matchingBody3))
                .body("content.type", containsInAnyOrder(
                        QuestionType.SHORT_ANSWER.name(),
                        QuestionType.SHORT_ANSWER.name(),
                        QuestionType.MCQ.name()))
                .body("content.difficulty", containsInAnyOrder(
                        difficultyResponse(QuestionDifficulty.MEDIUM),
                        difficultyResponse(QuestionDifficulty.MEDIUM),
                        difficultyResponse(QuestionDifficulty.HARD)))
                .body("content.linkedConceptIds", everyItem(contains((int) CONCEPT_ID)));
    }

    // The difficulty serialises as a {value, code} object, which RestAssured surfaces as a Map — so an
    // expected value is built in that shape. A QuestionDifficultyResponse would never equal a Map (and has
    // no equals anyway), which is what made the whole-object comparison silently unmatchable.
    private static Map<String, Object> difficultyResponse(QuestionDifficulty difficulty) {
        return Map.of("value", difficulty.value(), "code", difficulty.name());
    }

    @Test
    void getPagesInStableCreatedAtThenIdOrderAcrossMultiplePages() {
        OffsetDateTime day1 = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime day2 = OffsetDateTime.parse("2026-01-02T00:00:00Z");
        OffsetDateTime day3 = OffsetDateTime.parse("2026-01-03T00:00:00Z");

        // created_at leads; id breaks ties within an equal timestamp. earliest has the highest id but the
        // earliest time, so it still sorts first — and the day2/day3 pairs prove the id tiebreak. Full
        // order: [5, 1, 2, 3, 4].
        approvedLinkedQuestion(5L, "Earliest by time.", day1);
        approvedLinkedQuestion(1L, "Day two, lower id.", day2);
        approvedLinkedQuestion(2L, "Day two, higher id.", day2);
        approvedLinkedQuestion(3L, "Day three, lower id.", day3);
        approvedLinkedQuestion(4L, "Day three, higher id.", day3);

        // Walk all three pages at size 2. Each is a contiguous, non-overlapping slice of the one total
        // order — including the last, partial page at index 2 — so a row can't straddle, repeat or vanish.
        given().when().get(QUESTIONS_PATH + "?page=0&size=2")
                .then().statusCode(OK.getCode()).contentType(ContentType.JSON)
                .body("content.size()", equalTo(2)).body("content.id", contains(5, 1))
                .body("page", equalTo(0)).body("size", equalTo(2)).body("totalCount", equalTo(5));

        given().when().get(QUESTIONS_PATH + "?page=1&size=2")
                .then().statusCode(OK.getCode()).contentType(ContentType.JSON)
                .body("content.size()", equalTo(2)).body("content.id", contains(2, 3))
                .body("page", equalTo(1)).body("size", equalTo(2)).body("totalCount", equalTo(5));

        given().when().get(QUESTIONS_PATH + "?page=2&size=2")
                .then().statusCode(OK.getCode()).contentType(ContentType.JSON)
                .body("content.size()", equalTo(1)).body("content.id", contains(4))
                .body("page", equalTo(2)).body("size", equalTo(2)).body("totalCount", equalTo(5));
    }

    @Test
    void getReturnsTheSameOrderedResultOnRepeatedCalls() {
        OffsetDateTime sameInstant = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        // Equal created_at across all three, so the order is decided purely by the id tiebreak. Repeated
        // calls must reproduce it identically rather than return an arbitrary (DB-dependent) order.
        approvedLinkedQuestion(1L, "One.", sameInstant);
        approvedLinkedQuestion(2L, "Two.", sameInstant);
        approvedLinkedQuestion(3L, "Three.", sameInstant);

        for (int call = 0; call < 2; call++) {
            given()
                    .when()
                    .get(QUESTIONS_PATH)
                    .then()
                    .statusCode(OK.getCode())
                    .contentType(ContentType.JSON)
                    .body("content.id", contains(1, 2, 3));
        }
    }

    @Test
    void getQuestionByIdReturns404IfNotFound() {
        String path = QUESTIONS_PATH + "/123";
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getQuestionByIdReturnsNotFoundIfNoApprovedQuestionExistsForId() {
        long targetId = 1L;
        data.question(targetId)
                .status(QuestionStatus.REJECTED)
                .body("State Newton's first law.")
                .source(QuestionSource.SEED)
                .insert();
        data.link(targetId, CONCEPT_ID).insert();

        String path = QUESTIONS_PATH + "/" + targetId;
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getQuestionByIdReturnsNotFoundIfNoLinkExistsForId() {
        long targetId = 1L;
        data.question(targetId)
                .status(QuestionStatus.APPROVED)
                .body("State Newton's first law.")
                .source(QuestionSource.SEED)
                .insert();

        String path = QUESTIONS_PATH + "/" + targetId;
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getQuestionByIdReturnsQuestionIfApprovedAndLinkedQuestionExistsForId() {
        String approvedBody = "State Newton's first law.";
        long approvedId = 1L;
        data.question(approvedId)
                .status(QuestionStatus.APPROVED)
                .body(approvedBody)
                .source(QuestionSource.SEED)
                .insert();
        data.link(approvedId, CONCEPT_ID).insert();

        given()
                .when()
                .get(QUESTIONS_PATH + "/" + approvedId)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("body", equalTo(approvedBody));
    }

    private void approvedLinkedQuestion(long id, String body, OffsetDateTime createdAt) {
        data.question(id).status(QuestionStatus.APPROVED)
                .body(body)
                .source(QuestionSource.SEED)
                .createdAt(createdAt)
                .insert();
        data.link(id, CONCEPT_ID).insert();
    }
}
