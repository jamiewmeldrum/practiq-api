package com.practiq.controller;

import com.practiq.domain.Question;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import com.practiq.service.QuestionService;
import utils.ComponentTest;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static utils.TestReflection.assertAllFieldsSet;
import static utils.TestReflection.setField;
import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ComponentTest
public class QuestionControllerCT {
    private static final String QUESTIONS_PATH = "/api/v1/questions";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionService questionService;

    @Inject
    private EmbeddedServer embeddedServer;

    @Inject
    private QuestionConceptRepository questionConceptRepository;

    @MockBean(QuestionRepository.class)
    QuestionRepository questionRepository() {
        return mock(QuestionRepository.class);
    }

    @MockBean(QuestionConceptRepository.class)
    QuestionConceptRepository questionConceptRepository() {
        return mock(QuestionConceptRepository.class);
    }

    // Spy the real service so its logic still runs for the serialisation tests, while letting us
    // verify the exact QuestionRequest the controller hands it. spy(Class) can't be used — Mockito
    // has no constructor to call — so wrap a real instance built from the (mocked) repositories and the
    // real specification factory.
    @MockBean(QuestionService.class)
    QuestionService questionService(QuestionRepository questionRepository,
                                    QuestionConceptRepository questionConceptRepository,
                                    QuestionSpecificationFactory questionSpecificationFactory) {
        return spy(new QuestionService(questionRepository, questionConceptRepository, questionSpecificationFactory));
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
        QuestionSource sourceA = QuestionSource.GENERATED;
        QuestionStatus statusA = QuestionStatus.APPROVED;
        String sourceSpecA = "GCSE Physics";
        Instant createdAtA = Instant.parse("2026-01-01T00:00:00Z");

        Question questionA = new Question(
                bodyA,
                difficultyA,
                typeA,
                sourceA,
                statusA,
                sourceSpecA
        );
        setField(questionA, "id", idA);
        setField(questionA, "createdAt", createdAtA);

        long idB = 2L;
        String bodyB = "Question B";
        QuestionDifficulty difficultyB = QuestionDifficulty.HARD;
        QuestionType typeB = QuestionType.MCQ;
        QuestionSource sourceB = QuestionSource.EXTRACTED;
        QuestionStatus statusB = QuestionStatus.APPROVED;
        String sourceSpecB = "GCSE Maths";
        Instant createdAtB = Instant.parse("2026-01-01T00:00:00Z");

        Question questionB = new Question(
                bodyB,
                difficultyB,
                typeB,
                sourceB,
                statusB,
                sourceSpecB
        );
        setField(questionB, "id", idB);
        setField(questionB, "createdAt", createdAtB);

        // Question B is linked to two concepts. Links now arrive via a separate repository query keyed
        // by question id (not the entity's conceptLinks collection), so we stub that repository directly.
        long conceptIdB1 = 10L;
        long conceptIdB2 = 11L;

        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenReturn(Page.of(List.of(questionA, questionB), Pageable.from(0), 2L));
        when(questionConceptRepository.findLinksByQuestionIds(Mockito.any()))
                .thenReturn(List.of(new QuestionConceptLink(idB, conceptIdB1), new QuestionConceptLink(idB, conceptIdB2)));

        given()
                .when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("content", "page", "size", "totalCount"))
                .body("content.id", containsInAnyOrder((int) idA, (int) idB))
                .body("content[0].keySet()", containsInAnyOrder("id", "body", "difficulty", "type", "createdAt", "linkedConceptIds"))
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
                .body("content.find { it.id == " + idB + " }.linkedConceptIds", containsInAnyOrder((int) conceptIdB1, (int) conceptIdB2));

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class));
    }

    @Test
    void getQuestionsReturnsEmptyArrayWhenRepositoryEmpty() {

        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenReturn(Page.of(Collections.emptyList(), Pageable.from(0), 0L));

        given()
                .when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("content", "page", "size", "totalCount"))
                .body("content", empty())
                .body("totalCount", equalTo(0));

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class));
    }

    @Test
    void getQuestionsReturnsEmptyArrayWhenFilterMatchesNoQuestions() {
        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenReturn(Page.of(Collections.emptyList(), Pageable.from(0), 0L));

        // A filtered request that binds cleanly but matches nothing: the empty page must serialise to [],
        // and with no question ids there's no second query for links to run.
        given()
                .when()
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
        long linkedId = 1L;
        String linkedBody = "Calculate the acceleration of a car.";
        QuestionDifficulty linkedDifficulty = QuestionDifficulty.EASY;
        QuestionType linkedType = QuestionType.SHORT_ANSWER;
        Question linked = approvedQuestion(linkedId, linkedBody, linkedDifficulty, linkedType);

        long bareId = 2L;
        String bareBody = "Explain what is meant by diffraction.";
        QuestionDifficulty bareDifficulty = QuestionDifficulty.MEDIUM;
        QuestionType bareType = QuestionType.EXTENDED;
        Question bare = approvedQuestion(bareId, bareBody, bareDifficulty, bareType);

        long conceptA = 10L;
        long conceptB = 11L;
        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenReturn(Page.of(List.of(linked, bare), Pageable.from(0), 2L));
        // Only the linked question has concept rows; the bare one is absent, so its links serialise as [].
        when(questionConceptRepository.findLinksByQuestionIds(Mockito.any()))
                .thenReturn(List.of(new QuestionConceptLink(linkedId, conceptA), new QuestionConceptLink(linkedId, conceptB)));

        given()
                .when()
                .get(QUESTIONS_PATH + "?types=SHORT_ANSWER,EXTENDED")
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("content.id", containsInAnyOrder((int) linkedId, (int) bareId))
                .body("content[0].keySet()", containsInAnyOrder("id", "body", "difficulty", "type", "createdAt", "linkedConceptIds"))

                .body("content.find { it.id == " + linkedId + " }.body", equalTo(linkedBody))
                .body("content.find { it.id == " + linkedId + " }.difficulty.value", equalTo(linkedDifficulty.value()))
                .body("content.find { it.id == " + linkedId + " }.difficulty.code", equalTo(linkedDifficulty.name()))
                .body("content.find { it.id == " + linkedId + " }.type", equalTo(linkedType.name()))
                .body("content.find { it.id == " + linkedId + " }.createdAt", equalTo(CREATED_AT.toString()))
                .body("content.find { it.id == " + linkedId + " }.linkedConceptIds", containsInAnyOrder((int) conceptA, (int) conceptB))

                .body("content.find { it.id == " + bareId + " }.body", equalTo(bareBody))
                .body("content.find { it.id == " + bareId + " }.difficulty.value", equalTo(bareDifficulty.value()))
                .body("content.find { it.id == " + bareId + " }.difficulty.code", equalTo(bareDifficulty.name()))
                .body("content.find { it.id == " + bareId + " }.type", equalTo(bareType.name()))
                .body("content.find { it.id == " + bareId + " }.createdAt", equalTo(CREATED_AT.toString()))
                .body("content.find { it.id == " + bareId + " }.linkedConceptIds", empty());

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class));
    }

    @Test
    void getQuestionsPassesCorrectRequestToQuestionService() {
        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenReturn(Page.of(Collections.emptyList(), Pageable.from(0), 0L));

        // The request we expect this URL to produce, with every field driven to a distinguishable value.
        QuestionRequest expected = new QuestionRequest();
        expected.setTypes(List.of(QuestionType.SHORT_ANSWER, QuestionType.EXTENDED, QuestionType.MCQ));
        expected.setDifficulties(List.of(QuestionDifficulty.TRIVIAL, QuestionDifficulty.EASY, QuestionDifficulty.MEDIUM));
        expected.setConceptId(99L);

        given()
                .when()
                .get(QUESTIONS_PATH + "?types=SHORT_ANSWER,EXTENDED,MCQ&difficulties=1,2,3&conceptId=99")
                .then()
                .statusCode(OK.getCode());

        ArgumentCaptor<QuestionRequest> captor = ArgumentCaptor.forClass(QuestionRequest.class);
        verify(questionService).get(captor.capture(), Mockito.any(Pageable.class));
        QuestionRequest actual = captor.getValue();

        // Tripwire: the request the service actually received must have every field populated. Add a
        // field to QuestionRequest and this fails until the URL above drives it — so a new field can't
        // slip through unasserted.
        assertAllFieldsSet(actual);
        // ...and every field must carry the value we expect (@EqualsAndHashCode covers them all).
        assertEquals(expected, actual);
    }

    @Test
    void getQuestionsReturnsUnprocessableEntityWhenDuplicatesInDifficulties() {
        given()
                .when()
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
        given()
                .when()
                .get(QUESTIONS_PATH + "?difficulties=BAD,PARAMS")
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("difficulties: must be one of "
                        + "1(TRIVIAL), 2(EASY), 3(MEDIUM), 4(HARD), 5(VERY_HARD)"))
                .body("status", equalTo(400));
    }

    @Test
    void getQuestionsReturnsUnprocessableEntityWhenDuplicatesInTypes() {
        given()
                .when()
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
        given()
                .when()
                .get(QUESTIONS_PATH + "?types=BAD,PARAMS")
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("types: must be one of SHORT_ANSWER, EXTENDED, MCQ"))
                .body("status", equalTo(400));
    }

    @Test
    void getQuestionsReturnsBadRequestWhenConceptIdInvalid() {
        given()
                .when()
                .get(QUESTIONS_PATH + "?conceptId=BAD")
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("conceptId: invalid value"))
                .body("status", equalTo(400));
    }

    // An APPROVED question with id and created_at set (both DB-assigned in production, so set by reflection
    // here). created_at must be non-null or Serde omits the key and the keySet assertions break.
    private static Question approvedQuestion(long id, String body, QuestionDifficulty difficulty, QuestionType type) {
        Question question = new Question(body, difficulty, type, QuestionSource.SEED, QuestionStatus.APPROVED, "AQA GCSE Physics");
        setField(question, "id", id);
        setField(question, "createdAt", CREATED_AT);
        return question;
    }
}