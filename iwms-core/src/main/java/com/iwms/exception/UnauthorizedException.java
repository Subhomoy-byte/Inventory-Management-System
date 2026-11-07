package com.iwms.exception;

/**
 * OOP: Custom Unchecked Runtime Exception.
 * Thrown when an unauthenticated request attempts to access protected endpoints (maps to HTTP 401).
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
