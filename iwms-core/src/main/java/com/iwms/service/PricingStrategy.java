package com.iwms.service;

/**
 * Design Pattern: Strategy Pattern.
 * Evaluates inventory valuation and Cost of Goods Sold (COGS).
 */
public interface PricingStrategy {
    double calculateCostOfGoodsSold(int quantitySold, double unitCost);
}
