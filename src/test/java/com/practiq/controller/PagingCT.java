package com.practiq.controller;

import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import com.practiq.service.QuestionService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import utils.ComponentTest;

import java.util.Collections;

import static io.micronaut.http.HttpStatus.OK;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * A little bit hacky, but necessary.
 * Default paging behavior is controlled by Micronaut and can be specified by application properties.
 * We don't need to test Micronaut, but we do want to pin the default behavior for the app so that it
 * doesn't get changed without due consideration. We don't want to have to do this for every end point
 * as that will lead to bloat, however there isn't a nice way of doing this super generically. Using
 * Questions endpoint as our pin mechanism. Can always be updated to another endpoint later if needs be.
 */
@ComponentTest
public class PagingCT {

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
    void getQuestionsCapsRequestedSizeAtTheConfiguredMaximum() {
        stubRepositoryToEchoThePageable();

        // micronaut.data.pageable.max-page-size=50: an oversized request is capped, not rejected. The
        // envelope's size field is the requested-size echo, so the cap is observable on the wire.
        given()
                .when()
                .get(QUESTIONS_PATH + "?size=500")
                .then()
                .statusCode(OK.getCode())
                .body("size", equalTo(50));
    }

    @Test
    void getQuestionsClampsNegativePageToZero() {
        stubRepositoryToEchoThePageable();

        given()
                .when()
                .get(QUESTIONS_PATH + "?page=-1")
                .then()
                .statusCode(OK.getCode())
                .body("page", equalTo(0));
    }

    @Test
    void getQuestionsFallsBackToDefaultsForUnusablePagingValues() {
        stubRepositoryToEchoThePageable();

        // Micronaut's Pageable binding is deliberately lenient: non-positive or non-numeric page/size
        // fall back to the defaults (page 0, size 10) rather than producing a 400. Pinned so a framework
        // upgrade changing this surfaces here — note the contrast with filter params (conceptId=abc is a
        // 400), which is a known inconsistency of the binder, not of our handlers.
        for (String query : new String[]{"?size=0", "?size=-5", "?size=abc", "?page=abc"}) {
            given()
                    .when()
                    .get(QUESTIONS_PATH + query)
                    .then()
                    .statusCode(OK.getCode())
                    .body("page", equalTo(0))
                    .body("size", equalTo(10));
        }
    }

    @Test
    void getQuestionsReturnsAnEmptyPageBeyondTheData() {
        stubRepositoryToEchoThePageable();

        // Paging past the end is not an error: the envelope echoes the requested position with no rows,
        // which is what lets a client walk pages without a priori knowledge of the total.
        given()
                .when()
                .get(QUESTIONS_PATH + "?page=999")
                .then()
                .statusCode(OK.getCode())
                .body("content", empty())
                .body("page", equalTo(999))
                .body("totalCount", equalTo(0));
    }

    // Echoes whatever Pageable the web layer bound back through the empty repository Page, so the
    // envelope reveals exactly how the layer interpreted the query params.
    private void stubRepositoryToEchoThePageable() {
        when(questionRepository.findAll(Mockito.any(QuerySpecification.class), Mockito.any(Pageable.class)))
                .thenAnswer(invocation -> Page.of(Collections.emptyList(), invocation.getArgument(1), 0L));
    }
}
