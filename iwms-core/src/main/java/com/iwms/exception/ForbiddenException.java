package com.iwms.exception;

/**
 * OOP: Custom Unchecked Runtime Exception.
 * Thrown when an authenticated user lacks required permissions (maps to HTTP 403).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
