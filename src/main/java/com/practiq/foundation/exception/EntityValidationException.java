package com.practiq.foundation.exception;

// Thrown where the state of data is validated by hand before an action is performed, to say that the
// incoming types do not meet expectations and the action cannot proceed.
public class EntityValidationException extends RuntimeException {

    private final EntityValidationError error;

    public EntityValidationException(String errorField, String failureReason) {
        // For logs and stack traces only. The client-facing format is the handler's, and the two are
        // free to diverge.
        super(errorField + ": " + failureReason);
        this.error = new EntityValidationError(errorField, failureReason);
    }

    public EntityValidationError error() {
        return error;
    }
}
