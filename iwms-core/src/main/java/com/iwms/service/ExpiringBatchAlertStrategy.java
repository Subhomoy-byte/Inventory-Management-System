package com.iwms.service;

import com.iwms.model.Alert;
import com.iwms.model.Batch;
import com.iwms.model.Product;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Design Pattern: Strategy Implementation 2.
 * Monitors batch expiration dates and raises urgency warnings for batches expiring within threshold days.
 */
public class ExpiringBatchAlertStrategy implements AlertStrategy {
    private final int thresholdDays;

    public ExpiringBatchAlertStrategy() {
        this(7);
    }

    public ExpiringBatchAlertStrategy(int thresholdDays) {
        this.thresholdDays = thresholdDays;
    }

    @Override
    public List<Alert> evaluate(Product product, int currentStock, List<Batch> batches) {
        List<Alert> alerts = new ArrayList<>();
        if (batches == null || batches.isEmpty()) {
            return alerts;
        }

        for (Batch batch : batches) {
            if (batch.getQuantity() <= 0) {
                continue;
            }

            if (batch.isExpired()) {
                alerts.add(new Alert(
                        "EXP-" + UUID.randomUUID().toString().substring(0, 8),
                        batch.getProductSku(),
                        String.format("EXPIRED: Batch '%s' of SKU '%s' expired on %s with %d units remaining",
                                batch.getBatchNumber(), batch.getProductSku(), batch.getExpiryDate(), batch.getQuantity()),
                        Alert.Severity.CRITICAL,
                        Instant.now()
                ));
            } else if (batch.getDaysUntilExpiration() <= thresholdDays) {
                alerts.add(new Alert(
                        "EXP-" + UUID.randomUUID().toString().substring(0, 8),
                        batch.getProductSku(),
                        String.format("EXPIRING SOON: Batch '%s' of SKU '%s' expires in %d days (%s) with %d units",
                                batch.getBatchNumber(), batch.getProductSku(), batch.getDaysUntilExpiration(), batch.getExpiryDate(), batch.getQuantity()),
                        Alert.Severity.CRITICAL,
                        Instant.now()
                ));
            }
        }

        return alerts;
    }
}
