package com.iwms.exception;

/**
 * OOP: Custom Checked Exception.
 * Thrown when an operation requests more stock than is physically available in a bin or batch.
 */
public class InsufficientStockException extends Exception {
    private final String sku;
    private final int requested;
    private final int available;

    public InsufficientStockException(String sku, int requested, int available) {
        super(String.format("Insufficient stock for SKU '%s': requested %d, but only %d available", sku, requested, available));
        this.sku = sku;
        this.requested = requested;
        this.available = available;
    }

    public String getSku() {
        return sku;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
