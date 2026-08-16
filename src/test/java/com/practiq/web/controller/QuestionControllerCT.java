package com.practiq.web.controller;

import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static utils.TestReflection.setField;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import com.practiq.persistence.QuestionEntity;
import com.practiq.persistence.projection.QuestionConceptLinkProjection;
import com.practiq.persistence.repository.QuestionConceptRepository;
import com.practiq.persistence.repository.QuestionRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import utils.ComponentTest;

@ComponentTest
public class QuestionControllerCT {
    private static final String QUESTIONS_PATH = "/api/v1/questions";

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionConceptRepository questionConceptRepository;

    @Inject
    private EmbeddedServer embeddedServer;

    @MockBean(QuestionRepository.class)
    QuestionRepository questionRepository() {
        return mock(QuestionRepository.class);
    }

    @MockBean(QuestionConceptRepository.class)
    QuestionConceptRepository questionConceptRepository() {
        return mock(QuestionConceptRepository.class);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void getQuestionsSerializesRepositoryResults() {
        long idA = 1L;
        String bodyA = "Question A";
        QuestionDifficulty difficultyA = QuestionDifficulty.EASY;
        QuestionType typeA = QuestionType.EXTENDED;
        QuestionStatus statusA = QuestionStatus.APPROVED;
        Instant createdAtA = Instant.parse("2026-01-01T00:00:00Z");

        QuestionEntity questionA = new QuestionEntity(bodyA, difficultyA, typeA, statusA);
        setField(questionA, "id", idA);
        setField(questionA, "createdAt", createdAtA);

        long idB = 2L;
        String bodyB = "Question B";
        QuestionDifficulty difficultyB = QuestionDifficulty.HARD;
        QuestionType typeB = QuestionType.MCQ;
        QuestionStatus statusB = QuestionStatus.APPROVED;
        Instant createdAtB = Instant.parse("2026-01-01T00:00:00Z");

        QuestionEntity questionB = new QuestionEntity(bodyB, difficultyB, typeB, statusB);
        setField(questionB, "id", idB);
        setField(questionB, "createdAt", createdAtB);

        // Question B is linked to two concepts. Links now arrive via a separate repository query keyed
        // by question id (not the entity's conceptLinks collection), so we stub that repository directly.
        long conceptIdB1 = 10L;
        long conceptIdB2 = 11L;

        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenReturn(Page.of(List.of(questionA, questionB), Pageable.from(0), 2L));
        when(questionConceptRepository.findLinksByQuestionIds(Mockito.any()))
                .thenReturn(List.of(
                        new QuestionConceptLinkProjection(idB, conceptIdB1),
                        new QuestionConceptLinkProjection(idB, conceptIdB2)));

        given().when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("content", "page", "size", "totalCount"))
                .body("content.id", containsInAnyOrder((int) idA, (int) idB))
                .body(
                        "content[0].keySet()",
                        containsInAnyOrder("id", "body", "difficulty", "type", "createdAt", "linkedConceptIds"))
                .body("content.find { it.id == " + idA + " }.body", equalTo(bodyA))
                .body("content.find { it.id == " + idA + " }.difficulty.value", equalTo(difficultyA.value()))
                .body("content.find { it.id == " + idA + " }.difficulty.code", equalTo(difficultyA.name()))
                .body("content.find { it.id == " + idA + " }.type", equalTo(typeA.name()))
                .body("content.find { it.id == " + idA + " }.createdAt", equalTo(createdAtA.toString()))
                .body("content.find { it.id == " + idA + " }.linkedConceptIds", empty())
                .body("content.find { it.id == " + idB + " }.body", equalTo(bodyB))
                .body("content.find { it.id == " + idB + " }.difficulty.value", equalTo(difficultyB.value()))
                .body("content.find { it.id == " + idB + " }.difficulty.code", equalTo(difficultyB.name()))
                .body("content.find { it.id == " + idB + " }.type", equalTo(typeB.name()))
                .body("content.find { it.id == " + idB + " }.createdAt", equalTo(createdAtB.toString()))
                .body(
                        "content.find { it.id == " + idB + " }.linkedConceptIds",
                        containsInAnyOrder((int) conceptIdB1, (int) conceptIdB2));

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class));
        verify(questionConceptRepository).findLinksByQuestionIds(Mockito.any());
    }

    @Test
    void getQuestionsReturnsEmptyArrayWhenRepositoryEmpty() {

        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenReturn(Page.of(Collections.emptyList(), Pageable.from(0), 0L));

        given().when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("content", "page", "size", "totalCount"))
                .body("content", empty())
                .body("totalCount", equalTo(0));

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class));
        verifyNoInteractions(questionConceptRepository);
    }

    @Test
    void getQuestionsReturnsEmptyArrayWhenFilterMatchesNoQuestions() {
        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenReturn(Page.of(Collections.emptyList(), Pageable.from(0), 0L));

        // A filtered request that binds cleanly but matches nothing: the empty page must serialise to [],
        // and with no question ids there's no second query for links to run.
        given().when()
                .get(QUESTIONS_PATH + "?types=MCQ&difficulties=5&conceptId=99")
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("content", "page", "size", "totalCount"))
                .body("content", empty())
                .body("totalCount", equalTo(0));

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class));
        verifyNoInteractions(questionConceptRepository);
    }

    @Test
    void getQuestionsSerializesAFilteredPageWithAndWithoutLinks() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        long linkedId = 1L;
        String linkedBody = "Calculate the acceleration of a car.";
        QuestionDifficulty linkedDifficulty = QuestionDifficulty.EASY;
        QuestionType linkedType = QuestionType.SHORT_ANSWER;
        QuestionEntity linked = approvedQuestion(linkedId, linkedBody, linkedDifficulty, linkedType, createdAt);

        long bareId = 2L;
        String bareBody = "Explain what is meant by diffraction.";
        QuestionDifficulty bareDifficulty = QuestionDifficulty.MEDIUM;
        QuestionType bareType = QuestionType.EXTENDED;
        QuestionEntity bare = approvedQuestion(bareId, bareBody, bareDifficulty, bareType, createdAt);

        long conceptA = 10L;
        long conceptB = 11L;
        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenReturn(Page.of(List.of(linked, bare), Pageable.from(0), 2L));
        // Only the linked question has concept rows; the bare one is absent, so its links serialise as [].
        when(questionConceptRepository.findLinksByQuestionIds(Mockito.any()))
                .thenReturn(List.of(
                        new QuestionConceptLinkProjection(linkedId, conceptA),
                        new QuestionConceptLinkProjection(linkedId, conceptB)));

        given().when()
                .get(QUESTIONS_PATH + "?types=SHORT_ANSWER,EXTENDED")
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("content.id", containsInAnyOrder((int) linkedId, (int) bareId))
                .body(
                        "content[0].keySet()",
                        containsInAnyOrder("id", "body", "difficulty", "type", "createdAt", "linkedConceptIds"))
                .body("content.find { it.id == " + linkedId + " }.body", equalTo(linkedBody))
                .body("content.find { it.id == " + linkedId + " }.difficulty.value", equalTo(linkedDifficulty.value()))
                .body("content.find { it.id == " + linkedId + " }.difficulty.code", equalTo(linkedDifficulty.name()))
                .body("content.find { it.id == " + linkedId + " }.type", equalTo(linkedType.name()))
                .body("content.find { it.id == " + linkedId + " }.createdAt", equalTo(createdAt.toString()))
                .body(
                        "content.find { it.id == " + linkedId + " }.linkedConceptIds",
                        containsInAnyOrder((int) conceptA, (int) conceptB))
                .body("content.find { it.id == " + bareId + " }.body", equalTo(bareBody))
                .body("content.find { it.id == " + bareId + " }.difficulty.value", equalTo(bareDifficulty.value()))
                .body("content.find { it.id == " + bareId + " }.difficulty.code", equalTo(bareDifficulty.name()))
                .body("content.find { it.id == " + bareId + " }.type", equalTo(bareType.name()))
                .body("content.find { it.id == " + bareId + " }.createdAt", equalTo(createdAt.toString()))
                .body("content.find { it.id == " + bareId + " }.linkedConceptIds", empty());

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class));
        verify(questionConceptRepository).findLinksByQuestionIds(Mockito.any());
    }

    @Test
    void getQuestionsReturnsUnprocessableEntityWhenDuplicatesInDifficulties() {
        given().when()
                .get(QUESTIONS_PATH + "?difficulties=1,2,2")
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("difficulties: contains duplicates ([1(TRIVIAL), 2(EASY), 2(EASY)])"))
                .body("status", equalTo(422));
    }

    @Test
    void getQuestionsReturnsBadRequestIfQuestionDifficultiesInvalid() {
        given().when()
                .get(QUESTIONS_PATH + "?difficulties=BAD,PARAMS")
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body(
                        "error",
                        equalTo("difficulties: must be one of "
                                + "1(TRIVIAL), 2(EASY), 3(MEDIUM), 4(HARD), 5(VERY_HARD)"))
                .body("status", equalTo(400));
    }

    @Test
    void getQuestionsReturnsUnprocessableEntityWhenDuplicatesInTypes() {
        given().when()
                .get(QUESTIONS_PATH + "?types=SHORT_ANSWER,EXTENDED,MCQ,MCQ")
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("types: contains duplicates ([SHORT_ANSWER, EXTENDED, MCQ, MCQ])"))
                .body("status", equalTo(422));
    }

    @Test
    void getQuestionsReturnsBadRequestIfQuestionTypesInvalid() {
        given().when()
                .get(QUESTIONS_PATH + "?types=BAD,PARAMS")
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("types: must be one of SHORT_ANSWER, EXTENDED, MCQ"))
                .body("status", equalTo(400));
    }

    // This endpoint binds Pageable through the shared PageableQueryBinder, whose whole contract is proven in
    // PageableQueryBinderCT. One rejection it alone produces is enough to show it is the binder in use here,
    // so those cases are not repeated per endpoint.
    @Test
    void getQuestionsBindsPagingThroughTheSharedBinder() {
        given().when()
                .get(QUESTIONS_PATH + "?page=-1")
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("page: must be greater than or equal to 0"))
                .body("status", equalTo(422));

        verifyNoInteractions(questionRepository);
    }

    @Test
    void getQuestionsReturnsBadRequestWhenConceptIdInvalid() {
        given().when()
                .get(QUESTIONS_PATH + "?conceptId=BAD")
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("conceptId: invalid value"))
                .body("status", equalTo(400));
    }

    // The null-field guard for the question payload: micronaut.serde.serialization.inclusion=always is what
    // keeps an unrated difficulty and an absent type as present-but-null keys rather than dropping them
    // (Serde's own default is NON_EMPTY). Asserted over a real HTTP call, because the encoder the route uses
    // is the only one whose behaviour is the contract.
    @Test
    void getQuestionByIdSerialisesNullDifficultyAndTypeAsPresentNullKeys() {
        long id = 7L;
        String body = "Explain what is meant by diffraction.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        QuestionEntity question = approvedQuestion(id, body, null, null, createdAt);

        when(questionRepository.findAll(Mockito.any(QuerySpecification.class))).thenReturn(List.of(question));
        when(questionConceptRepository.findLinksByQuestionIds(Set.of(id))).thenReturn(List.of());

        given().when()
                .get(QUESTIONS_PATH + "/" + id)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body(
                        "keySet()",
                        containsInAnyOrder("id", "body", "difficulty", "type", "createdAt", "linkedConceptIds"))
                .body("id", equalTo((int) id))
                .body("body", equalTo(body))
                .body("difficulty", nullValue())
                .body("type", nullValue())
                .body("linkedConceptIds", empty());

        verify(questionConceptRepository).findLinksByQuestionIds(Set.of(id));
    }

    @Test
    void getQuestionByIdSerialisedQuestionResponse() {
        long id = 3L;
        String body = "Question A";
        QuestionDifficulty difficulty = QuestionDifficulty.EASY;
        QuestionType type = QuestionType.EXTENDED;
        QuestionStatus status = QuestionStatus.APPROVED;
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        QuestionEntity question = new QuestionEntity(body, difficulty, type, status);
        setField(question, "id", id);
        setField(question, "createdAt", createdAt);

        // Question is linked to two concepts.
        long conceptIdA1 = 10L;
        long conceptIdA2 = 11L;

        List<QuestionConceptLinkProjection> links = List.of(
                new QuestionConceptLinkProjection(id, conceptIdA1), new QuestionConceptLinkProjection(id, conceptIdA2));

        when(questionRepository.findAll(Mockito.any(QuerySpecification.class))).thenReturn(List.of(question));
        when(questionConceptRepository.findLinksByQuestionIds(Set.of(id))).thenReturn(links);

        String path = QUESTIONS_PATH + "/" + id;
        given().when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body(
                        "keySet()",
                        containsInAnyOrder("id", "body", "difficulty", "type", "createdAt", "linkedConceptIds"))
                .body("id", equalTo((int) id))
                .body("body", equalTo(body))
                .body("difficulty.value", equalTo(difficulty.value()))
                .body("difficulty.code", equalTo(difficulty.name()))
                .body("type", equalTo(type.name()))
                .body("createdAt", equalTo(createdAt.toString()))
                .body("linkedConceptIds", containsInAnyOrder((int) conceptIdA1, (int) conceptIdA2));

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class));
        verify(questionConceptRepository).findLinksByQuestionIds(Set.of(id));
    }

    @Test
    void getQuestionByIdSerialisedErrorResponseIfNotFound() {
        long id = 3L;

        when(questionRepository.findAll(Mockito.any(QuerySpecification.class))).thenReturn(List.of());

        String path = QUESTIONS_PATH + "/" + id;
        given().when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class));
    }

    @Test
    void getQuestionByIdReturnsBadRequestIfIdNotANumber() {
        String path = QUESTIONS_PATH + "/error";
        given().when()
                .get(path)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("id: invalid value"))
                .body("status", equalTo(400));
    }

    // An APPROVED question with id and created_at set (both DB-assigned in production, so set by reflection
    // here). created_at must be non-null or Serde omits the key and the keySet assertions break.
    private static QuestionEntity approvedQuestion(
            long id, String body, QuestionDifficulty difficulty, QuestionType type, Instant createdAt) {
        QuestionEntity question = new QuestionEntity(body, difficulty, type, QuestionStatus.APPROVED);
        setField(question, "id", id);
        setField(question, "createdAt", createdAt);
        return question;
    }

    @Test
    void getQuestionByIdReturnsEnvelopeForIdBelowOneAndNeverAsksTheRepository() {
        for (String id : List.of("0", "-4002")) {
            given().when()
                    .get(QUESTIONS_PATH + "/" + id)
                    .then()
                    .statusCode(UNPROCESSABLE_ENTITY.getCode())
                    .contentType(ContentType.JSON)
                    .body("keySet()", containsInAnyOrder("error", "status"))
                    .body("error", equalTo("id: must be greater than or equal to 1"))
                    .body("status", equalTo(422));
        }

        verifyNoInteractions(questionRepository);
    }
}
