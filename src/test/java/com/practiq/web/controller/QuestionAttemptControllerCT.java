package com.practiq.web.controller;

import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static utils.TestReflection.setField;
import static utils.data.TestData.QUESTION_ATTEMPT_SESSION_TOKEN_MAX_LENGTH;
import static utils.data.TestData.SESSION_TOKEN_HEADER;

import com.practiq.persistence.QuestionAttemptEntity;
import com.practiq.persistence.query.attempt.QuestionAttemptQuery;
import com.practiq.persistence.query.attempt.QuestionAttemptSpecificationFactory;
import com.practiq.persistence.repository.QuestionAttemptRepository;
import com.practiq.persistence.repository.QuestionRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

@ComponentTest
public class QuestionAttemptControllerCT {

    private static final String QUESTION_ATTEMPTS_PATH = "/api/v1/questions/%s/attempts";

    // The product rule on the request DTO, which is deliberately lower than the entity and column guard.
    private static final int ATTEMPT_BODY_MAX_LENGTH = 20000;

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
        given().when()
                .get(path)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Required Header [" + SESSION_TOKEN_HEADER + "] not specified"))
                .body("status", equalTo(400));
    }

    @Test
    void getQuestionAttemptsReturnsUnprocessableEntityIfSessionTokenBlank() {
        String path = QUESTION_ATTEMPTS_PATH.formatted(1L);

        // Empty
        given().header(new Header(SESSION_TOKEN_HEADER, ""))
                .when()
                .get(path)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("sessionToken: must not be blank"))
                .body("status", equalTo(422));

        // Blank
        given().header(new Header(SESSION_TOKEN_HEADER, "   "))
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
    void getQuestionAttemptsReturnsBadRequestIfQuestionIdNotANumber() {
        String questionId = "error";
        String sessionToken = "test";
        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given().header(new Header(SESSION_TOKEN_HEADER, sessionToken))
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
    void getQuestionAttemptsReturns404IfQuestionDoesNotExistForId() {
        long questionId = 5L;

        when(questionRepository.exists(any(QuerySpecification.class))).thenReturn(false);

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given().header(new Header(SESSION_TOKEN_HEADER, "test"))
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
        when(questionAttemptRepository.findAll(any(QuerySpecification.class), eq(STABLE_ORDER)))
                .thenReturn(List.of());

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);

        given().header(new Header(SESSION_TOKEN_HEADER, sessionToken))
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
    void getQuestionAttemptsSerialisesResponse() {
        long questionId = 1L;
        String sessionToken = "test";

        long attemptId1 = 10L;
        String attempt1Body = "body 1";
        Instant createdAt1 = Instant.parse("2026-01-01T00:00:00Z");
        QuestionAttemptEntity attempt1 = new QuestionAttemptEntity(questionId, sessionToken, attempt1Body);
        setField(attempt1, "createdAt", createdAt1);
        setField(attempt1, "id", attemptId1);

        long attemptId2 = 20L;
        String attempt2Body = "body 2";
        Instant createdAt2 = Instant.parse("2026-01-02T00:00:00Z");
        QuestionAttemptEntity attempt2 = new QuestionAttemptEntity(questionId, sessionToken, attempt2Body);
        setField(attempt2, "createdAt", createdAt2);
        setField(attempt2, "id", attemptId2);

        List<QuestionAttemptEntity> attempts = List.of(attempt1, attempt2);

        when(questionRepository.exists(any(QuerySpecification.class))).thenReturn(true);
        when(questionAttemptRepository.findAll(any(QuerySpecification.class), eq(STABLE_ORDER)))
                .thenReturn(attempts);

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);

        given().header(new Header(SESSION_TOKEN_HEADER, sessionToken))
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

    @Test
    void postQuestionAttemptReturns400IfNoSessionToken() {
        String attemptBody = "attempt 1";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", attemptBody);

        String path = QUESTION_ATTEMPTS_PATH.formatted(9L);
        given().contentType(ContentType.JSON)
                .when()
                .body(requestBody)
                .post(path)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("Required Header [" + SESSION_TOKEN_HEADER + "] not specified"))
                .body("status", equalTo(400));

        verifyNoInteractions(questionRepository);
        verifyNoInteractions(questionAttemptRepository);
    }

    @Test
    void postQuestionAttemptReturnsUnprocessableEntityIfSessionTokenBlank() {
        String attemptBody = "attempt 1";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", attemptBody);

        String path = QUESTION_ATTEMPTS_PATH.formatted(1L);

        // Empty
        given().contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER, ""))
                .when()
                .body(requestBody)
                .post(path)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("sessionToken: must not be blank"))
                .body("status", equalTo(422));

        // Blank
        given().contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER, "   "))
                .when()
                .body(requestBody)
                .post(path)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("sessionToken: must not be blank"))
                .body("status", equalTo(422));

        verifyNoInteractions(questionRepository);
        verifyNoInteractions(questionAttemptRepository);
    }

    @Test
    void postQuestionAttemptReturnsBadRequestIfQuestionIdNotANumber() {
        String questionId = "error";
        String sessionToken = "test";

        String attemptBody = "attempt 1";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", attemptBody);

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);
        given().contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                .when()
                .body(requestBody)
                .post(path)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("questionId: invalid value"))
                .body("status", equalTo(400));

        verifyNoInteractions(questionRepository);
        verifyNoInteractions(questionAttemptRepository);
    }

    @Test
    void postQuestionAttemptReturnsUnprocessableEntityIfBodyFieldBlank() {
        String sessionToken = "test";

        String path = QUESTION_ATTEMPTS_PATH.formatted(1L);

        // Empty
        given().contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                .when()
                .body(Map.of("body", ""))
                .post(path)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("body: must not be blank"))
                .body("status", equalTo(422));

        // Blank
        given().contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                .when()
                .body(Map.of("body", "   "))
                .post(path)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("body: must not be blank"))
                .body("status", equalTo(422));

        verifyNoInteractions(questionRepository);
        verifyNoInteractions(questionAttemptRepository);
    }

    @Test
    void postQuestionAttemptAcceptsBodyAtTheMaximumLengthAndErrorsOneAbove() {
        String sessionToken = "test";
        String path = QUESTION_ATTEMPTS_PATH.formatted(1L);

        when(questionRepository.exists(any(QuerySpecification.class))).thenReturn(true);
        when(questionAttemptRepository.save(any(QuestionAttemptEntity.class)))
                .thenAnswer(invocation -> setField(invocation.getArgument(0), "id", 1L));

        given().contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                .when()
                .body(Map.of("body", RandomStringUtils.insecure().nextAlphanumeric(ATTEMPT_BODY_MAX_LENGTH)))
                .post(path)
                .then()
                .statusCode(CREATED.getCode());

        given().contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                .when()
                .body(Map.of("body", RandomStringUtils.insecure().nextAlphanumeric(ATTEMPT_BODY_MAX_LENGTH + 1)))
                .post(path)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("body: size must be between 0 and " + ATTEMPT_BODY_MAX_LENGTH))
                .body("status", equalTo(422));
    }

    @Test
    void postQuestionAttemptReturnsBadRequestIfRequestBodyMissing() {
        String sessionToken = "test";

        String path = QUESTION_ATTEMPTS_PATH.formatted(1L);

        given().contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                .when()
                .body("")
                .post(path)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Request body not specified"))
                .body("status", equalTo(400));

        verifyNoInteractions(questionRepository);
        verifyNoInteractions(questionAttemptRepository);
    }

    @Test
    void postQuestionAttemptSavesAttemptAndSerialisesResponse() {
        long questionId = 1L;
        String sessionToken = "test";
        String attemptBody = "body 1";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", attemptBody);

        QuestionAttemptEntity attemptToSave = new QuestionAttemptEntity(questionId, sessionToken, attemptBody);

        long attemptId = 10L;
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        QuestionAttemptEntity attemptDB = new QuestionAttemptEntity(questionId, sessionToken, attemptBody);
        setField(attemptDB, "createdAt", createdAt);
        setField(attemptDB, "id", attemptId);

        when(questionRepository.exists(any(QuerySpecification.class))).thenReturn(true);
        when(questionAttemptRepository.save(attemptToSave)).thenReturn(attemptDB);

        String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);

        given().contentType(ContentType.JSON)
                .header(new Header(SESSION_TOKEN_HEADER, sessionToken))
                .when()
                .body(requestBody)
                .post(path)
                .then()
                .statusCode(CREATED.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("id", "questionId", "body", "createdAt"))
                .body("id", equalTo((int) attemptId))
                .body("questionId", equalTo((int) questionId))
                .body("body", equalTo(attemptBody))
                .body("createdAt", equalTo(createdAt.toString()));

        verify(questionRepository).exists(any(QuerySpecification.class));
        verify(questionAttemptRepository).save(attemptToSave);
    }

    @Test
    void questionAttemptEndpointsRejectAQuestionIdBelowOneWithoutAskingTheRepositories() {
        for (String questionId : List.of("0", "-4002")) {
            String path = QUESTION_ATTEMPTS_PATH.formatted(questionId);

            given().header(new Header(SESSION_TOKEN_HEADER, "test"))
                    .when()
                    .get(path)
                    .then()
                    .statusCode(UNPROCESSABLE_ENTITY.getCode())
                    .body("error", equalTo("questionId: must be greater than or equal to 1"));

            given().contentType(ContentType.JSON)
                    .header(new Header(SESSION_TOKEN_HEADER, "test"))
                    .when()
                    .body(Map.of("body", "an attempt"))
                    .post(path)
                    .then()
                    .statusCode(UNPROCESSABLE_ENTITY.getCode())
                    .body("error", equalTo("questionId: must be greater than or equal to 1"));
        }

        verifyNoInteractions(questionRepository);
        verifyNoInteractions(questionAttemptRepository);
    }

    @Test
    void questionAttemptEndpointsAcceptASessionTokenAtTheMaximumLengthAndRejectOneAbove() {
        String path = QUESTION_ATTEMPTS_PATH.formatted(1L);
        String validToken = RandomStringUtils.insecure().nextAlphanumeric(QUESTION_ATTEMPT_SESSION_TOKEN_MAX_LENGTH);
        String tooLongToken =
                RandomStringUtils.insecure().nextAlphanumeric(QUESTION_ATTEMPT_SESSION_TOKEN_MAX_LENGTH + 1);

        when(questionRepository.exists(any(QuerySpecification.class))).thenReturn(true);
        when(questionAttemptRepository.findAll(any(QuerySpecification.class), any(Sort.class)))
                .thenReturn(List.of());

        given().header(new Header(SESSION_TOKEN_HEADER, validToken))
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode());

        given().header(new Header(SESSION_TOKEN_HEADER, tooLongToken))
                .when()
                .get(path)
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body(
                        "error",
                        equalTo("sessionToken: size must be between 0 and "
                                + QUESTION_ATTEMPT_SESSION_TOKEN_MAX_LENGTH))
                .body("status", equalTo(422));
    }
}
