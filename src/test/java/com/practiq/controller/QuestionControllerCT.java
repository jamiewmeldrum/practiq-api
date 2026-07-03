package com.practiq.controller;

import com.practiq.domain.Concept;
import com.practiq.domain.Question;
import com.practiq.domain.QuestionConcept;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.repository.QuestionRepository;
import com.practiq.test.ComponentTest;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.practiq.test.TestReflection.setField;
import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

//TODO - pagination, filtering on conceptId
@ComponentTest
public class QuestionControllerCT {
    private static final String QUESTIONS_PATH = "/api/v1/questions";

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private EmbeddedServer embeddedServer;

    @MockBean(QuestionRepository.class)
    QuestionRepository questionRepository() {
        return mock(QuestionRepository.class);
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
}