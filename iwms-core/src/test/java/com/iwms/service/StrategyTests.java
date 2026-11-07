package com.iwms.service;

import com.iwms.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Strategy Interfaces & Implementations (Design Patterns Showcase)")
class StrategyTests {

    @Test
    @DisplayName("Strategy Pattern: FefoPickStrategy picks batches with earliest expiry first")
    void testFefoPickStrategy() {
        PickStrategy fefo = new FefoPickStrategy();

        LocalDate today = LocalDate.now();
        Batch b1 = new Batch("B1", "MILK", today.minusDays(5), today.plusDays(10), 50);
        Batch b2 = new Batch("B2", "MILK", today.minusDays(2), today.plusDays(3), 30); // expires sooner!
        Batch b3 = new Batch("B3", "MILK", today.minusDays(1), today.plusDays(20), 40);

        List<Batch> selected = fefo.selectBatches(List.of(b1, b2, b3), 40);

        // B2 expires first (in 3 days), so it should be picked first
        assertEquals(2, selected.size(), "Should select 2 batches to satisfy quantity of 40");
        assertEquals("B2", selected.get(0).getBatchNumber(), "First picked batch must be B2 (earliest expiry)");
        assertEquals("B1", selected.get(1).getBatchNumber(), "Second picked batch must be B1");
    }

    @Test
    @DisplayName("Strategy Pattern: FifoPickStrategy picks batches with earliest manufacture date first")
    void testFifoPickStrategy() {
        PickStrategy fifo = new FifoPickStrategy();

        LocalDate today = LocalDate.now();
        Batch b1 = new Batch("B1", "WIDGET", today.minusDays(15), today.plusDays(100), 20); // manufactured first
        Batch b2 = new Batch("B2", "WIDGET", today.minusDays(5), today.plusDays(20), 50);
        Batch b3 = new Batch("B3", "WIDGET", today.minusDays(1), today.plusDays(50), 30);

        List<Batch> selected = fifo.selectBatches(List.of(b2, b3, b1), 25);

        assertEquals(2, selected.size());
        assertEquals("B1", selected.get(0).getBatchNumber(), "First picked batch must be B1 (oldest mfg date)");
        assertEquals("B2", selected.get(1).getBatchNumber(), "Second picked batch must be B2");
    }

    @Test
    @DisplayName("Strategy Pattern: LowStockAlertStrategy raises urgency alert when stock is below threshold")
    void testLowStockAlertStrategy() {
        AlertStrategy strategy = new LowStockAlertStrategy();

        Product product = PerishableProduct.builder()
                .sku("SKU-LOW")
                .name("Butter")
                .unitPrice(5.0)
                .costPrice(3.0)
                .minStockLevel(25)
                .shelfLifeDays(30)
                .build();

        // Current stock 10 is well below min 25
        List<Alert> alerts = strategy.evaluate(product, 10, List.of());

        assertFalse(alerts.isEmpty(), "Alert should be generated for low stock");
        Alert alert = alerts.get(0);
        assertEquals(Alert.Severity.HIGH, alert.severity());
        assertTrue(alert.message().contains("below minimum threshold"));
    }

    @Test
    @DisplayName("Strategy Pattern: ExpiringBatchAlertStrategy alerts for batches expiring within threshold")
    void testExpiringBatchAlertStrategy() {
        AlertStrategy strategy = new ExpiringBatchAlertStrategy(7); // alert if <= 7 days

        Product product = PerishableProduct.builder()
                .sku("SKU-YOGURT")
                .name("Greek Yogurt")
                .unitPrice(2.50)
                .costPrice(1.20)
                .minStockLevel(10)
                .shelfLifeDays(14)
                .build();

        LocalDate today = LocalDate.now();
        Batch expiringSoon = new Batch("EXP-1", "SKU-YOGURT", today.minusDays(10), today.plusDays(3), 15);
        Batch fresh = new Batch("EXP-2", "SKU-YOGURT", today, today.plusDays(20), 40);

        List<Alert> alerts = strategy.evaluate(product, 55, List.of(expiringSoon, fresh));

        assertEquals(1, alerts.size());
        assertEquals(Alert.Severity.CRITICAL, alerts.get(0).severity());
        assertTrue(alerts.get(0).message().contains("EXP-1"));
    }

    @Test
    @DisplayName("Strategy Pattern: ReorderStrategy evaluates replenishment suggestions")
    void testReorderStrategies() {
        Product product = BulkProduct.builder()
                .sku("BULK-STEEL")
                .name("Steel Rods")
                .unitPrice(100.0)
                .costPrice(60.0)
                .minStockLevel(50)
                .weightKg(200.0)
                .volumeCubicMeters(0.5)
                .build();

        // 1. MinMax Reorder Strategy (replenish up to target max level of 100)
        ReorderStrategy minMax = new MinMaxReorderStrategy(100);
        ReorderSuggestion suggestion1 = minMax.evaluateReorder(product, 30, 5);
        assertNotNull(suggestion1);
        assertEquals(70, suggestion1.suggestedQuantity(), "MinMax should order 100 - 30 = 70 units");

        // 2. Velocity Reorder Strategy (based on daily demand and lead time days)
        ReorderStrategy velocity = new VelocityReorderStrategy(14, 5); // 14 lead days, 5 safety days
        // Demand 4/day * 19 days cover = 76 target - 30 current = 46 suggested
        ReorderSuggestion suggestion2 = velocity.evaluateReorder(product, 30, 4);
        assertNotNull(suggestion2);
        assertTrue(suggestion2.suggestedQuantity() > 0);
    }

    @Test
    @DisplayName("Strategy Pattern: PricingStrategy evaluates cost of goods sold")
    void testPricingStrategies() {
        // Average Cost Pricing Strategy
        PricingStrategy avgCost = new AverageCostPricingStrategy();
        double cogs1 = avgCost.calculateCostOfGoodsSold(10, 4.50);
        assertEquals(45.00, cogs1, 0.001);

        // FIFO Pricing Strategy with batch cost breakdown
        PricingStrategy fifoPricing = new FifoPricingStrategy();
        double cogs2 = fifoPricing.calculateCostOfGoodsSold(10, 5.00);
        assertEquals(50.00, cogs2, 0.001);
    }
}
