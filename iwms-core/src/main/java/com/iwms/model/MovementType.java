package com.iwms.model;

/**
 * OOP: Enum defining physical stock movement operations.
 */
public enum MovementType {
    IN("Stock In / Inbound Receiving"),
    OUT("Stock Out / Customer Dispatch"),
    TRANSFER("Internal Bin-to-Bin / Zone Transfer"),
    ADJUSTMENT("Audit Discrepancy Adjustment");

    private final String description;

    MovementType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
