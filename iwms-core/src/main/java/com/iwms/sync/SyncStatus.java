package com.iwms.sync;

/**
 * Status resulting from processing an offline synchronization operation.
 */
public enum SyncStatus {
    APPLIED,
    DUPLICATE,
    CONFLICT
}
