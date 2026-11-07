package com.iwms.model;

import java.time.Instant;

/**
 * OOP Principle: Immutability.
 * Immutable record representing an atomic stock movement event in the warehouse ledger.
 */
public record StockMovement(
        String movementId,
        Instant timestamp,
        String sku,
        String batchNumber,
        MovementType type,
        int quantity,
        String fromBinId,
        String toBinId,
        String reason,
        String actingUsername
) {
    public StockMovement {
        if (movementId == null || movementId.isBlank()) {
            throw new IllegalArgumentException("Movement ID cannot be blank");
        }
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("Product SKU cannot be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Movement type cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Movement quantity must be positive: " + quantity);
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (actingUsername == null || actingUsername.isBlank()) {
            actingUsername = "system";
        }
    }
}
