package com.iwms.service;

import com.iwms.model.Batch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Design Pattern: Strategy Implementation 2.
 * First-In-First-Out (FIFO): Prioritizes batches manufactured or received earliest.
 * Standard for general merchandise, electronics, and durable goods.
 */
public class FifoPickStrategy implements PickStrategy {

    @Override
    public List<Batch> selectBatches(List<Batch> availableBatches, int quantityNeeded) {
        if (availableBatches == null || availableBatches.isEmpty() || quantityNeeded <= 0) {
            return List.of();
        }

        // Sort by manufactureDate ascending
        List<Batch> sorted = new ArrayList<>(availableBatches);
        sorted.sort(Comparator.comparing(Batch::getManufactureDate, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Batch> selected = new ArrayList<>();
        int accumulated = 0;

        for (Batch batch : sorted) {
            if (batch.getQuantity() <= 0) {
                continue;
            }
            selected.add(batch);
            accumulated += batch.getQuantity();
            if (accumulated >= quantityNeeded) {
                break;
            }
        }

        return selected;
    }
}
