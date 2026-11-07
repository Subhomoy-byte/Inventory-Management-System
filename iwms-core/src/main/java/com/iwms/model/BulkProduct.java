package com.iwms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * OOP Principle: Inheritance & Polymorphism.
 * Specialization for heavy or large volume items stored on pallets or bulk bays.
 */
public class BulkProduct extends Product {
    private double weightKg;
    private double volumeCubicMeters;
    private String unitOfMeasure;

    private BulkProduct(Builder builder) {
        super(builder);
        this.weightKg = builder.weightKg;
        this.volumeCubicMeters = builder.volumeCubicMeters;
        this.unitOfMeasure = builder.unitOfMeasure != null ? builder.unitOfMeasure : "UNIT";
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public double calculateStorageCost() {
        // Floor space and structural load capacity cost
        return 2.00 + (volumeCubicMeters * 15.0) + (weightKg * 0.05);
    }

    @Override
    public List<String> getHandlingRules() {
        List<String> rules = new ArrayList<>();
        rules.add("Heavy bulk item: Operate via pallet jack or certified forklift only");
        rules.add("Confirm floor loading limits before staging in mezzanine zones");
        rules.add("Ensure shrink-wrap integrity on full-pallet movements");
        return rules;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public double getVolumeCubicMeters() {
        return volumeCubicMeters;
    }

    public void setVolumeCubicMeters(double volumeCubicMeters) {
        this.volumeCubicMeters = volumeCubicMeters;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public static class Builder extends Product.ProductBuilder<BulkProduct, Builder> {
        private double weightKg;
        private double volumeCubicMeters;
        private String unitOfMeasure = "UNIT";

        public Builder weightKg(double weightKg) {
            this.weightKg = weightKg;
            return this;
        }

        public Builder volumeCubicMeters(double volumeCubicMeters) {
            this.volumeCubicMeters = volumeCubicMeters;
            return this;
        }

        public Builder unitOfMeasure(String unitOfMeasure) {
            this.unitOfMeasure = unitOfMeasure;
            return this;
        }

        @Override
        public BulkProduct build() {
            if (category == null) {
                category = Category.BULK;
            }
            return new BulkProduct(this);
        }
    }
}
