package com.iwms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * OOP Principle: Inheritance & Polymorphism.
 * Specialization for perishable items requiring expiration monitoring and cold-chain storage.
 */
public class PerishableProduct extends Product {
    private int shelfLifeDays;
    private double storageTempCelsius;
    private boolean requiresRefrigeration;

    private PerishableProduct(Builder builder) {
        super(builder);
        this.shelfLifeDays = builder.shelfLifeDays;
        this.storageTempCelsius = builder.storageTempCelsius;
        this.requiresRefrigeration = builder.requiresRefrigeration;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public double calculateStorageCost() {
        double baseCost = 2.50; // standard base storage rate
        if (requiresRefrigeration || storageTempCelsius < 8.0) {
            baseCost += 4.00; // cooling energy surcharge
        }
        return baseCost + (shelfLifeDays < 15 ? 1.50 : 0.50);
    }

    @Override
    public List<String> getHandlingRules() {
        List<String> rules = new ArrayList<>();
        rules.add("Strictly enforce First-Expired-First-Out (FEFO) picking order");
        if (requiresRefrigeration) {
            rules.add(String.format("Refrigerated storage required: Maintain %.1f°C (±1°C)", storageTempCelsius));
        }
        rules.add("Inspect packaging for seal breaches upon receipt and dispatch");
        return rules;
    }

    public int getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void setShelfLifeDays(int shelfLifeDays) {
        if (shelfLifeDays <= 0) {
            throw new IllegalArgumentException("Shelf life must be positive");
        }
        this.shelfLifeDays = shelfLifeDays;
    }

    public double getStorageTempCelsius() {
        return storageTempCelsius;
    }

    public void setStorageTempCelsius(double storageTempCelsius) {
        this.storageTempCelsius = storageTempCelsius;
    }

    public boolean isRequiresRefrigeration() {
        return requiresRefrigeration;
    }

    public void setRequiresRefrigeration(boolean requiresRefrigeration) {
        this.requiresRefrigeration = requiresRefrigeration;
    }

    public static class Builder extends Product.ProductBuilder<PerishableProduct, Builder> {
        private int shelfLifeDays;
        private double storageTempCelsius;
        private boolean requiresRefrigeration;

        public Builder shelfLifeDays(int shelfLifeDays) {
            this.shelfLifeDays = shelfLifeDays;
            return this;
        }

        public Builder storageTempCelsius(double storageTempCelsius) {
            this.storageTempCelsius = storageTempCelsius;
            return this;
        }

        public Builder requiresRefrigeration(boolean requiresRefrigeration) {
            this.requiresRefrigeration = requiresRefrigeration;
            return this;
        }

        @Override
        public PerishableProduct build() {
            if (category == null) {
                category = Category.PERISHABLE;
            }
            return new PerishableProduct(this);
        }
    }
}
