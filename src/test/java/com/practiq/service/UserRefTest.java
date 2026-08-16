package com.practiq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.data.TestData.QUESTION_ATTEMPT_SESSION_TOKEN_MAX_LENGTH;

import org.junit.jupiter.api.Test;

class UserRefTest {

    @Test
    void aUserRefHoldsTheTokenItWasBuiltWith() {
        String sessionToken = "b10ef800-3a3a-4395-9bbd-bfe2fa316872";

        assertEquals(sessionToken, new UserRef(sessionToken).sessionToken());
    }

    @Test
    void aUserRefCannotBeBuiltWithoutASessionToken() {
        assertEquals(
                "sessionToken must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new UserRef(null))
                        .getMessage());
        assertEquals(
                "sessionToken must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new UserRef(""))
                        .getMessage());
        assertEquals(
                "sessionToken must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new UserRef("   "))
                        .getMessage());
    }

    @Test
    void aUserRefTakesASessionTokenAtTheMaximumLengthButNotOneAbove() {
        String atTheMaximum = "a".repeat(QUESTION_ATTEMPT_SESSION_TOKEN_MAX_LENGTH);

        assertEquals(atTheMaximum, new UserRef(atTheMaximum).sessionToken());

        String oneAbove = "a".repeat(QUESTION_ATTEMPT_SESSION_TOKEN_MAX_LENGTH + 1);

        assertEquals(
                "sessionToken cannot exceed max length " + QUESTION_ATTEMPT_SESSION_TOKEN_MAX_LENGTH,
                assertThrows(IllegalArgumentException.class, () -> new UserRef(oneAbove))
                        .getMessage());
    }
}
