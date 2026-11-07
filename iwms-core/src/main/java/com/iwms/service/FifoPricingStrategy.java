package com.iwms.service;

/**
 * Design Pattern: Strategy Implementation 2.
 * First-In-First-Out (FIFO) cost of goods sold calculation.
 */
public class FifoPricingStrategy implements PricingStrategy {

    @Override
    public double calculateCostOfGoodsSold(int quantitySold, double unitCost) {
        if (quantitySold <= 0 || unitCost <= 0) {
            return 0.0;
        }
        return quantitySold * unitCost;
    }
}
