package com.iwms.storage;

import com.iwms.model.*;
import com.iwms.repository.FileBackedRepository;
import com.iwms.repository.InMemoryRepository;
import com.iwms.repository.Repository;
import com.iwms.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Task 5: File Persistence, Atomic Writes, Ledger, Audit, Backup and Seeder Tests")
class FilePersistenceTest {

    @TempDir
    Path tempDir;

    private Path dataDir;
    private Path backupDir;

    @BeforeEach
    void setUp() throws IOException {
        dataDir = tempDir.resolve("data");
        backupDir = tempDir.resolve("backups");
        Files.createDirectories(dataDir);
        Files.createDirectories(backupDir);
    }

    @Test
    @DisplayName("AtomicFileWriter: Writes files atomically without corruption")
    void testAtomicFileWriter() throws Exception {
        Path targetFile = dataDir.resolve("test_config.json");
        String initialContent = "{\"version\": 1, \"status\": \"ACTIVE\"}";

        AtomicFileWriter.writeString(targetFile, initialContent);
        assertTrue(Files.exists(targetFile));
        assertEquals(initialContent, AtomicFileWriter.readString(targetFile));

        // Overwrite atomically
        String updatedContent = "{\"version\": 2, \"status\": \"UPGRADED\"}";
        AtomicFileWriter.writeString(targetFile, updatedContent);
        assertEquals(updatedContent, AtomicFileWriter.readString(targetFile));

        // Temp file should not linger
        Path tempFile = targetFile.resolveSibling(targetFile.getFileName().toString() + ".tmp");
        assertFalse(Files.exists(tempFile));
    }

    @Test
    @DisplayName("MovementLedger: Append-only log persists stock movements and replays them")
    void testMovementLedgerAppendAndReplay() throws Exception {
        Path ledgerFile = dataDir.resolve("ledger.log");
        MovementLedger ledger = new MovementLedger(ledgerFile);

        StockMovement mov1 = new StockMovement(
                "MOV-001", Instant.now().minusSeconds(60), "MILK-001", "B-01",
                MovementType.IN, 50, null, "BIN-A1", "Initial stock", "admin"
        );
        StockMovement mov2 = new StockMovement(
                "MOV-002", Instant.now(), "MILK-001", "B-01",
                MovementType.OUT, 10, "BIN-A1", null, "Order #1", "picker"
        );

        ledger.append(mov1);
        ledger.append(mov2);

        assertEquals(2, ledger.countMovements());

        List<StockMovement> replayed = ledger.replay();
        assertEquals(2, replayed.size());
        assertEquals("MOV-001", replayed.get(0).movementId());
        assertEquals(50, replayed.get(0).quantity());
        assertEquals("MOV-002", replayed.get(1).movementId());
        assertEquals(10, replayed.get(1).quantity());
    }

    @Test
    @DisplayName("AuditLogger: Records security and operational events sequentially")
    void testAuditLogger() throws Exception {
        Path auditFile = dataDir.resolve("audit.log");
        AuditLogger logger = new AuditLogger(auditFile);

        logger.log("admin", "LOGIN", "SYSTEM", "SUCCESS", "Logged in via localhost");
        logger.log("employee", "DELETE_PRODUCT", "SKU-999", "FAILURE", "Insufficient permissions");

        List<AuditLogger.AuditEvent> events = logger.getRecentEvents(10);
        assertEquals(2, events.size());
        assertEquals("LOGIN", events.get(0).action());
        assertEquals("DELETE_PRODUCT", events.get(1).action());
        assertEquals("FAILURE", events.get(1).outcome());
    }

    @Test
    @DisplayName("FileBackedRepository: Persists entities across restarts using JSON")
    void testFileBackedRepository() throws IOException {
        Path repoFile = dataDir.resolve("products.json");

        // First session: create and save products
        FileBackedRepository<Product, String> repo1 = new FileBackedRepository<>(
                repoFile, Product.class, Product::getSku
        );

        Product p1 = PerishableProduct.builder()
                .sku("MILK-001")
                .name("Organic Whole Milk 1L")
                .category(Category.PERISHABLE)
                .unitPrice(3.99)
                .costPrice(2.50)
                .minStockLevel(20)
                .shelfLifeDays(14)
                .storageTempCelsius(4.0)
                .requiresRefrigeration(true)
                .build();

        repo1.save(p1);
        assertEquals(1, repo1.count());
        assertTrue(Files.exists(repoFile), "Repository file must be written to disk");

        // Second session: simulate system restart by opening the repository afresh
        FileBackedRepository<Product, String> repo2 = new FileBackedRepository<>(
                repoFile, Product.class, Product::getSku
        );
        assertEquals(1, repo2.count(), "Persisted entities must be reloaded on startup");
        Optional<Product> reloaded = repo2.findById("MILK-001");
        assertTrue(reloaded.isPresent());
        assertEquals("Organic Whole Milk 1L", reloaded.get().getName());
        assertEquals(2.50, reloaded.get().getCostPrice());
    }

    @Test
    @DisplayName("BackupManager: Creates zip backup of data directory and restores it")
    void testBackupAndRestore() throws Exception {
        // Create dummy data files
        Files.writeString(dataDir.resolve("file1.txt"), "Important inventory data 1");
        Files.writeString(dataDir.resolve("file2.txt"), "Important inventory data 2");

        BackupManager backupManager = new BackupManager(dataDir, backupDir);
        Path backupZip = backupManager.createBackup("manual_test");

        assertTrue(Files.exists(backupZip));
        assertTrue(backupZip.getFileName().toString().endsWith(".zip"));

        // Verify listing
        List<Path> backups = backupManager.listBackups();
        assertFalse(backups.isEmpty());

        // Restore to a fresh restore target
        Path restoreTarget = tempDir.resolve("restored_data");
        backupManager.restoreBackup(backupZip, restoreTarget);

        assertTrue(Files.exists(restoreTarget.resolve("file1.txt")));
        assertTrue(Files.exists(restoreTarget.resolve("file2.txt")));
        assertEquals("Important inventory data 1", Files.readString(restoreTarget.resolve("file1.txt")));
    }

    @Test
    @DisplayName("DataSeeder: Seeds 200 catalog items and warehouse topology")
    void testDataSeeder() throws Exception {
        InventoryService service = new InventoryService();
        Repository<Product, String> productRepo = new InMemoryRepository<>(Product::getSku);
        DataSeeder seeder = new DataSeeder(service, productRepo);

        seeder.seedIfEmpty();

        // Verify product catalog size (~200 products)
        List<Product> products = productRepo.findAll();
        assertTrue(products.size() >= 200, "Catalog must have at least 200 seeded items, found: " + products.size());

        // Verify category diversity
        long perishableCount = products.stream().filter(p -> p instanceof PerishableProduct).count();
        long electronicCount = products.stream().filter(p -> p instanceof ElectronicProduct).count();
        long fragileCount = products.stream().filter(p -> p instanceof FragileProduct).count();
        long bulkCount = products.stream().filter(p -> p instanceof BulkProduct).count();

        assertTrue(perishableCount > 20, "Should have >20 perishables");
        assertTrue(electronicCount > 20, "Should have >20 electronics");
        assertTrue(fragileCount > 20, "Should have >20 fragile items");
        assertTrue(bulkCount > 20, "Should have >20 bulk items");

        // Verify warehouse topology
        List<Warehouse> warehouses = service.getWarehouses();
        assertFalse(warehouses.isEmpty());
        Warehouse mainWh = warehouses.get(0);
        assertTrue(mainWh.getZones().size() >= 3, "Warehouse must have at least 3 zones");

        // Second call should be a no-op (idempotent)
        seeder.seedIfEmpty();
        assertEquals(products.size(), productRepo.count(), "Second seed must be idempotent");
    }
}
