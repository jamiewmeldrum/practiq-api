package com.practiq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.practiq.dto.filter.UserRequestFilter;
import com.practiq.dto.request.QuestionAttemptRequest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

@ComponentTest
class QuestionAttemptServiceValidationCT {

    private static final long QUESTION_ID = 1L;
    private static final String BLANK = "   ";

    @Inject
    private QuestionAttemptService questionAttemptService;

    @Test
    void getForQuestionIdRejectsBlankSessionTokenOnFilter() {
        UserRequestFilter filter = new UserRequestFilter(BLANK);

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class, () -> questionAttemptService.getForQuestionId(filter, QUESTION_ID));

        assertEquals("getForQuestionId.userRequestFilter.sessionToken: must not be blank", exception.getMessage());
    }

    @Test
    void postForQuestionIdRejectsBlankSessionToken() {
        QuestionAttemptRequest request = new QuestionAttemptRequest("an attempt");

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> questionAttemptService.postForQuestionId(BLANK, request, QUESTION_ID));

        assertEquals("postForQuestionId.sessionToken: must not be blank", exception.getMessage());
    }

    @Test
    void postForQuestionIdRejectsBlankBodyOnRequest() {
        QuestionAttemptRequest request = new QuestionAttemptRequest(BLANK);

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> questionAttemptService.postForQuestionId("a session token", request, QUESTION_ID));

        assertEquals("postForQuestionId.request.body: must not be blank", exception.getMessage());
    }
}
