package com.iwms.model;

/**
 * OOP Principle: Immutable Record.
 * Represents an analytical suggestion for stock replenishment.
 */
public record ReorderSuggestion(
        String sku,
        int suggestedQuantity,
        String rationale,
        int urgencyRating
) {
    public ReorderSuggestion {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be blank");
        }
        if (suggestedQuantity < 0) {
            throw new IllegalArgumentException("Suggested quantity cannot be negative");
        }
    }
}
