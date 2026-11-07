package com.iwms.model;

import com.iwms.exception.DuplicateSkuException;
import com.iwms.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Hierarchy & Core Model Tests (OOP Demonstration)")
class ProductHierarchyTest {

    @Test
    @DisplayName("Product Builder enforces Encapsulation and validates input parameters")
    void testProductBuilderValidation() {
        // Blank SKU should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                PerishableProduct.builder()
                        .sku("")
                        .name("Fresh Milk")
                        .category(Category.PERISHABLE)
                        .unitPrice(4.50)
                        .costPrice(2.50)
                        .minStockLevel(10)
                        .shelfLifeDays(7)
                        .storageTempCelsius(4.0)
                        .build()
        );

        // Negative Unit Price should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                ElectronicProduct.builder()
                        .sku("ELEC-101")
                        .name("Wireless Router")
                        .category(Category.ELECTRONICS)
                        .unitPrice(-50.0)
                        .costPrice(30.0)
                        .minStockLevel(5)
                        .warrantyMonths(24)
                        .voltage(220)
                        .build()
        );

        // Negative Min Stock Level should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                FragileProduct.builder()
                        .sku("FRAG-001")
                        .name("Glass Vase")
                        .category(Category.FRAGILE)
                        .unitPrice(25.0)
                        .costPrice(10.0)
                        .minStockLevel(-1)
                        .fragilityIndex(8)
                        .maxStackHeight(2)
                        .build()
        );
    }

    @Test
    @DisplayName("Polymorphism: PerishableProduct calculates refrigeration cost and FEFO handling rules")
    void testPerishableProductPolymorphism() {
        PerishableProduct milk = PerishableProduct.builder()
                .sku("PER-001")
                .name("Organic Whole Milk")
                .category(Category.PERISHABLE)
                .unitPrice(3.99)
                .costPrice(2.10)
                .minStockLevel(20)
                .shelfLifeDays(10)
                .storageTempCelsius(3.5)
                .requiresRefrigeration(true)
                .createdBy("admin")
                .build();

        assertEquals("PER-001", milk.getSku());
        assertEquals(Category.PERISHABLE, milk.getCategory());
        assertTrue(milk.calculateStorageCost() > 0, "Perishable storage cost must be greater than base rate");
        
        List<String> rules = milk.getHandlingRules();
        assertNotNull(rules);
        assertTrue(rules.stream().anyMatch(r -> r.contains("FEFO") || r.contains("Refrigerated")),
                "Perishable rules must mention cold storage or FEFO");
    }

    @Test
    @DisplayName("Polymorphism: ElectronicProduct calculates ESD climate storage and grounding rules")
    void testElectronicProductPolymorphism() {
        ElectronicProduct router = ElectronicProduct.builder()
                .sku("ELEC-002")
                .name("Gigabit Switch")
                .category(Category.ELECTRONICS)
                .unitPrice(120.00)
                .costPrice(75.00)
                .minStockLevel(5)
                .warrantyMonths(36)
                .voltage(110)
                .hasLithiumBattery(false)
                .createdBy("admin")
                .build();

        assertTrue(router.calculateStorageCost() > 0);
        List<String> rules = router.getHandlingRules();
        assertTrue(rules.stream().anyMatch(r -> r.toLowerCase().contains("esd") || r.toLowerCase().contains("static") || r.toLowerCase().contains("moisture")),
                "Electronic rules must mention ESD, static or moisture precautions");
    }

    @Test
    @DisplayName("Polymorphism: FragileProduct adds fragility surcharge and stacking limits")
    void testFragileProductPolymorphism() {
        FragileProduct glass = FragileProduct.builder()
                .sku("FRAG-002")
                .name("Crystal Decanter")
                .category(Category.FRAGILE)
                .unitPrice(80.00)
                .costPrice(35.00)
                .minStockLevel(4)
                .fragilityIndex(9)
                .maxStackHeight(1)
                .createdBy("admin")
                .build();

        assertTrue(glass.calculateStorageCost() > 0);
        List<String> rules = glass.getHandlingRules();
        assertTrue(rules.stream().anyMatch(r -> r.toLowerCase().contains("fragile") || r.toLowerCase().contains("cushion")),
                "Fragile rules must mention fragile warning or cushioning");
    }

    @Test
    @DisplayName("Polymorphism: BulkProduct calculates cost based on volume and weight")
    void testBulkProductPolymorphism() {
        BulkProduct cement = BulkProduct.builder()
                .sku("BULK-001")
                .name("Portland Cement")
                .category(Category.BULK)
                .unitPrice(15.00)
                .costPrice(8.00)
                .minStockLevel(50)
                .weightKg(50.0)
                .volumeCubicMeters(0.04)
                .unitOfMeasure("BAG")
                .createdBy("admin")
                .build();

        assertEquals(50.0, cement.getWeightKg());
        assertEquals("BAG", cement.getUnitOfMeasure());
        assertTrue(cement.calculateStorageCost() > 0);
        List<String> rules = cement.getHandlingRules();
        assertTrue(rules.stream().anyMatch(r -> r.toLowerCase().contains("pallet") || r.toLowerCase().contains("forklift")),
                "Bulk product rules must mention pallet or heavy machinery requirements");
    }

    @Test
    @DisplayName("Batch model validates quantities and tracks expiration date")
    void testBatchModel() {
        LocalDate today = LocalDate.now();
        LocalDate expiry = today.plusDays(30);

        Batch batch = new Batch("BATCH-2026-A", "PER-001", today, expiry, 100);

        assertEquals("BATCH-2026-A", batch.getBatchNumber());
        assertEquals("PER-001", batch.getProductSku());
        assertEquals(100, batch.getQuantity());
        assertFalse(batch.isExpired(), "Batch expiring in 30 days should not be expired");

        // Verify invalid quantity
        assertThrows(IllegalArgumentException.class, () ->
                new Batch("BATCH-NEG", "PER-001", today, expiry, -10));
    }

    @Test
    @DisplayName("StockMovement model is immutable and records acting user")
    void testStockMovementImmutability() {
        Instant now = Instant.now();
        StockMovement movement = new StockMovement(
                "MOV-1001",
                now,
                "PER-001",
                "BATCH-2026-A",
                MovementType.IN,
                50,
                null,
                "BIN-A1-01",
                "Initial supplier delivery",
                "manager_john"
        );

        assertEquals("MOV-1001", movement.movementId());
        assertEquals(MovementType.IN, movement.type());
        assertEquals(50, movement.quantity());
        assertEquals("manager_john", movement.actingUsername());
        assertEquals("BIN-A1-01", movement.toBinId());
    }
}
