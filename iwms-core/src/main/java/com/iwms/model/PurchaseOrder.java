package com.iwms.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * OOP Principle: Encapsulation & Composition.
 * Represents an inbound purchase order from an approved supplier.
 */
public class PurchaseOrder implements Storable, Auditable {

    public enum Status {
        CREATED,
        RECEIVED,
        CANCELLED
    }

    public record OrderLine(String sku, int quantity, double unitCost) {
        public OrderLine {
            if (sku == null || sku.isBlank()) throw new IllegalArgumentException("SKU cannot be blank");
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
            if (unitCost < 0) throw new IllegalArgumentException("Unit cost cannot be negative");
        }
    }

    private final String poNumber;
    private final String supplierId;
    private Status status;
    private final List<OrderLine> lines;
    private final String createdBy;
    private final Instant createdAt;
    private Instant receivedAt;

    public PurchaseOrder(String poNumber, String supplierId, String createdBy) {
        if (poNumber == null || poNumber.isBlank()) {
            throw new IllegalArgumentException("PO number cannot be blank");
        }
        if (supplierId == null || supplierId.isBlank()) {
            throw new IllegalArgumentException("Supplier ID cannot be blank");
        }

        this.poNumber = poNumber.trim();
        this.supplierId = supplierId.trim();
        this.status = Status.CREATED;
        this.lines = new ArrayList<>();
        this.createdBy = createdBy != null ? createdBy : "admin";
        this.createdAt = Instant.now();
    }

    @Override
    public String getId() {
        return poNumber;
    }

    @Override
    public String getStorageKey() {
        return "orders";
    }

    public synchronized void addLine(String sku, int quantity, double unitCost) {
        if (status != Status.CREATED) {
            throw new IllegalStateException("Cannot add line items to purchase order in status: " + status);
        }
        lines.add(new OrderLine(sku, quantity, unitCost));
    }

    public synchronized void markReceived() {
        if (this.status != Status.CREATED) {
            throw new IllegalStateException("Only CREATED purchase orders can be received. Current status: " + status);
        }
        this.status = Status.RECEIVED;
        this.receivedAt = Instant.now();
    }

    public synchronized void markCancelled() {
        if (this.status != Status.CREATED) {
            throw new IllegalStateException("Only CREATED purchase orders can be cancelled. Current status: " + status);
        }
        this.status = Status.CANCELLED;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public synchronized double calculateTotalCost() {
        return lines.stream().mapToDouble(l -> l.quantity() * l.unitCost()).sum();
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized Instant getReceivedAt() {
        return receivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseOrder that = (PurchaseOrder) o;
        return Objects.equals(poNumber, that.poNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(poNumber);
    }
}
