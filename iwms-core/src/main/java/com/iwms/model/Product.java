package com.iwms.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * OOP Principle: Abstraction & Encapsulation.
 * Abstract base class for all inventory products.
 * Defines common state and enforces invariant validation.
 */
public abstract class Product implements Storable, Auditable, Exportable {
    // Encapsulation: Private state fields
    private final String sku;
    private String name;
    private Category category;
    private double unitPrice;
    private double costPrice;
    private int minStockLevel;
    private final String createdBy;
    private final Instant createdAt;

    protected Product(ProductBuilder<?, ?> builder) {
        if (builder.sku == null || builder.sku.isBlank()) {
            throw new IllegalArgumentException("Product SKU cannot be null or blank");
        }
        if (builder.name == null || builder.name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (builder.category == null) {
            throw new IllegalArgumentException("Product category cannot be null");
        }
        if (builder.unitPrice < 0) {
            throw new IllegalArgumentException("Product unit price cannot be negative: " + builder.unitPrice);
        }
        if (builder.costPrice < 0) {
            throw new IllegalArgumentException("Product cost price cannot be negative: " + builder.costPrice);
        }
        if (builder.minStockLevel < 0) {
            throw new IllegalArgumentException("Product minimum stock level cannot be negative: " + builder.minStockLevel);
        }

        this.sku = builder.sku.trim();
        this.name = builder.name.trim();
        this.category = builder.category;
        this.unitPrice = builder.unitPrice;
        this.costPrice = builder.costPrice;
        this.minStockLevel = builder.minStockLevel;
        this.createdBy = builder.createdBy != null ? builder.createdBy : "system";
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
    }

    // Abstraction: polymorphic methods to be realized by subclasses
    public abstract double calculateStorageCost();
    public abstract List<String> getHandlingRules();

    @Override
    public String getId() {
        return sku;
    }

    @Override
    public String getStorageKey() {
        return "products";
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }
        this.name = name.trim();
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = Objects.requireNonNull(category, "Category cannot be null");
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        this.unitPrice = unitPrice;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        if (costPrice < 0) {
            throw new IllegalArgumentException("Cost price cannot be negative");
        }
        this.costPrice = costPrice;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        if (minStockLevel < 0) {
            throw new IllegalArgumentException("Minimum stock level cannot be negative");
        }
        this.minStockLevel = minStockLevel;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String[] toCsvRow() {
        return new String[]{
                sku,
                name,
                category.name(),
                String.format("%.2f", unitPrice),
                String.format("%.2f", costPrice),
                String.valueOf(minStockLevel)
        };
    }

    public static String[] getCsvHeaders() {
        return new String[]{"SKU", "Name", "Category", "UnitPrice", "CostPrice", "MinStockLevel"};
    }

    /**
     * Design Pattern: Builder Pattern with generic self-type recursion.
     */
    public abstract static class ProductBuilder<C extends Product, B extends ProductBuilder<C, B>> {
        protected String sku;
        protected String name;
        protected Category category;
        protected double unitPrice;
        protected double costPrice;
        protected int minStockLevel;
        protected String createdBy;
        protected Instant createdAt;

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B sku(String sku) {
            this.sku = sku;
            return self();
        }

        public B name(String name) {
            this.name = name;
            return self();
        }

        public B category(Category category) {
            this.category = category;
            return self();
        }

        public B unitPrice(double unitPrice) {
            this.unitPrice = unitPrice;
            return self();
        }

        public B costPrice(double costPrice) {
            this.costPrice = costPrice;
            return self();
        }

        public B minStockLevel(int minStockLevel) {
            this.minStockLevel = minStockLevel;
            return self();
        }

        public B createdBy(String createdBy) {
            this.createdBy = createdBy;
            return self();
        }

        public B createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return self();
        }

        public abstract C build();
    }
}
