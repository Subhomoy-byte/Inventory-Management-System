# Inventory & Warehouse Management System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-grade, offline-capable inventory and warehouse management system in Java 17+ (pure OOP domain) and React + Vite (glassmorphism PWA), packaged into a single runnable JAR (`java -jar iwms.jar`).

**Architecture:** Multi-module Maven (`iwms-core`, `iwms-web`, `iwms-ui`). Plain Java 17 domain with zero framework dependencies, atomic file persistence, custom auth filter, OpenPDF report generator, and React PWA with IndexedDB offline sync.

**Tech Stack:** Java 17+, Maven Wrapper (`mvnw`), Spring Boot 3 (REST/Static host only), React, Vite, Tailwind CSS, Framer Motion, Recharts, OpenPDF, ZXing, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-08-inventory-warehouse-management-system-design.md`

## Global Constraints
- Core domain packages (`model`, `service`, `repository`, `storage`) must have NO framework imports (plain Java 17+).
- Persistence via JSON/CSV files using `java.nio.file` and `java.io` with atomic writes; strictly no SQL database.
- Backend first, then frontend corresponding to that.
- Single command runnable: `java -jar iwms.jar`.
- README and GitHub repo updated and committed/pushed after each task.

## Review Focus
1. **Concurrency data race on concurrent stock mutations:** `ReentrantReadWriteLock` properly protects both in-memory cache and atomic file writes.
2. **Server-side price leak to employee users:** DTO projection on `/api/products` and `/api/reports` strips prices/valuation for `EmployeeUser`.
3. **Double counting on offline sync retries:** `HashSet<String> processedSyncIds` idempotently rejects duplicates with status `DUPLICATE`.
4. **File corruption on abnormal termination:** Atomic writes via temp file and `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`.
5. **Cold start with missing data directory:** Repository and storage layers auto-create `data/` and seed 200 initial products gracefully.

---

### Task 1: Project Scaffolding & Multi-Module Maven Setup
**Files:**
- Create: `pom.xml` (parent root)
- Create: `iwms-core/pom.xml`
- Create: `iwms-web/pom.xml`
- Create: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- Test: `iwms-core/src/test/java/com/iwms/SmokeTest.java`

**Interfaces:**
- Consumes: None
- Produces: Maven build lifecycle compiling `iwms-core` with zero Spring dependencies, and `iwms-web` as Spring Boot app.

- [ ] **Step 1: Create parent `pom.xml` and module POMs**
- [ ] **Step 2: Add Maven Wrapper scripts (`mvnw.cmd`, `mvnw`, wrapper jar)**
- [ ] **Step 3: Create SmokeTest in `iwms-core` verifying Java 17 runtime**
- [ ] **Step 4: Execute `mvnw clean test` to verify build succeeds**
- [ ] **Step 5: Commit and push: `chore: setup multi-module maven structure`**

---

### Task 2: Core Domain Model, Enums, Custom Exceptions & Product Hierarchy
**Files:**
- Create: `iwms-core/src/main/java/com/iwms/exception/*.java`
- Create: `iwms-core/src/main/java/com/iwms/model/Category.java`, `MovementType.java`, `Role.java`, `Permission.java`
- Create: `iwms-core/src/main/java/com/iwms/model/Storable.java`, `Auditable.java`, `Exportable.java`
- Create: `iwms-core/src/main/java/com/iwms/model/Product.java` (abstract + `ProductBuilder`)
- Create: `iwms-core/src/main/java/com/iwms/model/PerishableProduct.java`, `ElectronicProduct.java`, `FragileProduct.java`, `BulkProduct.java`
- Create: `iwms-core/src/main/java/com/iwms/model/Batch.java`, `StockMovement.java` (immutable)
- Test: `iwms-core/src/test/java/com/iwms/model/ProductHierarchyTest.java`

**Interfaces:**
- Consumes: None
- Produces: `Product`, `PerishableProduct`, `ElectronicProduct`, `FragileProduct`, `BulkProduct`, `StockMovement`, custom exceptions (`InsufficientStockException`, `DuplicateSkuException`).

- [ ] **Step 1: Write JUnit 5 tests for Product encapsulation, validation, builder, and polymorphic cost calculation**
- [ ] **Step 2: Implement custom domain exceptions and interfaces (`Storable`, `Auditable`, `Exportable`)**
- [ ] **Step 3: Implement abstract `Product` with private fields, validation, and nested `ProductBuilder`**
- [ ] **Step 4: Implement subclasses (`PerishableProduct`, `ElectronicProduct`, `FragileProduct`, `BulkProduct`)**
- [ ] **Step 5: Run tests to verify all tests pass**
- [ ] **Step 6: Commit and push: `feat(core): implement core domain models, builder, and product hierarchy`**

---

### Task 3: Domain Strategy Interfaces & Business Logic
**Files:**
- Create: `iwms-core/src/main/java/com/iwms/service/PickStrategy.java`, `FefoPickStrategy.java`, `FifoPickStrategy.java`
- Create: `iwms-core/src/main/java/com/iwms/service/AlertStrategy.java`, `LowStockAlertStrategy.java`, `ExpiringBatchAlertStrategy.java`
- Create: `iwms-core/src/main/java/com/iwms/service/PricingStrategy.java`, `FifoPricingStrategy.java`, `AverageCostPricingStrategy.java`
- Create: `iwms-core/src/main/java/com/iwms/service/ReorderStrategy.java`, `MinMaxReorderStrategy.java`, `VelocityReorderStrategy.java`
- Create: `iwms-core/src/main/java/com/iwms/model/Alert.java`, `ReorderSuggestion.java`, `StockSnapshot.java`
- Test: `iwms-core/src/test/java/com/iwms/service/StrategyTests.java`

**Interfaces:**
- Consumes: `Product`, `Batch`, `StockMovement`
- Produces: Pluggable business strategy implementations for picking, alerts, reorders, and pricing.

- [ ] **Step 1: Write unit tests verifying FEFO vs FIFO batch selection and alert priority calculation**
- [ ] **Step 2: Implement `PickStrategy` with `FefoPickStrategy` and `FifoPickStrategy`**
- [ ] **Step 3: Implement `AlertStrategy` with `LowStockAlertStrategy` and `ExpiringBatchAlertStrategy`**
- [ ] **Step 4: Implement `PricingStrategy` and `ReorderStrategy`**
- [ ] **Step 5: Run tests and ensure 100% pass**
- [ ] **Step 6: Commit and push: `feat(core): implement domain strategy interfaces with 2+ implementations`**

---

### Task 4: Warehouse Entities, Collections & Thread-Safe Service Layer
**Files:**
- Create: `iwms-core/src/main/java/com/iwms/model/Warehouse.java`, `Zone.java`, `Bin.java`, `Supplier.java`, `PurchaseOrder.java`
- Create: `iwms-core/src/main/java/com/iwms/service/InventoryService.java` (implements `ReentrantReadWriteLock`, `PriorityQueue` for alerts, `ArrayDeque` for undo stack, `LinkedHashMap` for recently viewed)
- Test: `iwms-core/src/test/java/com/iwms/service/InventoryServiceTest.java`

**Interfaces:**
- Consumes: Models and Strategy interfaces
- Produces: High-level inventory mutations (`stockIn`, `stockOut`, `transfer`, `adjust`, `undoLastOperation`, `getUrgentAlerts`).

- [ ] **Step 1: Write JUnit tests for stock In, Out, Transfer, and Undo operations using `ArrayDeque`**
- [ ] **Step 2: Implement `Warehouse`, `Zone`, `Bin`, `Supplier`, `PurchaseOrder`**
- [ ] **Step 3: Implement `InventoryService` with `ReentrantReadWriteLock` and collection structures**
- [ ] **Step 4: Run tests and verify thread-safe state updates**
- [ ] **Step 5: Commit and push: `feat(core): implement warehouse entities, collections, and InventoryService with undo stack`**

---

### Task 5: File Persistence Layer (Atomic Writes, Ledger, Audit, Backup/Restore)
**Files:**
- Create: `iwms-core/src/main/java/com/iwms/repository/Repository.java`, `InMemoryRepository.java`, `FileBackedRepository.java`
- Create: `iwms-core/src/main/java/com/iwms/storage/AtomicFileWriter.java`
- Create: `iwms-core/src/main/java/com/iwms/storage/MovementLedger.java`
- Create: `iwms-core/src/main/java/com/iwms/storage/AuditLogger.java`
- Create: `iwms-core/src/main/java/com/iwms/storage/BackupManager.java`
- Create: `iwms-core/src/main/java/com/iwms/storage/DataSeeder.java` (~200 diverse products across categories)
- Test: `iwms-core/src/test/java/com/iwms/storage/FilePersistenceTest.java`

**Interfaces:**
- Consumes: `InventoryService`, `Product`, `StockMovement`
- Produces: Crash-resilient file persistence, append-only logs, time-travel reconstruction from ledger, automated backups.

- [ ] **Step 1: Write unit tests for atomic file writes, ledger append, and restart data recovery**
- [ ] **Step 2: Implement `AtomicFileWriter` using temporary files and `ATOMIC_MOVE`**
- [ ] **Step 3: Implement `MovementLedger` and `AuditLogger` with append-only file writers**
- [ ] **Step 4: Implement `FileBackedRepository` and `BackupManager`**
- [ ] **Step 5: Implement `DataSeeder` with 200 catalog items**
- [ ] **Step 6: Run tests and verify disk persistence and data recovery**
- [ ] **Step 7: Commit and push: `feat(storage): implement atomic file persistence, ledger, audit logging, and data seeder`**

---

### Task 6: Authentication, User RBAC & Offline Sync Engine
**Files:**
- Create: `iwms-core/src/main/java/com/iwms/model/User.java`, `AdminUser.java`, `ManagerUser.java`, `EmployeeUser.java`
- Create: `iwms-core/src/main/java/com/iwms/auth/Authenticator.java`, `Pbkdf2Authenticator.java`, `SessionManager.java`
- Create: `iwms-core/src/main/java/com/iwms/sync/SyncEngine.java`, `SyncOperation.java`, `SyncResult.java`
- Test: `iwms-core/src/test/java/com/iwms/auth/AuthAndSyncTest.java`

**Interfaces:**
- Consumes: `User`, `Role`, `Permission`, `InventoryService`
- Produces: PBKDF2 authentication, token sessions, account lockout, idempotent sync deduplication.

- [ ] **Step 1: Write tests for PBKDF2 password hashing, session expiry, role permissions, and sync deduplication**
- [ ] **Step 2: Implement `User` hierarchy with `AdminUser`, `ManagerUser`, `EmployeeUser` and `EnumSet<Permission>`**
- [ ] **Step 3: Implement `Pbkdf2Authenticator` and `SessionManager`**
- [ ] **Step 4: Implement `SyncEngine` with `HashSet<String> processedSyncIds` and conflict detection**
- [ ] **Step 5: Run tests and verify RBAC and sync deduplication**
- [ ] **Step 6: Commit and push: `feat(auth-sync): implement PBKDF2 authenticator, RBAC user hierarchy, and offline sync engine`**

---

### Task 7: OpenPDF Summary Report Generator
**Files:**
- Create: `iwms-core/src/main/java/com/iwms/report/ReportGenerator.java`, `PdfReportGenerator.java`, `CsvReportGenerator.java`
- Test: `iwms-core/src/test/java/com/iwms/report/ReportGeneratorTest.java`

**Interfaces:**
- Consumes: `InventoryService`, `Product`, `Zone`, `User`
- Produces: High-contrast monochrome A4 PDF bytes and CSV exports.

- [ ] **Step 1: Write tests verifying PDF generation, section layout, and price omission for `EmployeeUser`**
- [ ] **Step 2: Implement `PdfReportGenerator` using OpenPDF with A4 styling and zone count sheets**
- [ ] **Step 3: Implement `CsvReportGenerator` for product data import/export**
- [ ] **Step 4: Run tests to verify generated PDF structure and compliance**
- [ ] **Step 5: Commit and push: `feat(report): implement OpenPDF warehouse floor report generator and CSV exporter`**

---

### Task 8: Spring Boot REST API, Custom Auth Filter & HTTPS Keystore
**Files:**
- Create: `iwms-web/src/main/java/com/iwms/api/IwmsApplication.java`
- Create: `iwms-web/src/main/java/com/iwms/api/filter/CustomAuthFilter.java`
- Create: `iwms-web/src/main/java/com/iwms/api/controller/*.java` (Auth, Product, Stock, Warehouse, Order, Sync, Report, Backup)
- Create: `iwms-web/src/main/java/com/iwms/api/dto/*.java` (Role-aware response projections)
- Create: `iwms-web/src/main/resources/application.yml` (HTTPS port 8443, SSL keystore)
- Test: `iwms-web/src/test/java/com/iwms/api/ApiControllerTests.java`

**Interfaces:**
- Consumes: All `iwms-core` services
- Produces: Complete REST API with custom auth filter, role-based response projection, and HTTPS configuration.

- [ ] **Step 1: Write integration tests for API authentication, stock movements, and employee price redaction**
- [ ] **Step 2: Implement `CustomAuthFilter` with token verification and `@RequiresPermission` checks**
- [ ] **Step 3: Implement REST Controllers and role-aware DTO mappers**
- [ ] **Step 4: Configure HTTPS with self-signed PKCS12 keystore**
- [ ] **Step 5: Run tests and verify API endpoints**
- [ ] **Step 6: Commit and push: `feat(api): implement REST controllers, custom auth filter, and HTTPS configuration`**

---

### Task 9: Frontend Architecture & Glassmorphism Design System (`iwms-ui`)
**Files:**
- Create: `iwms-ui/package.json`, `vite.config.ts`, `tailwind.config.js`, `tsconfig.json`
- Create: `iwms-ui/src/index.css`, `App.tsx`, `main.tsx`
- Create: `iwms-ui/src/components/layout/Dock.tsx`, `Navbar.tsx`, `GlassCard.tsx`, `StatusChip.tsx`
- Create: `iwms-ui/src/pages/LoginPage.tsx`
- Test: Build verification with `npm.cmd run build`

**Interfaces:**
- Consumes: REST API (`/api/auth/login`)
- Produces: Glassmorphism shell, dark/light theme toggle, floating navigation dock, responsive login screen.

- [ ] **Step 1: Scaffold React + Vite + Tailwind + Framer Motion project in `iwms-ui`**
- [ ] **Step 2: Implement glassmorphism CSS components, gradient blobs, and theme provider**
- [ ] **Step 3: Implement floating glass dock and responsive navigation**
- [ ] **Step 4: Implement `LoginPage` with demo credential buttons**
- [ ] **Step 5: Verify build with `npm.cmd run build`**
- [ ] **Step 6: Commit and push: `feat(ui): initialize React PWA shell with glassmorphism design system and login`**

---

### Task 10: Core Frontend Screens & PWA Offline Sync Engine
**Files:**
- Create: `iwms-ui/src/services/api.ts`, `storage/db.ts`, `storage/sync.ts`
- Create: `iwms-ui/src/pages/DashboardPage.tsx`
- Create: `iwms-ui/src/pages/InventoryPage.tsx` (glass table, search, category filter, pagination)
- Create: `iwms-ui/src/pages/WarehouseMapPage.tsx` (bin grid, fill level glow, bin detail drawer)
- Create: `iwms-ui/src/components/modals/StockMovementModal.tsx` (In, Out, Transfer, Adjust)
- Create: `iwms-ui/src/components/sync/ConflictReviewPanel.tsx`
- Test: Offline outbox verification and build test

**Interfaces:**
- Consumes: REST API and browser IndexedDB
- Produces: Full inventory management UI with offline queueing and conflict review.

- [ ] **Step 1: Implement IndexedDB outbox queue using `idb` and network status listener**
- [ ] **Step 2: Implement `DashboardPage` with animated metric cards, low-stock radar, and charts**
- [ ] **Step 3: Implement `InventoryPage` with search, filters, and stock movement dialogs**
- [ ] **Step 4: Implement `WarehouseMapPage` with visual bin fill levels and inspection drawer**
- [ ] **Step 5: Implement `ConflictReviewPanel` for manager review**
- [ ] **Step 6: Verify build passes cleanly**
- [ ] **Step 7: Commit and push: `feat(ui): implement core screens, warehouse zone map, and offline outbox sync`**

---

### Task 11: Advanced Features (Time-Travel, FEFO Picking, Cycle Count, PDF Modal)
**Files:**
- Create: `iwms-ui/src/pages/MovementsPage.tsx` (timeline + time-travel slider querying `/api/stock/asof`)
- Create: `iwms-ui/src/pages/OrdersPage.tsx` (PO management and receiving)
- Create: `iwms-ui/src/pages/ReportsPage.tsx` (PDF preview modal and CSV import/export)
- Create: `iwms-ui/src/pages/CycleCountPage.tsx` (guided count with variance report)
- Create: `iwms-ui/src/components/CommandPalette.tsx` (`Ctrl + K`)

**Interfaces:**
- Consumes: REST API (`/api/stock/asof`, `/api/reports/summary.pdf`, `/api/orders`)
- Produces: Advanced operational tools.

- [ ] **Step 1: Implement `MovementsPage` with interactive time-travel slider**
- [ ] **Step 2: Implement `OrdersPage` and `CycleCountPage`**
- [ ] **Step 3: Implement `ReportsPage` with in-browser PDF preview modal**
- [ ] **Step 4: Implement global `CommandPalette` (`Ctrl + K`)**
- [ ] **Step 5: Verify build with `npm.cmd run build`**
- [ ] **Step 6: Commit and push: `feat(ui): implement time-travel slider, cycle count, orders, and PDF preview`**

---

### Task 12: Production Packaging, End-to-End Verification & Comprehensive README
**Files:**
- Modify: `iwms-web/pom.xml` (configure maven-resources / frontend integration to bundle `iwms-ui/dist` into `static/`)
- Modify: `README.md` (comprehensive setup, verification steps, concept table with exact file links)
- Test: Full build `mvnw clean package` and end-to-end JAR verification

**Interfaces:**
- Consumes: All modules
- Produces: Executable `iwms.jar` runnable via `java -jar iwms.jar`.

- [ ] **Step 1: Configure automated frontend bundling into Spring Boot static resources**
- [ ] **Step 2: Execute full build and verify single JAR creation**
- [ ] **Step 3: Verify all unit and integration tests pass**
- [ ] **Step 4: Update `README.md` with complete architectural documentation and concept mapping table**
- [ ] **Step 5: Final commit and push to GitHub repository `main` branch**
