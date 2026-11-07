package com.iwms.exception;

/**
 * OOP: Custom Checked Exception.
 * Wraps file I/O errors and serialization failures during atomic file persistence.
 */
public class StorageException extends Exception {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
