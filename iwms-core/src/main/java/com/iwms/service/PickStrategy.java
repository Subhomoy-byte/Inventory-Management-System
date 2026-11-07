package com.iwms.service;

import com.iwms.model.Batch;

import java.util.List;

/**
 * Design Pattern: Strategy Pattern.
 * Interface defining batch selection strategies for outbound stock operations.
 */
public interface PickStrategy {
    /**
     * Selects and sequences batches to fulfill the required quantity.
     *
     * @param availableBatches List of currently available candidate batches
     * @param quantityNeeded Total units required for fulfillment
     * @return Ordered list of batches to be picked
     */
    List<Batch> selectBatches(List<Batch> availableBatches, int quantityNeeded);
}
