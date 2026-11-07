package com.iwms.auth;

import com.iwms.exception.InsufficientStockException;
import com.iwms.exception.UnauthorizedException;
import com.iwms.model.*;
import com.iwms.service.InventoryService;
import com.iwms.sync.SyncEngine;
import com.iwms.sync.SyncOperation;
import com.iwms.sync.SyncResult;
import com.iwms.sync.SyncStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Task 6: Authentication, RBAC User Hierarchy & Offline Sync Engine Tests")
class AuthAndSyncTest {

    private Pbkdf2Authenticator authenticator;
    private SessionManager sessionManager;
    private InventoryService inventoryService;
    private SyncEngine syncEngine;

    @BeforeEach
    void setUp() throws Exception {
        authenticator = new Pbkdf2Authenticator();
        sessionManager = new SessionManager(Duration.ofHours(8));
        inventoryService = new InventoryService();

        // Warehouse setup for sync tests
        Warehouse wh = new Warehouse("WH-1", "Main Hub", "Test Facility");
        Zone zone = new Zone("Z-1", "Ambient", Category.GENERAL);
        Bin bin1 = new Bin("BIN-1", "B-1", 500, 1000.0);
        Bin bin2 = new Bin("BIN-2", "B-2", 500, 1000.0);
        zone.addBin(bin1);
        zone.addBin(bin2);
        wh.addZone(zone);
        inventoryService.addWarehouse(wh);

        // Product setup
        BulkProduct product = BulkProduct.builder()
                .sku("RICE-100")
                .name("Basmati Rice 25kg")
                .category(Category.BULK)
                .unitPrice(40.0)
                .costPrice(25.0)
                .minStockLevel(10)
                .weightKg(25.0)
                .volumeCubicMeters(0.04)
                .build();
        inventoryService.registerProduct(product);

        syncEngine = new SyncEngine(inventoryService);
    }

    @Test
    @DisplayName("Pbkdf2Authenticator: Generates secure salts, hashes passwords, and verifies them")
    void testPasswordHashingAndVerification() {
        byte[] salt1 = authenticator.generateSalt();
        byte[] salt2 = authenticator.generateSalt();

        assertNotEquals(salt1, salt2, "Each salt must be unique");

        String password = "SecureWarehousePassword#2026";
        String hash1 = authenticator.hashPassword(password, salt1);
        String hash2 = authenticator.hashPassword(password, salt2);

        assertNotEquals(hash1, hash2, "Identical passwords with different salts must produce distinct hashes");
        assertTrue(authenticator.verifyPassword(password, hash1, salt1), "Password should verify with correct salt");
        assertFalse(authenticator.verifyPassword("WrongPassword", hash1, salt1), "Incorrect password must fail");
    }

    @Test
    @DisplayName("Account Lockout: 5 consecutive failed logins locks account for 15 minutes")
    void testAccountLockout() {
        byte[] salt = authenticator.generateSalt();
        String hash = authenticator.hashPassword("correct_pass", salt);
        User user = new EmployeeUser("EMP-01", "john_floor", hash, salt, "John Doe");

        assertFalse(user.isAccountLocked(), "User should not be locked initially");

        // Simulate 4 failed attempts
        for (int i = 1; i <= 4; i++) {
            boolean success = authenticator.authenticate(user, "wrong_pass");
            assertFalse(success);
            assertEquals(i, user.getFailedLoginAttempts());
            assertFalse(user.isAccountLocked());
        }

        // 5th failed attempt triggers lockout
        boolean fifthAttempt = authenticator.authenticate(user, "wrong_pass");
        assertFalse(fifthAttempt);
        assertTrue(user.isAccountLocked(), "Account must be locked on 5th failure");

        // Even with correct password, locked account must be rejected
        assertThrows(UnauthorizedException.class, () -> authenticator.authenticate(user, "correct_pass"));
    }

    @Test
    @DisplayName("User RBAC Hierarchy: Enforces polymorphic permissions across Admin, Manager, and Employee")
    void testUserRBACHierarchy() {
        byte[] salt = authenticator.generateSalt();
        String hash = authenticator.hashPassword("pass", salt);

        User admin = new AdminUser("U-01", "admin", hash, salt, "Super Admin");
        User manager = new ManagerUser("U-02", "sarah_mgr", hash, salt, "Sarah Manager");
        User employee = new EmployeeUser("U-03", "tim_emp", hash, salt, "Tim Employee");

        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(Role.MANAGER, manager.getRole());
        assertEquals(Role.EMPLOYEE, employee.getRole());

        // Admin has all permissions
        EnumSet<Permission> adminPerms = admin.getPermissions();
        assertTrue(adminPerms.contains(Permission.MANAGE_USERS));
        assertTrue(adminPerms.contains(Permission.VIEW_FINANCIALS));
        assertTrue(adminPerms.contains(Permission.VIEW_COST_PRICES));
        assertTrue(adminPerms.contains(Permission.DELETE_PRODUCTS));

        // Manager has operational and financial permissions, but not system admin
        EnumSet<Permission> mgrPerms = manager.getPermissions();
        assertTrue(mgrPerms.contains(Permission.VIEW_FINANCIALS));
        assertTrue(mgrPerms.contains(Permission.VIEW_COST_PRICES));
        assertTrue(mgrPerms.contains(Permission.STOCK_IN));
        assertFalse(mgrPerms.contains(Permission.MANAGE_USERS));

        // Employee has only physical warehouse floor permissions (costs and financials redacted)
        EnumSet<Permission> empPerms = employee.getPermissions();
        assertTrue(empPerms.contains(Permission.STOCK_IN));
        assertTrue(empPerms.contains(Permission.STOCK_OUT));
        assertTrue(empPerms.contains(Permission.CYCLE_COUNT));
        assertFalse(empPerms.contains(Permission.VIEW_COST_PRICES), "Employee must not see cost prices");
        assertFalse(empPerms.contains(Permission.VIEW_FINANCIALS), "Employee must not see financial margins");
        assertFalse(empPerms.contains(Permission.MANAGE_USERS));
    }

    @Test
    @DisplayName("SessionManager: Manages token lifecycle, expiration, and invalidation")
    void testSessionManagement() {
        byte[] salt = authenticator.generateSalt();
        String hash = authenticator.hashPassword("pass", salt);
        User user = new ManagerUser("U-02", "sarah_mgr", hash, salt, "Sarah Manager");

        SessionManager.Session session = sessionManager.createSession(user);
        assertNotNull(session.token());
        assertTrue(session.token().length() >= 32);

        Optional<User> activeUser = sessionManager.validateSession(session.token());
        assertTrue(activeUser.isPresent());
        assertEquals("sarah_mgr", activeUser.get().getUsername());

        // Invalidate session
        sessionManager.invalidateSession(session.token());
        Optional<User> afterLogout = sessionManager.validateSession(session.token());
        assertFalse(afterLogout.isPresent(), "Invalidated token must be rejected");
    }

    @Test
    @DisplayName("SyncEngine: Applies offline operations and handles duplicate idempotency")
    void testSyncEngineIdempotency() {
        // Initial stock in via sync operation
        SyncOperation op1 = new SyncOperation(
                "SYNC-UUID-001",
                Instant.now().toString(),
                "IN",
                "RICE-100",
                "BATCH-R1",
                null,
                "BIN-1",
                50,
                "PWA Offline Receiving",
                "tim_emp"
        );

        SyncResult res1 = syncEngine.processOperation(op1);
        assertEquals(SyncStatus.APPLIED, res1.status());
        assertEquals(50, inventoryService.getCurrentStock("RICE-100"));

        // Replaying the exact same sync operation (e.g. client retries or network glitch)
        SyncResult res2 = syncEngine.processOperation(op1);
        assertEquals(SyncStatus.DUPLICATE, res2.status());
        // Stock must remain 50, not double-incremented to 100
        assertEquals(50, inventoryService.getCurrentStock("RICE-100"));
    }

    @Test
    @DisplayName("SyncEngine: Flags conflicts when offline dispatch exceeds available stock")
    void testSyncEngineConflictHandling() throws InsufficientStockException {
        // Inbound 20 units
        inventoryService.stockIn("RICE-100", "BATCH-R1", "BIN-1", 20, null, "Inbound", "admin");

        // Offline device attempts to dispatch 30 units (10 more than available)
        SyncOperation opOut = new SyncOperation(
                "SYNC-UUID-002",
                Instant.now().toString(),
                "OUT",
                "RICE-100",
                "BATCH-R1",
                "BIN-1",
                null,
                30,
                "Offline Floor Order #999",
                "tim_emp"
        );

        SyncResult res = syncEngine.processOperation(opOut);
        assertEquals(SyncStatus.CONFLICT, res.status());
        assertTrue(res.message().contains("deficit") || res.message().contains("Insufficient"));

        // Verify conflict recorded for manager review
        List<SyncEngine.SyncConflict> conflicts = syncEngine.getPendingConflicts();
        assertEquals(1, conflicts.size());
        assertEquals("SYNC-UUID-002", conflicts.get(0).operation().syncId());

        // Resolving conflict
        syncEngine.resolveConflict(conflicts.get(0).conflictId(), false, "Rejected due to stock deficit", "sarah_mgr");
        assertEquals(0, syncEngine.getPendingConflicts().size(), "Pending conflicts should be empty after resolution");
    }
}
