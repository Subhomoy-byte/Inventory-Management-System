package com.iwms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * OOP Principle: Inheritance & Polymorphism.
 * Specialization for fragile items requiring single-tier shelving and cushioning.
 */
public class FragileProduct extends Product {
    private int fragilityIndex; // 1 to 10
    private int maxStackHeight;

    private FragileProduct(Builder builder) {
        super(builder);
        this.fragilityIndex = builder.fragilityIndex;
        this.maxStackHeight = builder.maxStackHeight;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public double calculateStorageCost() {
        double cost = 3.50; // fragile baseline
        // Higher fragility index increases floor footprint surcharge (no stacking)
        cost += (fragilityIndex * 0.40);
        if (maxStackHeight <= 1) {
            cost += 2.00; // single tier dedicated shelf surcharge
        }
        return cost;
    }

    @Override
    public List<String> getHandlingRules() {
        List<String> rules = new ArrayList<>();
        rules.add("Fragile merchandise: Do not drop, throw, or tilt");
        rules.add(String.format("Maximum allowed stack height: %d unit(s)", maxStackHeight));
        rules.add("Use reinforced bubble wrap cushioning and corner guards during pack-out");
        return rules;
    }

    public int getFragilityIndex() {
        return fragilityIndex;
    }

    public void setFragilityIndex(int fragilityIndex) {
        this.fragilityIndex = fragilityIndex;
    }

    public int getMaxStackHeight() {
        return maxStackHeight;
    }

    public void setMaxStackHeight(int maxStackHeight) {
        this.maxStackHeight = maxStackHeight;
    }

    public static class Builder extends Product.ProductBuilder<FragileProduct, Builder> {
        private int fragilityIndex = 5;
        private int maxStackHeight = 2;

        public Builder fragilityIndex(int fragilityIndex) {
            this.fragilityIndex = fragilityIndex;
            return this;
        }

        public Builder maxStackHeight(int maxStackHeight) {
            this.maxStackHeight = maxStackHeight;
            return this;
        }

        @Override
        public FragileProduct build() {
            if (category == null) {
                category = Category.FRAGILE;
            }
            return new FragileProduct(this);
        }
    }
}
