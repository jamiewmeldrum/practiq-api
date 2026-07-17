package integration.e2e;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import java.time.OffsetDateTime;
import java.util.Map;

import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@IntegrationTest
class QuestionControllerIT {

    private static final String QUESTIONS_PATH = "/api/v1/questions";

    @Inject
    private QuestionTestData data;

    @Inject
    private EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        data.clear();
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
                .body("content", empty())
                .body("totalCount", equalTo(0));
    }

    @Test
    void getReturnsOnlyApprovedQuestionsWithConceptLinks() {
        long conceptId = 100L;
        data.concept(conceptId).insert();

        String approvedBody = "State Newton's first law.";
        long approvedId = 1L;
        data.question(approvedId)
                .status(QuestionStatus.APPROVED)
                .body(approvedBody)
                .source(QuestionSource.SEED)
                .insert();
        data.link(approvedId, conceptId).insert();

        data.question(2L)
                .status(QuestionStatus.PENDING)
                .body("A pending question.")
                .source(QuestionSource.SEED)
                .insert();
        data.link(2L, conceptId).insert();

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
        long conceptId = 100L;
        data.concept(conceptId).insert();

        String matchingBody1 = "State Newton's first law.";
        String matchingBody2 = "State Newton's second law.";
        String matchingBody3 = "State Newton's third law.";

        /*
         * MATCHING CRITERIA
         * Status = APPROVED
         * Concept = conceptId
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
        data.link(1L, conceptId).insert();

        //Matches all, difficulty HARD, type SHORT_ANSWER. Source changed as irrelevant
        data.question(2L)
                .status(QuestionStatus.APPROVED)
                .body(matchingBody2)
                .source(QuestionSource.EXTRACTED)
                .type(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.HARD)
                .insert();
        data.link(2L, conceptId).insert();

        //Matches all, difficulty MEDIUM, type MCQ. Source changed as irrelevant
        data.question(3L)
                .status(QuestionStatus.APPROVED)
                .body(matchingBody3)
                .source(QuestionSource.GENERATED)
                .type(QuestionType.MCQ)
                .difficulty(QuestionDifficulty.MEDIUM)
                .insert();
        data.link(3L, conceptId).insert();

        // Wrong type
        data.question(4L)
                .status(QuestionStatus.APPROVED)
                .body("Wrong type")
                .source(QuestionSource.SEED)
                .type(QuestionType.EXTENDED)
                .difficulty(QuestionDifficulty.MEDIUM)
                .insert();
        data.link(4L, conceptId).insert();

        // Wrong difficulty
        data.question(5L)
                .status(QuestionStatus.APPROVED)
                .body("Wrong difficulty")
                .source(QuestionSource.SEED)
                .type(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.VERY_HARD)
                .insert();
        data.link(5L, conceptId).insert();

        // Wrong concept
        long additionalConceptId = conceptId + 1;
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
                .get(QUESTIONS_PATH + "?types=SHORT_ANSWER,MCQ&difficulties=3,4&conceptId=" + conceptId)
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
                .body("content.linkedConceptIds", everyItem(contains((int) conceptId)));
    }

    // The difficulty serialises as a {value, code} object, which RestAssured surfaces as a Map — so an
    // expected value is built in that shape. A QuestionDifficultyResponse would never equal a Map (and has
    // no equals anyway), which is what made the whole-object comparison silently unmatchable.
    private static Map<String, Object> difficultyResponse(QuestionDifficulty difficulty) {
        return Map.of("value", difficulty.value(), "code", difficulty.name());
    }

    @Test
    void getPagesInStableCreatedAtThenIdOrderAcrossMultiplePages() {
        long conceptId = 100L;
        data.concept(conceptId).insert();

        OffsetDateTime day1 = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime day2 = OffsetDateTime.parse("2026-01-02T00:00:00Z");
        OffsetDateTime day3 = OffsetDateTime.parse("2026-01-03T00:00:00Z");

        // created_at leads; id breaks ties within an equal timestamp. earliest has the highest id but the
        // earliest time, so it still sorts first — and the day2/day3 pairs prove the id tiebreak. Full
        // order: [5, 1, 2, 3, 4].
        approvedLinkedQuestion(5L, "Earliest by time.", day1, conceptId);
        approvedLinkedQuestion(1L, "Day two, lower id.", day2, conceptId);
        approvedLinkedQuestion(2L, "Day two, higher id.", day2, conceptId);
        approvedLinkedQuestion(3L, "Day three, lower id.", day3, conceptId);
        approvedLinkedQuestion(4L, "Day three, higher id.", day3, conceptId);

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
        long conceptId = 100L;
        data.concept(conceptId).insert();

        OffsetDateTime sameInstant = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        // Equal created_at across all three, so the order is decided purely by the id tiebreak. Repeated
        // calls must reproduce it identically rather than return an arbitrary (DB-dependent) order.
        approvedLinkedQuestion(1L, "One.", sameInstant, conceptId);
        approvedLinkedQuestion(2L, "Two.", sameInstant, conceptId);
        approvedLinkedQuestion(3L, "Three.", sameInstant, conceptId);

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

    // Each by-id test below carries a second, fully-servable question. Without it a handler returning *a*
    // question rather than *the* one asked for would pass.
    @Test
    void getQuestionByIdReturnsNotFoundIfNoQuestionExistsForId() {
        long conceptId = 100L;
        data.concept(conceptId).insert();
        approvedLinkedQuestion(7L, "Servable question seven.", conceptId);
        approvedLinkedQuestion(8L, "Servable question eight.", conceptId);

        // 9 is neither of the two rows present.
        String path = QUESTIONS_PATH + "/" + 9L;
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getQuestionByIdReturnsNotFoundIfNoApprovedQuestionExistsForId() {
        long conceptId = 100L;
        data.concept(conceptId).insert();

        long targetId = 7L;
        data.question(targetId)
                .status(QuestionStatus.REJECTED)
                .insert();
        data.link(targetId, conceptId).insert();

        approvedLinkedQuestion(8L, "Servable question eight.", conceptId);

        String path = QUESTIONS_PATH + "/" + targetId;
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getQuestionByIdReturnsNotFoundIfNoLinkExistsForId() {
        // A concept exists but is never linked to the target, so the missing link is the only reason it
        // isn't served — the second question is linked and is.
        long conceptId = 100L;
        data.concept(conceptId).insert();

        long targetId = 7L;
        data.question(targetId)
                .status(QuestionStatus.APPROVED)
                .insert();

        approvedLinkedQuestion(8L, "Servable question eight.", conceptId);

        String path = QUESTIONS_PATH + "/" + targetId;
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));
    }

    @Test
    void getQuestionByIdReturnsQuestionIfApprovedAndLinkedQuestionExistsForId() {
        long conceptId = 100L;
        data.concept(conceptId).insert();

        long id = 7L;
        String body = "State Newton's first law.";
        QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;
        QuestionType type = QuestionType.SHORT_ANSWER;
        data.question(id)
                .status(QuestionStatus.APPROVED)
                .difficulty(difficulty)
                .body(body)
                .type(type)
                .insert();
        data.link(id, conceptId).insert();

        approvedLinkedQuestion(8L, "Servable question eight.", conceptId);

        given()
                .when()
                .get(QUESTIONS_PATH + "/" + id)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) id))
                .body("body", equalTo(body))
                .body("difficulty.value", equalTo(difficulty.value()))
                .body("difficulty.code", equalTo(difficulty.name()))
                .body("type", equalTo(type.name()))
                .body("createdAt", matchesPattern(data.getInstantPattern()))
                .body("linkedConceptIds", contains((int) conceptId));
    }

    private void approvedLinkedQuestion(long id, String body, long conceptId) {
        data.question(id).status(QuestionStatus.APPROVED)
                .body(body)
                .insert();
        data.link(id, conceptId).insert();
    }

    private void approvedLinkedQuestion(long id, String body, OffsetDateTime createdAt, long conceptId) {
        data.question(id).status(QuestionStatus.APPROVED)
                .body(body)
                .source(QuestionSource.SEED)
                .createdAt(createdAt)
                .insert();
        data.link(id, conceptId).insert();
    }
}
