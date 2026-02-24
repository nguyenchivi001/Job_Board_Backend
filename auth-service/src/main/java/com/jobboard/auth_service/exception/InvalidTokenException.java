package com.jobboard.auth_service.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Invalid or expired token");
    }
}
