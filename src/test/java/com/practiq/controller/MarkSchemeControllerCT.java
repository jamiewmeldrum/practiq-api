package com.practiq.controller;

import com.practiq.domain.MarkScheme;
import com.practiq.domain.Question;
import com.practiq.repository.MarkSchemeRepository;
import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import utils.ComponentTest;

import java.time.Instant;
import java.util.Optional;

import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static utils.TestReflection.setField;

// Real web layer through to the real service, QuestionQueryManager, spec factory and mapper — only the
// repositories (the persistence boundary) are mocked, so a single test exercises as much of the app as
// possible: routing, path-var binding, the student-visibility gate, the mark-scheme lookup, mapping and
// the serialised shape (including that version never reaches the payload).
@ComponentTest
public class MarkSchemeControllerCT {

    private static final String QUESTIONS_PATH = "/api/v1/questions";

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionConceptRepository questionConceptRepository;

    @Inject
    private MarkSchemeRepository markSchemeRepository;

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

    @MockBean(MarkSchemeRepository.class)
    MarkSchemeRepository markSchemeRepository() {
        return mock(MarkSchemeRepository.class);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void getMarkSchemeSerialisesResponse() {
        long questionId = 1L;
        long markSchemeId = 5L;
        String body = "Award 1 mark for stating the wave bends around the edge of the gap.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        Question question = new Question();
        setField(question, "id", questionId);

        // version is set to a non-zero value so its absence from the JSON proves the payload drops it,
        // rather than it merely happening to be zero.
        MarkScheme markScheme = new MarkScheme();
        setField(markScheme, "id", markSchemeId);
        setField(markScheme, "question", question);
        setField(markScheme, "version", 7);
        setField(markScheme, "body", body);
        setField(markScheme, "createdAt", createdAt);

        // The visibility gate is satisfied by an approved, resolvable question; the mark scheme is then
        // found for it. The spec is built inside QuestionQueryManager, so it can only be matched by type.
        when(questionRepository.findOne(Mockito.any(QuerySpecification.class))).thenReturn(Optional.of(question));
        when(markSchemeRepository.findByQuestionId(questionId)).thenReturn(Optional.of(markScheme));

        String path = QUESTIONS_PATH + "/" + questionId + "/mark-scheme";
        given()
                .when()
                .get(path)
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("id", "questionId", "body", "createdAt"))
                .body("id", equalTo((int) markSchemeId))
                .body("questionId", equalTo((int) questionId))
                .body("body", equalTo(body))
                .body("createdAt", equalTo(createdAt.toString()));

        verify(questionRepository).findOne(Mockito.any(QuerySpecification.class));
        verify(markSchemeRepository).findByQuestionId(questionId);
    }

    // The two not-found causes (question not visible / no mark scheme) return the identical envelope, so
    // one arm suffices to pin the serialised 404. The invisible-question arm also proves the mark-scheme
    // lookup is never reached.
    @Test
    void getMarkSchemeSerialisesNotFoundEnvelopeWhenQuestionNotVisible() {
        long questionId = 1L;

        when(questionRepository.findOne(Mockito.any(QuerySpecification.class))).thenReturn(Optional.empty());

        String path = QUESTIONS_PATH + "/" + questionId + "/mark-scheme";
        given()
                .when()
                .get(path)
                .then()
                .statusCode(NOT_FOUND.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("Could not find resource for path: " + path))
                .body("status", equalTo(404));

        verify(questionRepository).findOne(Mockito.any(QuerySpecification.class));
        verifyNoInteractions(markSchemeRepository);
    }

    @Test
    void getMarkSchemeReturnsBadRequestIfQuestionIdNotNaturalNumber() {
        String path = QUESTIONS_PATH + "/error/mark-scheme";
        given()
                .when()
                .get(path)
                .then()
                .statusCode(BAD_REQUEST.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("questionId: invalid value"))
                .body("status", equalTo(400));
    }
}
