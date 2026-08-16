package com.practiq.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.practiq.foundation.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

class AdminKeyValidatorTest {

    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized due to missing or invalid header: X-Admin-Key";
    private static final String UNBOUND_KEY_MESSAGE = "Admin key could not bind from parameter ${practiq.admin-key}";

    @Test
    void validateAcceptsAKeyMatchingTheConfiguredOne() {
        String expectedKey = "cb0f9d0e-4d3f-4a2f-9b8d-2f1c6a7e5d40";
        AdminKeyValidator validator = new AdminKeyValidator(expectedKey);

        assertDoesNotThrow(() -> validator.validate(expectedKey));
    }

    @Test
    void validateRejectsAKeyThatDoesNotMatch() {
        AdminKeyValidator validator = new AdminKeyValidator("cb0f9d0e-4d3f-4a2f-9b8d-2f1c6a7e5d40");

        UnauthorizedException thrown =
                assertThrows(UnauthorizedException.class, () -> validator.validate("not-the-admin-key"));

        assertEquals(UNAUTHORIZED_MESSAGE, thrown.getMessage());
    }

    @Test
    void validateRejectsANullKey() {
        AdminKeyValidator validator = new AdminKeyValidator("cb0f9d0e-4d3f-4a2f-9b8d-2f1c6a7e5d40");

        UnauthorizedException thrown = assertThrows(UnauthorizedException.class, () -> validator.validate(null));

        assertEquals(UNAUTHORIZED_MESSAGE, thrown.getMessage());
    }

    @Test
    void validateRejectsAnEmptyKey() {
        AdminKeyValidator validator = new AdminKeyValidator("cb0f9d0e-4d3f-4a2f-9b8d-2f1c6a7e5d40");

        UnauthorizedException thrown = assertThrows(UnauthorizedException.class, () -> validator.validate(""));

        assertEquals(UNAUTHORIZED_MESSAGE, thrown.getMessage());
    }

    @Test
    void validatorDoesNotConstructWhenTheConfiguredKeyIsEmpty() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> new AdminKeyValidator(""));

        assertEquals(UNBOUND_KEY_MESSAGE, thrown.getMessage());
    }

    @Test
    void validatorDoesNotConstructWhenTheConfiguredKeyIsNull() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> new AdminKeyValidator(null));

        assertEquals(UNBOUND_KEY_MESSAGE, thrown.getMessage());
    }

    @Test
    void validatorDoesNotConstructWhenTheConfiguredKeyIsWhitespace() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> new AdminKeyValidator("   "));

        assertEquals(UNBOUND_KEY_MESSAGE, thrown.getMessage());
    }
}
