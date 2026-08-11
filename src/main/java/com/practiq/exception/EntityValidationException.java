package com.practiq.exception;

// Thrown where the state of data is validated by hand before an action is performed, to say that the
// incoming types do not meet expectations and the action cannot proceed.
public class EntityValidationException extends RuntimeException {
    public EntityValidationException(String message) {
        super(message);
    }
}
