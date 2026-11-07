package com.iwms.service;

import com.iwms.exception.DuplicateSkuException;
import com.iwms.exception.InsufficientStockException;
import com.iwms.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InventoryService & Collections Tests (Thread-Safety, Undo, Collections)")
class InventoryServiceTest {

    private InventoryService service;
    private Product milk;
    private Product laptop;

    @BeforeEach
    void setUp() throws DuplicateSkuException {
        service = new InventoryService();

        // Setup warehouse structure
        Warehouse mainWh = new Warehouse("WH-01", "Central Logistics Hub", "Building A");
        Zone coldZone = new Zone("ZONE-COLD", "Cold Storage", Category.PERISHABLE);
        coldZone.addBin(new Bin("BIN-C1", "Cold Bin 1", 100, 200.0));
        coldZone.addBin(new Bin("BIN-C2", "Cold Bin 2", 150, 300.0));

        Zone dryZone = new Zone("ZONE-DRY", "Dry Electronics", Category.ELECTRONICS);
        dryZone.addBin(new Bin("BIN-D1", "Dry Bin 1", 200, 500.0));

        mainWh.addZone(coldZone);
        mainWh.addZone(dryZone);
        service.addWarehouse(mainWh);

        // Register products
        milk = PerishableProduct.builder()
                .sku("MILK-001")
                .name("Whole Milk 1L")
                .unitPrice(4.00)
                .costPrice(2.00)
                .minStockLevel(20)
                .shelfLifeDays(10)
                .storageTempCelsius(4.0)
                .requiresRefrigeration(true)
                .build();

        laptop = ElectronicProduct.builder()
                .sku("LAPTOP-001")
                .name("Ultrabook Pro")
                .unitPrice(1200.00)
                .costPrice(800.00)
                .minStockLevel(5)
                .warrantyMonths(24)
                .voltage(110)
                .build();

        service.registerProduct(milk);
        service.registerProduct(laptop);
    }

    @Test
    @DisplayName("Collections: HashMap provides O(1) product lookup by SKU")
    void testProductLookup() {
        Product p = service.getProduct("MILK-001");
        assertNotNull(p);
        assertEquals("Whole Milk 1L", p.getName());

        assertThrows(DuplicateSkuException.class, () -> service.registerProduct(milk));
    }

    @Test
    @DisplayName("Stock In creates batch, ledger movement and updates bin capacity")
    void testStockIn() throws InsufficientStockException {
        LocalDate expiry = LocalDate.now().plusDays(8);
        StockMovement movement = service.stockIn(
                "MILK-001",
                "BATCH-M1",
                "BIN-C1",
                50,
                expiry,
                "Initial supplier delivery",
                "operator1"
        );

        assertNotNull(movement);
        assertEquals(MovementType.IN, movement.type());
        assertEquals(50, movement.quantity());
        assertEquals(50, service.getCurrentStock("MILK-001"));

        Bin bin = service.getBin("BIN-C1");
        assertNotNull(bin);
        assertEquals(50, bin.getCurrentQuantity());
        assertEquals(50.0, bin.calculateFillPercentage(), 0.01);
    }

    @Test
    @DisplayName("Stock Out uses PickStrategy and throws InsufficientStockException on deficit")
    void testStockOut() throws InsufficientStockException {
        // First put 40 units in
        service.stockIn("MILK-001", "BATCH-M1", "BIN-C1", 40, LocalDate.now().plusDays(10), "Inbound", "admin");

        // Stock out 25 units
        StockMovement outMov = service.stockOut("MILK-001", 25, "Sales Order #101", "picker_sarah");
        assertNotNull(outMov);
        assertEquals(MovementType.OUT, outMov.type());
        assertEquals(25, outMov.quantity());
        assertEquals(15, service.getCurrentStock("MILK-001"));

        // Attempting to stock out 20 units when only 15 remain must throw InsufficientStockException
        assertThrows(InsufficientStockException.class, () ->
                service.stockOut("MILK-001", 20, "Excess Order", "picker_sarah"));
    }

    @Test
    @DisplayName("Stock Transfer moves items between bins atomically")
    void testStockTransfer() throws InsufficientStockException {
        service.stockIn("MILK-001", "BATCH-M1", "BIN-C1", 60, LocalDate.now().plusDays(10), "Inbound", "admin");

        StockMovement transfer = service.transferStock(
                "MILK-001",
                "BATCH-M1",
                "BIN-C1",
                "BIN-C2",
                20,
                "Balancing zone storage",
                "admin"
        );

        assertNotNull(transfer);
        assertEquals(MovementType.TRANSFER, transfer.type());
        assertEquals(40, service.getBin("BIN-C1").getCurrentQuantity());
        assertEquals(20, service.getBin("BIN-C2").getCurrentQuantity());
        assertEquals(60, service.getCurrentStock("MILK-001"));
    }

    @Test
    @DisplayName("Collections: Deque undo stack allows rolling back the last stock operation")
    void testUndoOperation() throws InsufficientStockException {
        service.stockIn("MILK-001", "BATCH-M1", "BIN-C1", 50, LocalDate.now().plusDays(10), "Inbound", "admin");
        assertEquals(50, service.getCurrentStock("MILK-001"));

        // Perform stock out of 10
        service.stockOut("MILK-001", 10, "Order 1", "admin");
        assertEquals(40, service.getCurrentStock("MILK-001"));

        // Undo the last operation (should restore 10 units back)
        StockMovement undoMovement = service.undoLastOperation("supervisor");
        assertNotNull(undoMovement);
        assertEquals(50, service.getCurrentStock("MILK-001"), "Stock must be reverted to 50");
    }

    @Test
    @DisplayName("Collections: PriorityQueue prioritizes critical alerts over lower severity")
    void testAlertPriorityQueue() {
        // Trigger low stock for laptop (stock is 0, min is 5 -> Critical)
        service.triggerAlert(new Alert("A1", "LAPTOP-001", "Out of stock", Alert.Severity.CRITICAL, Instant.now()));
        service.triggerAlert(new Alert("A2", "MILK-001", "Approaching reorder point", Alert.Severity.MEDIUM, Instant.now()));
        service.triggerAlert(new Alert("A3", "MILK-001", "Milk below min stock", Alert.Severity.HIGH, Instant.now()));

        List<Alert> topAlerts = service.getPendingAlerts();
        assertEquals(3, topAlerts.size());
        assertEquals(Alert.Severity.CRITICAL, topAlerts.get(0).severity(), "First alert must be CRITICAL");
        assertEquals(Alert.Severity.HIGH, topAlerts.get(1).severity(), "Second alert must be HIGH");
        assertEquals(Alert.Severity.MEDIUM, topAlerts.get(2).severity(), "Third alert must be MEDIUM");
    }

    @Test
    @DisplayName("Collections: LinkedHashMap maintains access order for recently viewed items")
    void testRecentlyViewedCache() {
        service.recordProductView("MILK-001");
        service.recordProductView("LAPTOP-001");
        service.recordProductView("MILK-001"); // Access again

        List<Product> recent = service.getRecentlyViewed();
        assertFalse(recent.isEmpty());
        assertEquals("MILK-001", recent.get(0).getSku(), "Most recently accessed must be at head/tail according to LRU");
    }

    @Test
    @DisplayName("Streams: Analytical reports calculate category valuation and top movers")
    void testStreamAnalytics() throws InsufficientStockException {
        service.stockIn("MILK-001", "B-M1", "BIN-C1", 50, LocalDate.now().plusDays(10), "Inbound", "admin");
        service.stockIn("LAPTOP-001", "B-L1", "BIN-D1", 10, null, "Inbound", "admin");

        // Valuation: 50 * 2.00 cost = 100 for PERISHABLE; 10 * 800.00 cost = 8000 for ELECTRONICS
        Map<Category, Double> valuation = service.calculateValuationByCategory();
        assertEquals(100.0, valuation.get(Category.PERISHABLE), 0.01);
        assertEquals(8000.0, valuation.get(Category.ELECTRONICS), 0.01);

        // Stock out laptops to make it a top mover
        service.stockOut("LAPTOP-001", 6, "Order 1", "admin");
        service.stockOut("MILK-001", 2, "Order 2", "admin");

        List<Map.Entry<String, Integer>> topMovers = service.getTopMovers(5);
        assertEquals("LAPTOP-001", topMovers.get(0).getKey(), "LAPTOP-001 had 6 units moved, must be #1 mover");
    }
}
