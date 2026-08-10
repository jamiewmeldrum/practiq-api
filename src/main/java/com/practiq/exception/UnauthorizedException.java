package com.practiq.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String header) {
        super("Unauthorized due to missing or invalid header: " + header);
    }
}
