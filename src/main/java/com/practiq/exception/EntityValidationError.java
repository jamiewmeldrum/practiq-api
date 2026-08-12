package com.practiq.exception;

public record EntityValidationError(String errorField, String failureReason) {}
