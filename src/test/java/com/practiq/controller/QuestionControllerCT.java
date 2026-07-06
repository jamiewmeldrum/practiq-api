package com.practiq.controller;

import com.practiq.domain.Concept;
import com.practiq.domain.Question;
import com.practiq.domain.QuestionConcept;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.repository.QuestionRepository;
import com.practiq.service.QuestionService;
import com.practiq.test.ComponentTest;
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
import java.util.Set;

import static com.practiq.test.TestReflection.assertAllFieldsSet;
import static com.practiq.test.TestReflection.setField;
import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

//TODO - pagination, filtering on conceptId
@ComponentTest
public class QuestionControllerCT {
    private static final String QUESTIONS_PATH = "/api/v1/questions";

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionService questionService;

    @Inject
    private EmbeddedServer embeddedServer;

    @MockBean(QuestionRepository.class)
    QuestionRepository questionRepository() {
        return mock(QuestionRepository.class);
    }

    // Spy the real service so its logic still runs for the serialisation tests, while letting us
    // verify the exact QuestionRequest the controller hands it. spy(Class) can't be used — Mockito
    // has no constructor to call — so wrap a real instance built from the (mocked) repository and the
    // real specification factory.
    @MockBean(QuestionService.class)
    QuestionService questionService(QuestionRepository questionRepository,
                                    QuestionSpecificationFactory questionSpecificationFactory) {
        return spy(new QuestionService(questionRepository, questionSpecificationFactory));
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

        //Links some Concepts to Question B
        long conceptIdB1 = 10L;
        Concept conceptB1 = new Concept("Concept A", "Concept A description");
        setField(conceptB1, "id", conceptIdB1);
        long conceptIdB2 = 11L;
        Concept conceptB2 = new Concept("Concept B", "Concept B description");
        setField(conceptB2, "id", conceptIdB2);

        QuestionConcept conceptLinkB1 = new QuestionConcept(questionB, conceptB1);
        QuestionConcept conceptLinkB2 = new QuestionConcept(questionB, conceptB2);
        Set<QuestionConcept> conceptLinks = Set.of(conceptLinkB1, conceptLinkB2);
        setField(questionB, "conceptLinks", conceptLinks);

        when(questionRepository.findAll(Mockito.any(QuerySpecification.class))).thenReturn(List.of(questionA, questionB));

        given()
                .when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("id", containsInAnyOrder((int) idA, (int) idB))
                .body("[0].keySet()", containsInAnyOrder("id", "body", "difficulty", "type", "source", "status", "sourceSpec", "createdAt", "linkedConceptIds"))
                .body("find { it.id == " + idA + " }.body", equalTo(bodyA))
                .body("find { it.id == " + idA + " }.difficulty.value", equalTo(difficultyA.value()))
                .body("find { it.id == " + idA + " }.difficulty.code", equalTo(difficultyA.name()))
                .body("find { it.id == " + idA + " }.type", equalTo(typeA.name()))
                .body("find { it.id == " + idA + " }.source", equalTo(sourceA.name()))
                .body("find { it.id == " + idA + " }.status", equalTo(statusA.name()))
                .body("find { it.id == " + idA + " }.sourceSpec", equalTo(sourceSpecA))
                .body("find { it.id == " + idA + " }.createdAt", equalTo(createdAtA.toString()))
                .body("find { it.id == " + idA + " }.linkedConceptIds", empty())
                .body("find { it.id == " + idB + " }.body", equalTo(bodyB))
                .body("find { it.id == " + idB + " }.difficulty.value", equalTo(difficultyB.value()))
                .body("find { it.id == " + idB + " }.difficulty.code", equalTo(difficultyB.name()))
                .body("find { it.id == " + idB + " }.type", equalTo(typeB.name()))
                .body("find { it.id == " + idB + " }.source", equalTo(sourceB.name()))
                .body("find { it.id == " + idB + " }.status", equalTo(statusB.name()))
                .body("find { it.id == " + idB + " }.sourceSpec", equalTo(sourceSpecB))
                .body("find { it.id == " + idB + " }.createdAt", equalTo(createdAtB.toString()))
                .body("find { it.id == " + idB + " }.linkedConceptIds", containsInAnyOrder((int) conceptIdB1, (int) conceptIdB2));

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class));
    }

    @Test
    void getQuestionsReturnsEmptyArrayWhenRepositoryEmpty() {

        when(questionRepository.findAll(Mockito.any(QuerySpecification.class))).thenReturn(Collections.emptyList());

        given()
                .when()
                .get(QUESTIONS_PATH)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("$", empty());

        verify(questionRepository).findAll(Mockito.any(QuerySpecification.class));
    }

    @Test
    void getQuestionsPassesCorrectRequestToQuestionService() {
        when(questionRepository.findAll(Mockito.any(QuerySpecification.class))).thenReturn(Collections.emptyList());

        // The request we expect this URL to produce, with every field driven to a distinguishable value.
        QuestionRequest expected = new QuestionRequest();
        expected.setTypes(List.of(QuestionType.SHORT_ANSWER, QuestionType.EXTENDED, QuestionType.MCQ));

        given()
                .when()
                .get(QUESTIONS_PATH + "?types=SHORT_ANSWER,EXTENDED,MCQ")
                .then()
                .statusCode(OK.getCode());

        ArgumentCaptor<QuestionRequest> captor = ArgumentCaptor.forClass(QuestionRequest.class);
        verify(questionService).get(captor.capture());
        QuestionRequest actual = captor.getValue();

        // Tripwire: the request the service actually received must have every field populated. Add a
        // field to QuestionRequest and this fails until the URL above drives it — so a new field can't
        // slip through unasserted.
        assertAllFieldsSet(actual);
        // ...and every field must carry the value we expect (@EqualsAndHashCode covers them all).
        assertEquals(expected, actual);
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
}