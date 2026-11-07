package com.iwms.service;

import com.iwms.model.Product;
import com.iwms.model.ReorderSuggestion;

/**
 * Design Pattern: Strategy Pattern.
 * Evaluates replenishment suggestions based on target inventory models or velocity demand.
 */
public interface ReorderStrategy {
    ReorderSuggestion evaluateReorder(Product product, int currentStock, int dailyDemand);
}
