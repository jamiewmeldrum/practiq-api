package com.practiq.controller;

import com.practiq.domain.QuestionAttempt;
import com.practiq.domain.query.attempt.QuestionAttemptQuery;
import com.practiq.domain.query.attempt.QuestionAttemptSpecificationFactory;
import com.practiq.repository.QuestionAttemptRepository;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

import java.time.Instant;
import java.util.List;

import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static utils.TestReflection.setField;
import static utils.data.TestData.SESSION_TOKEN_HEADER;

@ComponentTest
public class QuestionAttemptControllerCT {

    private static final String QUESTION_ATTEMPTS_PATH = "/api/v1/questions/%s/attempts";

    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionAttemptRepository questionAttemptRepository;

    @Inject
    private QuestionAttemptSpecificationFactory questionAttemptSpecificationFactory;

    @Inject
    private EmbeddedServer embeddedServer;

    @MockBean(QuestionRepository.class)
    QuestionRepository questionRepository() {
        return mock(QuestionRepository.class);
    }

    @MockBean(QuestionAttemptRepository.class)
    QuestionAttemptRepository questionAttemptRepository() {
        return mock(QuestionAttemptRepository.class);
    }

    @MockBean(QuestionAttemptSpecificationFactory.class)
    QuestionAttemptSpecificationFactory questionAttemptSpecificationFactory() {
        return spy(new QuestionAttemptSpecificationFactory());
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void getQuestionAttemptsReturns400IfNoSessionToken() {
        String path = QUESTION_ATTEMPTS_PATH.formatted(9L);
        given()
                .when()
                .get(path)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Required Header [" + SESSION_TOKEN_HEADER + "] not specified"))
                .body("status", equalTo(400));
    }

    @Test
    void getQuestionAttemptsReturns404IfQuestionDoesNotExistForId() {
        long questionId = 5L;

        when(questionRepository.exists(any(QuerySpecification.class))).thenReturn(false);

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given()
                .header(new Header(SESSION_TOKEN_HEADER,"test"))
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));

        verify(questionRepository).exists(any(QuerySpecification.class));
    }

    @Test
    void getQuestionAttemptsReturnsEmptyResponseIfNoAttemptsForQuestionId() {
        long questionId = 4L;
        String sessionToken = "test";

        when(questionRepository.exists(any(QuerySpecification.class))).thenReturn(true);
        when(questionAttemptRepository.findAll(any(QuerySpecification.class), eq(STABLE_ORDER))).thenReturn(List.of());

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);

        given()
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("$", empty());

        verify(questionRepository).exists(any(QuerySpecification.class));

        QuestionAttemptQuery questionAttemptQuery = new QuestionAttemptQuery(questionId, sessionToken);
        verify(questionAttemptSpecificationFactory).forQuery(questionAttemptQuery);

        verify(questionAttemptRepository).findAll(any(QuerySpecification.class), eq(STABLE_ORDER));
    }

    @Test
    void getQuestionAttemptsReturnsBadRequestIfQuestionIdNotNaturalNumber() {
        String questionId = "error";
        String sessionToken = "test";
        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given()
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .when()
                .get(path)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("questionId: invalid value"))
                .body("status", equalTo(400));
    }

    @Test
    void getQuestionAttemptsReturnsUnprocessableEntityIfSessionTokenBlank() {
        String path = QUESTION_ATTEMPTS_PATH.formatted(1L);

        //Empty
        given()
                .header(new Header(SESSION_TOKEN_HEADER,""))
                .when()
                .get(path)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("sessionToken: must not be blank"))
                .body("status", equalTo(422));

        //Blank
        given()
                .header(new Header(SESSION_TOKEN_HEADER,"   "))
                .when()
                .get(path)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("sessionToken: must not be blank"))
                .body("status", equalTo(422));
    }

    @Test
    void getQuestionAttemptsSerialisesResponse() {
        long questionId = 1L;
        String sessionToken = "test";

        long attemptId1 = 10L;
        String attempt1Body = "body 1";
        Instant createdAt1 = Instant.parse("2026-01-01T00:00:00Z");
        QuestionAttempt attempt1 = new QuestionAttempt(questionId, sessionToken, attempt1Body);
        setField(attempt1, "createdAt", createdAt1);
        setField(attempt1, "id", attemptId1);

        long attemptId2 = 20L;
        String attempt2Body = "body 2";
        Instant createdAt2 = Instant.parse("2026-01-02T00:00:00Z");
        QuestionAttempt attempt2 = new QuestionAttempt(questionId, sessionToken, attempt2Body);
        setField(attempt2, "createdAt", createdAt2);
        setField(attempt2, "id", attemptId2);

        List<QuestionAttempt> attempts = List.of(
                attempt1,
                attempt2
        );

        when(questionRepository.exists(any(QuerySpecification.class))).thenReturn(true);
        when(questionAttemptRepository.findAll(any(QuerySpecification.class), eq(STABLE_ORDER))).thenReturn(attempts);

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);

        given()
                .header(new Header(SESSION_TOKEN_HEADER,sessionToken))
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("id", containsInAnyOrder((int) attemptId1, (int) attemptId2))
                .body("[0].keySet()", containsInAnyOrder("id", "questionId", "body", "createdAt"))
                .body("find { it.id == " + attemptId1 + " }.questionId", equalTo((int) questionId))
                .body("find { it.id == " + attemptId1 + " }.body", equalTo(attempt1Body))
                .body("find { it.id == " + attemptId1 + " }.createdAt", equalTo(createdAt1.toString()))
                .body("find { it.id == " + attemptId2 + " }.questionId", equalTo((int) questionId))
                .body("find { it.id == " + attemptId2 + " }.body", equalTo(attempt2Body))
                .body("find { it.id == " + attemptId2 + " }.createdAt", equalTo(createdAt2.toString()));

        verify(questionRepository).exists(any(QuerySpecification.class));

        QuestionAttemptQuery questionAttemptQuery = new QuestionAttemptQuery(questionId, sessionToken);
        verify(questionAttemptSpecificationFactory).forQuery(questionAttemptQuery);

        verify(questionAttemptRepository).findAll(any(QuerySpecification.class), eq(STABLE_ORDER));
    }
}
