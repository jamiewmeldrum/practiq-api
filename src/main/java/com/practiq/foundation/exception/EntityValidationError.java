package com.practiq.foundation.exception;

public record EntityValidationError(String errorField, String failureReason) {}
