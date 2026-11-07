package com.iwms.exception;

/**
 * OOP: Custom Checked Exception.
 * Thrown when attempting to register a product whose SKU already exists in the catalog.
 */
public class DuplicateSkuException extends Exception {
    private final String sku;

    public DuplicateSkuException(String sku) {
        super(String.format("Product with SKU '%s' already exists in catalog", sku));
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
