package com.iwms.api.config;

import com.iwms.auth.Authenticator;
import com.iwms.auth.Pbkdf2Authenticator;
import com.iwms.auth.SessionManager;
import com.iwms.model.*;
import com.iwms.report.CsvReportGenerator;
import com.iwms.report.PdfReportGenerator;
import com.iwms.repository.FileBackedRepository;
import com.iwms.repository.InMemoryRepository;
import com.iwms.repository.Repository;
import com.iwms.service.InventoryService;
import com.iwms.storage.*;
import com.iwms.sync.SyncEngine;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Spring Bean Configuration for Domain Services.
 * Wires pure Java core domain classes into Spring dependency injection container.
 * Enforces zero Spring annotations in the core domain packages.
 */
@Configuration
public class DomainBeanConfig {

    private final Path dataDir = Paths.get("data");
    private final Path backupDir = Paths.get("backups");

    @Bean
    public InventoryService inventoryService() {
        return new InventoryService();
    }

    @Bean
    public Authenticator authenticator() {
        return new Pbkdf2Authenticator();
    }

    @Bean
    public SessionManager sessionManager() {
        return new SessionManager(Duration.ofHours(8));
    }

    @Bean
    public Repository<Product, String> productRepository() {
        try {
            Files.createDirectories(dataDir);
        } catch (Exception ignored) {
        }
        return new FileBackedRepository<>(dataDir.resolve("products.json"), Product.class, Product::getSku);
    }

    @Bean
    public Repository<User, String> userRepository(Authenticator authenticator) {
        Repository<User, String> repo = new InMemoryRepository<>(User::getUsername);

        // Seed Default Users
        byte[] saltAdmin = authenticator.generateSalt();
        String hashAdmin = authenticator.hashPassword("admin123", saltAdmin);
        repo.save(new AdminUser("U-001", "admin", hashAdmin, saltAdmin, "Administrator"));

        byte[] saltManager = authenticator.generateSalt();
        String hashManager = authenticator.hashPassword("manager123", saltManager);
        repo.save(new ManagerUser("U-002", "manager", hashManager, saltManager, "Warehouse Manager"));

        byte[] saltEmployee = authenticator.generateSalt();
        String hashEmployee = authenticator.hashPassword("employee123", saltEmployee);
        repo.save(new EmployeeUser("U-003", "employee", hashEmployee, saltEmployee, "Floor Associate"));

        return repo;
    }

    @Bean
    public MovementLedger movementLedger() throws Exception {
        Files.createDirectories(dataDir);
        return new MovementLedger(dataDir.resolve("ledger.log"));
    }

    @Bean
    public AuditLogger auditLogger() throws Exception {
        Files.createDirectories(dataDir);
        return new AuditLogger(dataDir.resolve("audit.log"));
    }

    @Bean
    public BackupManager backupManager() throws Exception {
        Files.createDirectories(dataDir);
        Files.createDirectories(backupDir);
        BackupManager bm = new BackupManager(dataDir, backupDir);
        bm.registerShutdownHook();
        return bm;
    }

    @Bean
    public SyncEngine syncEngine(InventoryService inventoryService) {
        return new SyncEngine(inventoryService);
    }

    @Bean
    public PdfReportGenerator pdfReportGenerator(InventoryService inventoryService) {
        return new PdfReportGenerator(inventoryService);
    }

    @Bean
    public CsvReportGenerator csvReportGenerator(InventoryService inventoryService) {
        return new CsvReportGenerator(inventoryService);
    }

    @Bean
    public DataSeeder dataSeeder(InventoryService inventoryService, Repository<Product, String> productRepository) throws Exception {
        DataSeeder seeder = new DataSeeder(inventoryService, productRepository);
        seeder.seedIfEmpty();
        return seeder;
    }
}
