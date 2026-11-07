package com.iwms.service;

import com.iwms.model.Batch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Design Pattern: Strategy Implementation 1.
 * First-Expired-First-Out (FEFO): Prioritizes batches nearing expiration.
 * Essential for perishable foods, pharmaceuticals, and date-sensitive inventory.
 */
public class FefoPickStrategy implements PickStrategy {

    @Override
    public List<Batch> selectBatches(List<Batch> availableBatches, int quantityNeeded) {
        if (availableBatches == null || availableBatches.isEmpty() || quantityNeeded <= 0) {
            return List.of();
        }

        // Sort by expiryDate ascending, placing null expiry at the end
        List<Batch> sorted = new ArrayList<>(availableBatches);
        sorted.sort(Comparator.comparing(Batch::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())));

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
