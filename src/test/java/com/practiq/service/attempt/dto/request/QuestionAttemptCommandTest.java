package com.practiq.service.attempt.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.practiq.service.UserRef;
import org.junit.jupiter.api.Test;

class QuestionAttemptCommandTest {

    private static final UserRef USER = new UserRef("a-session-token");

    @Test
    void aCommandHoldsTheValuesItWasBuiltWith() {
        QuestionAttemptCommand command = new QuestionAttemptCommand(10L, USER, "An attempt.");

        assertEquals(10L, command.questionId());
        assertEquals(USER, command.userRef());
        assertEquals("An attempt.", command.body());
    }

    @Test
    void aCommandCannotBeBuiltWithAQuestionIdBelowOne() {
        assertEquals(
                "questionId must be greater than or equal to 1",
                assertThrows(IllegalArgumentException.class, () -> new QuestionAttemptCommand(0L, USER, "An attempt."))
                        .getMessage());
    }

    @Test
    void aCommandCannotBeBuiltWithoutAUser() {
        assertEquals(
                "userRef must not be null",
                assertThrows(IllegalArgumentException.class, () -> new QuestionAttemptCommand(10L, null, "An attempt."))
                        .getMessage());
    }

    @Test
    void aCommandCannotBeBuiltWithoutABody() {
        assertEquals(
                "body must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new QuestionAttemptCommand(10L, USER, "  "))
                        .getMessage());
    }
}
