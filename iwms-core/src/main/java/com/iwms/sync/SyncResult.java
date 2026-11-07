package com.iwms.sync;

import java.time.Instant;

/**
 * Result of attempting to reconcile an offline sync operation against the central ledger.
 */
public record SyncResult(
        String syncId,
        SyncStatus status,
        String message,
        Instant processedAt
) {
}
