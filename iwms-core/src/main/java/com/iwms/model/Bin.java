package com.iwms.model;

import java.util.Objects;

/**
 * OOP Principle: Encapsulation.
 * Represents a physical storage bin within a warehouse zone.
 * Manages physical capacity limits and fill percentage calculations.
 */
public class Bin {
    private final String id;
    private final String code;
    private final int capacityQuantity;
    private final double maxWeightKg;
    private int currentQuantity;
    private double currentWeightKg;

    public Bin(String id, String code, int capacityQuantity, double maxWeightKg) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Bin ID cannot be blank");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Bin code cannot be blank");
        }
        if (capacityQuantity <= 0) {
            throw new IllegalArgumentException("Bin capacity must be positive: " + capacityQuantity);
        }
        if (maxWeightKg <= 0) {
            throw new IllegalArgumentException("Bin max weight must be positive: " + maxWeightKg);
        }

        this.id = id.trim();
        this.code = code.trim();
        this.capacityQuantity = capacityQuantity;
        this.maxWeightKg = maxWeightKg;
        this.currentQuantity = 0;
        this.currentWeightKg = 0.0;
    }

    public synchronized double calculateFillPercentage() {
        return Math.min(100.0, ((double) currentQuantity / capacityQuantity) * 100.0);
    }

    public synchronized boolean canAccommodate(int additionalQuantity, double additionalWeight) {
        return (this.currentQuantity + additionalQuantity <= capacityQuantity) &&
               (this.currentWeightKg + additionalWeight <= maxWeightKg);
    }

    public synchronized void addStock(int quantity, double weight) {
        if (this.currentQuantity + quantity > capacityQuantity) {
            throw new IllegalArgumentException(String.format("Bin '%s' capacity exceeded: max %d, attempting to hold %d",
                    id, capacityQuantity, this.currentQuantity + quantity));
        }
        this.currentQuantity += quantity;
        this.currentWeightKg += weight;
    }

    public synchronized void removeStock(int quantity, double weight) {
        if (this.currentQuantity - quantity < 0) {
            throw new IllegalArgumentException(String.format("Cannot remove %d units from bin '%s'; only %d present",
                    quantity, id, this.currentQuantity));
        }
        this.currentQuantity -= quantity;
        this.currentWeightKg = Math.max(0.0, this.currentWeightKg - weight);
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public int getCapacityQuantity() {
        return capacityQuantity;
    }

    public double getMaxWeightKg() {
        return maxWeightKg;
    }

    public synchronized int getCurrentQuantity() {
        return currentQuantity;
    }

    public synchronized double getCurrentWeightKg() {
        return currentWeightKg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bin bin = (Bin) o;
        return Objects.equals(id, bin.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
