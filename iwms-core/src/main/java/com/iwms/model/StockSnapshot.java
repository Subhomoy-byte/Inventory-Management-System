package com.iwms.model;

import java.time.Instant;

/**
 * OOP Principle: Immutable Record.
 * Represents a point-in-time snapshot of product stock and valuation.
 */
public record StockSnapshot(
        String sku,
        int totalQuantity,
        int reservedQuantity,
        double valuation,
        Instant timestamp
) {
    public StockSnapshot {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be blank");
        }
        if (totalQuantity < 0 || reservedQuantity < 0) {
            throw new IllegalArgumentException("Stock quantities cannot be negative");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public int getAvailableQuantity() {
        return Math.max(0, totalQuantity - reservedQuantity);
    }
}
