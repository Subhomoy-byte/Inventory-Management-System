package com.iwms.service;

import com.iwms.model.Alert;
import com.iwms.model.Batch;
import com.iwms.model.Product;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Design Pattern: Strategy Implementation 1.
 * Monitors quantity thresholds and raises alerts when stock falls below min levels.
 */
public class LowStockAlertStrategy implements AlertStrategy {

    @Override
    public List<Alert> evaluate(Product product, int currentStock, List<Batch> batches) {
        List<Alert> alerts = new ArrayList<>();
        if (product == null) {
            return alerts;
        }

        if (currentStock <= 0) {
            alerts.add(new Alert(
                    "ALERT-" + UUID.randomUUID().toString().substring(0, 8),
                    product.getSku(),
                    String.format("CRITICAL: Product '%s' (%s) is completely OUT OF STOCK!", product.getName(), product.getSku()),
                    Alert.Severity.CRITICAL,
                    Instant.now()
            ));
        } else if (currentStock <= product.getMinStockLevel()) {
            alerts.add(new Alert(
                    "ALERT-" + UUID.randomUUID().toString().substring(0, 8),
                    product.getSku(),
                    String.format("Product '%s' stock (%d) is below minimum threshold (%d)", product.getName(), currentStock, product.getMinStockLevel()),
                    Alert.Severity.HIGH,
                    Instant.now()
            ));
        }

        return alerts;
    }
}
