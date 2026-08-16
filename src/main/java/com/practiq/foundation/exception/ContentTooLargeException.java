package com.practiq.foundation.exception;

// Sibling of EntityValidationException for size limits, so a product rule about a value inside the
// payload stays distinguishable from Micronaut rejecting the HTTP body itself — both answer 413.
public class ContentTooLargeException extends RuntimeException {

    private final EntityValidationError error;

    public ContentTooLargeException(String errorField, String failureReason) {
        super(errorField + ": " + failureReason);
        this.error = new EntityValidationError(errorField, failureReason);
    }

    public EntityValidationError error() {
        return error;
    }
}
