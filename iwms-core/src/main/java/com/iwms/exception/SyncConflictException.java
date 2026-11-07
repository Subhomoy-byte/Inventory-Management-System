package com.iwms.exception;

/**
 * OOP: Custom Checked Exception.
 * Raised during offline synchronization when client-submitted mutations conflict with current server state.
 */
public class SyncConflictException extends Exception {
    private final String operationId;

    public SyncConflictException(String operationId, String message) {
        super(String.format("Sync conflict in operation '%s': %s", operationId, message));
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }
}
