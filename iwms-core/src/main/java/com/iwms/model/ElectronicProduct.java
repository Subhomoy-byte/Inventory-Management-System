package com.iwms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * OOP Principle: Inheritance & Polymorphism.
 * Specialization for electronic hardware requiring climate and static (ESD) controls.
 */
public class ElectronicProduct extends Product {
    private int warrantyMonths;
    private int voltage;
    private boolean hasLithiumBattery;

    private ElectronicProduct(Builder builder) {
        super(builder);
        this.warrantyMonths = builder.warrantyMonths;
        this.voltage = builder.voltage;
        this.hasLithiumBattery = builder.hasLithiumBattery;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public double calculateStorageCost() {
        double cost = 3.00; // base rate
        cost += 1.50; // climate control / anti-humidity
        if (hasLithiumBattery) {
            cost += 2.00; // battery hazmat segregation surcharge
        }
        return cost;
    }

    @Override
    public List<String> getHandlingRules() {
        List<String> rules = new ArrayList<>();
        rules.add("Handle with Electrostatic Discharge (ESD) wrist straps and antistatic mats");
        rules.add("Maintain climate-controlled, low-moisture storage (<50% relative humidity)");
        if (hasLithiumBattery) {
            rules.add("Lithium battery precautions: Keep away from direct sunlight and heat sources");
        }
        return rules;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public int getVoltage() {
        return voltage;
    }

    public void setVoltage(int voltage) {
        this.voltage = voltage;
    }

    public boolean isHasLithiumBattery() {
        return hasLithiumBattery;
    }

    public void setHasLithiumBattery(boolean hasLithiumBattery) {
        this.hasLithiumBattery = hasLithiumBattery;
    }

    public static class Builder extends Product.ProductBuilder<ElectronicProduct, Builder> {
        private int warrantyMonths;
        private int voltage;
        private boolean hasLithiumBattery;

        public Builder warrantyMonths(int warrantyMonths) {
            this.warrantyMonths = warrantyMonths;
            return this;
        }

        public Builder voltage(int voltage) {
            this.voltage = voltage;
            return this;
        }

        public Builder hasLithiumBattery(boolean hasLithiumBattery) {
            this.hasLithiumBattery = hasLithiumBattery;
            return this;
        }

        @Override
        public ElectronicProduct build() {
            if (category == null) {
                category = Category.ELECTRONICS;
            }
            return new ElectronicProduct(this);
        }
    }
}
