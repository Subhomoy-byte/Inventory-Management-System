package com.iwms.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * OOP Principle: Encapsulation.
 * Represents an identifiable inventory batch/lot with expiration tracking.
 */
public class Batch {
    private final String batchNumber;
    private final String productSku;
    private final LocalDate manufactureDate;
    private final LocalDate expiryDate;
    private int quantity;

    public Batch(String batchNumber, String productSku, LocalDate manufactureDate, LocalDate expiryDate, int quantity) {
        if (batchNumber == null || batchNumber.isBlank()) {
            throw new IllegalArgumentException("Batch number cannot be null or blank");
        }
        if (productSku == null || productSku.isBlank()) {
            throw new IllegalArgumentException("Product SKU cannot be null or blank");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Batch initial quantity cannot be negative: " + quantity);
        }

        this.batchNumber = batchNumber.trim();
        this.productSku = productSku.trim();
        this.manufactureDate = manufactureDate != null ? manufactureDate : LocalDate.now();
        this.expiryDate = expiryDate;
        this.quantity = quantity;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public long getDaysUntilExpiration() {
        if (expiryDate == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public String getProductSku() {
        return productSku;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Batch quantity cannot be negative");
        }
        this.quantity = quantity;
    }

    public void adjustQuantity(int delta) {
        if (this.quantity + delta < 0) {
            throw new IllegalArgumentException("Cannot reduce batch quantity below zero");
        }
        this.quantity += delta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Batch batch = (Batch) o;
        return Objects.equals(batchNumber, batch.batchNumber) && Objects.equals(productSku, batch.productSku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchNumber, productSku);
    }
}
