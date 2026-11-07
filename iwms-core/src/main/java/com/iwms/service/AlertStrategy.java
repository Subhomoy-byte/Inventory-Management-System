package com.iwms.service;

import com.iwms.model.Alert;
import com.iwms.model.Batch;
import com.iwms.model.Product;

import java.util.List;

/**
 * Design Pattern: Strategy Pattern.
 * Interface defining inventory condition monitoring and alert generation strategies.
 */
public interface AlertStrategy {
    List<Alert> evaluate(Product product, int currentStock, List<Batch> batches);
}
