package com.iwms.sync;

/**
 * Immutable payload representing a stock operation performed offline by a client device.
 */
public record SyncOperation(
        String syncId,
        String clientTimestamp,
        String operationType,
        String sku,
        String batchNumber,
        String fromBinId,
        String toBinId,
        int quantity,
        String reason,
        String username
) {
}
