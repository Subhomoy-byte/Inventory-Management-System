package com.iwms.service;

/**
 * Design Pattern: Strategy Implementation 1.
 * Weighted average cost inventory valuation method.
 */
public class AverageCostPricingStrategy implements PricingStrategy {

    @Override
    public double calculateCostOfGoodsSold(int quantitySold, double unitCost) {
        if (quantitySold <= 0 || unitCost <= 0) {
            return 0.0;
        }
        return quantitySold * unitCost;
    }
}
