package com.iwms.service;

import com.iwms.model.Product;
import com.iwms.model.ReorderSuggestion;

/**
 * Design Pattern: Strategy Implementation 2.
 * Calculates dynamic days-of-cover based on historical sales velocity and supplier lead times.
 */
public class VelocityReorderStrategy implements ReorderStrategy {
    private final int supplierLeadDays;
    private final int safetyBufferDays;

    public VelocityReorderStrategy(int supplierLeadDays, int safetyBufferDays) {
        this.supplierLeadDays = Math.max(1, supplierLeadDays);
        this.safetyBufferDays = Math.max(1, safetyBufferDays);
    }

    @Override
    public ReorderSuggestion evaluateReorder(Product product, int currentStock, int dailyDemand) {
        if (product == null || dailyDemand <= 0) {
            return null;
        }

        int totalCoverDays = supplierLeadDays + safetyBufferDays;
        int targetInventory = dailyDemand * totalCoverDays;

        if (currentStock < targetInventory) {
            int toOrder = targetInventory - currentStock;
            int daysRemaining = currentStock / dailyDemand;
            int urgency = daysRemaining <= supplierLeadDays ? 5 : 2;
            String rationale = String.format("Current stock provides %d days cover. Target cover is %d days based on velocity %d/day.",
                    daysRemaining, totalCoverDays, dailyDemand);
            return new ReorderSuggestion(product.getSku(), toOrder, rationale, urgency);
        }

        return null;
    }
}
