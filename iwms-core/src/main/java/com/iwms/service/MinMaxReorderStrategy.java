package com.iwms.service;

import com.iwms.model.Product;
import com.iwms.model.ReorderSuggestion;

/**
 * Design Pattern: Strategy Implementation 1.
 * Replenishes inventory back up to maximum target capacity whenever stock falls to min level.
 */
public class MinMaxReorderStrategy implements ReorderStrategy {
    private final int targetMaxStock;

    public MinMaxReorderStrategy(int targetMaxStock) {
        if (targetMaxStock <= 0) {
            throw new IllegalArgumentException("Target max stock must be positive: " + targetMaxStock);
        }
        this.targetMaxStock = targetMaxStock;
    }

    @Override
    public ReorderSuggestion evaluateReorder(Product product, int currentStock, int dailyDemand) {
        if (product == null) {
            return null;
        }

        if (currentStock <= product.getMinStockLevel()) {
            int toOrder = Math.max(0, targetMaxStock - currentStock);
            int urgency = currentStock == 0 ? 5 : 3;
            String rationale = String.format("Stock (%d) is at/below min (%d). Replenish to target max (%d).",
                    currentStock, product.getMinStockLevel(), targetMaxStock);
            return new ReorderSuggestion(product.getSku(), toOrder, rationale, urgency);
        }

        return null; // No reorder needed
    }
}
