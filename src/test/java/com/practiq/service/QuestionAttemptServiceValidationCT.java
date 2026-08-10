package com.practiq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.practiq.domain.identity.UserRef;
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
    void getForQuestionIdRejectsBlankSessionTokenOnUserRef() {
        UserRef userRef = new UserRef(BLANK);

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> questionAttemptService.getForQuestionId(userRef, QUESTION_ID));

        assertEquals("getForQuestionId.userRef.sessionToken: must not be blank", exception.getMessage());
    }

    @Test
    void postForQuestionIdRejectsBlankSessionTokenOnUserRef() {
        UserRef userRef = new UserRef(BLANK);
        QuestionAttemptRequest request = new QuestionAttemptRequest("an attempt");

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> questionAttemptService.postForQuestionId(userRef, request, QUESTION_ID));

        assertEquals("postForQuestionId.userRef.sessionToken: must not be blank", exception.getMessage());
    }

    @Test
    void postForQuestionIdRejectsBlankBodyOnRequest() {
        UserRef userRef = new UserRef("a session token");
        QuestionAttemptRequest request = new QuestionAttemptRequest(BLANK);

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> questionAttemptService.postForQuestionId(userRef, request, QUESTION_ID));

        assertEquals("postForQuestionId.request.body: must not be blank", exception.getMessage());
    }
}
