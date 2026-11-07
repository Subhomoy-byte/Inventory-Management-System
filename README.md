# Inventory & Warehouse Management System (IWMS)

[![Java 17+](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React 19](https://img.shields.io/badge/React-19.3-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Vite 6](https://img.shields.io/badge/Vite-6.2-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-3.4-38B2AC?logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![Tests](https://img.shields.io/badge/Tests-44%20Passing%20(100%25)-brightgreen)](https://github.com/Subhomoy-byte/Inventory-Management-System)
[![License](https://img.shields.io/badge/Cost-%240.00%20(100%25%20Free%20%26%20Open%20Source)-blue)](LICENSE)

An industrial-grade, offline-capable, LAN-accessible Inventory and Warehouse Management System engineered to run on any standard PC or server and seamlessly serve mobile floor workers, warehouse terminals, and desktop clients over a Local Area Network (LAN) — **with zero external database requirements, zero cloud dependencies, and 100% free open-source software ($0 cost).**

Packaged as a single self-contained executable JAR (`java -jar iwms.jar`), IWMS demonstrates academic Java Object-Oriented Programming (OOP) principles, concurrency control, custom persistence engines, cryptographic security, and offline Progressive Web App (PWA) synchronization.

---

## Key System Architecture

```
                                  +---------------------------------------------+
                                  |              Local Area Network (LAN)       |
                                  |   Mobile Tablets / Handhelds / Barcode Scanners  |
                                  +---------------------------------------------+
                                                         |
                                                         v
                                  +---------------------------------------------+
                                  |          Spring Boot 3 HTTP Host            |
                                  |   Port 8443 (REST API + Static React PWA)   |
                                  +---------------------------------------------+
                                                         |
                                                         v
                                  +---------------------------------------------+
                                  |      CustomAuthFilter (Pure Java Tokens)    |
                                  |   RBAC: ADMIN / MANAGER / EMPLOYEE           |
                                  +---------------------------------------------+
                                                         |
                                                         v
  +-----------------------------------------------------------------------------------------------------------+
  |                                        IWMS CORE PURE JAVA DOMAIN                                         |
  |                                (Zero Spring / Jakarta Framework Leakage)                                  |
  |                                                                                                           |
  |   +--------------------------------+  +---------------------------------+  +--------------------------+   |
  |   |        InventoryService        |  |         Pluggable Strategies    |  |       OpenPDF Engine     |   |
  |   |  - ReentrantReadWriteLock      |  |  - Pick: FEFO / FIFO            |  |  - Monochrome A4 Sheets  |   |
  |   |  - ArrayDeque (LIFO Undo)      |  |  - Alert: MinMax / Expiry       |  |  - Dynamic Cost Redaction|   |
  |   |  - TreeMap (Time-Travel)       |  |  - Reorder: LeadTime / Velocity |  |  - CSV Importer/Exporter |   |
  |   |  - PriorityQueue (Alerts)      |  |  - Pricing: Bulk / Promotional  |  |                          |   |
  |   |  - LinkedHashMap (LRU Cache)   |  +---------------------------------+  +--------------------------+   |
  |   +--------------------------------+                                                                      |
  +-----------------------------------------------------------------------------------------------------------+
                                                         |
                                                         v
  +-----------------------------------------------------------------------------------------------------------+
  |                                       FILE PERSISTENCE & CRASH SAFETY                                     |
  |                                                                                                           |
  |   +--------------------------+    +--------------------------+    +-----------------------------------+   |
  |   |     AtomicFileWriter     |    |   MovementLedger (WAL)   |    |           BackupManager           |   |
  |   |  - Temporary file staging|    |  - Append-only TSV file  |    |  - Automated ZIP archive          |   |
  |   |  - OS fsync: getFD().sync|    |  - Crash replay recovery |    |  - Zip-Slip path traversal guard  |   |
  |   |  - Files.move ATOMIC_MOVE|    |  - AuditLogger security  |    |  - JVM shutdown hook snapshot     |   |
  |   +--------------------------+    +--------------------------+    +-----------------------------------+   |
  +-----------------------------------------------------------------------------------------------------------+
```

---

## Academic Java OOP & CS Concept Mapping

This codebase is specifically architected to serve as a comprehensive, textbook-quality showcase of Java Object-Oriented Programming and Computer Science algorithms. Every concept is implemented in pure Java with zero framework magic:

| Java & CS Concept | Exact File Location | Architectural Implementation & Highlights |
| :--- | :--- | :--- |
| **Encapsulation** | [`com.iwms.model.Product`](iwms-core/src/main/java/com/iwms/model/Product.java)<br>[`Batch`](iwms-core/src/main/java/com/iwms/model/Batch.java) | All domain fields are `private`; business invariants enforced through constructors and setters; [`StockMovement`](iwms-core/src/main/java/com/iwms/model/StockMovement.java) is completely immutable; collections return unmodifiable defensive views (`Collections.unmodifiableList`). |
| **Abstraction** | [`com.iwms.model.Product`](iwms-core/src/main/java/com/iwms/model/Product.java) | Abstract root class declaring abstract contracts: `calculateStorageCost(double days)` and `getHandlingRules()`, leaving category-specific implementation to subclasses. |
| **Inheritance & Polymorphism** | [`PerishableProduct`](iwms-core/src/main/java/com/iwms/model/PerishableProduct.java)<br>[`ElectronicProduct`](iwms-core/src/main/java/com/iwms/model/ElectronicProduct.java)<br>[`FragileProduct`](iwms-core/src/main/java/com/iwms/model/FragileProduct.java)<br>[`BulkProduct`](iwms-core/src/main/java/com/iwms/model/BulkProduct.java) | Specialized subclasses overriding polymorphic behaviors. For example: `PerishableProduct` factors refrigeration coefficients and shelf-life decay into storage costs, while `FragileProduct` enforces specialized air-cushioning and padding overheads. |
| **Creational Design Pattern: Builder** | [`Product.ProductBuilder`](iwms-core/src/main/java/com/iwms/model/Product.java) | Fluent recursive builder with strict validation verifying positive quantities, non-null SKUs, valid reorder thresholds, and bin locations before instantiation. |
| **Behavioral Design Pattern: Strategy** | [`PickStrategy`](iwms-core/src/main/java/com/iwms/service/PickStrategy.java)<br>[`AlertStrategy`](iwms-core/src/main/java/com/iwms/service/AlertStrategy.java)<br>[`ReorderStrategy`](iwms-core/src/main/java/com/iwms/service/ReorderStrategy.java)<br>[`PricingStrategy`](iwms-core/src/main/java/com/iwms/service/PricingStrategy.java) | Runtime-interchangeable strategy algorithms for picking, inventory alerts, procurement reorders, and customer pricing without modifying core service classes. |
| **Interfaces with Multiple Implementations** | Strategy Implementations | &bull; [`FefoPickStrategy`](iwms-core/src/main/java/com/iwms/service/FefoPickStrategy.java) (First-Expired-First-Out) vs [`FifoPickStrategy`](iwms-core/src/main/java/com/iwms/service/FifoPickStrategy.java) (First-In-First-Out)<br>&bull; [`LowStockAlertStrategy`](iwms-core/src/main/java/com/iwms/service/LowStockAlertStrategy.java) vs [`ExpiringBatchAlertStrategy`](iwms-core/src/main/java/com/iwms/service/ExpiringBatchAlertStrategy.java)<br>&bull; [`MinMaxReorderStrategy`](iwms-core/src/main/java/com/iwms/service/MinMaxReorderStrategy.java) vs [`VelocityReorderStrategy`](iwms-core/src/main/java/com/iwms/service/VelocityReorderStrategy.java)<br>&bull; [`AverageCostPricingStrategy`](iwms-core/src/main/java/com/iwms/service/AverageCostPricingStrategy.java) vs [`FifoPricingStrategy`](iwms-core/src/main/java/com/iwms/service/FifoPricingStrategy.java) |
| **Domain Interfaces** | [`Storable`](iwms-core/src/main/java/com/iwms/model/Storable.java)<br>[`Auditable`](iwms-core/src/main/java/com/iwms/model/Auditable.java)<br>[`Exportable`](iwms-core/src/main/java/com/iwms/model/Exportable.java) | Modular behavior segregation: `Storable` handles volume & storage metrics, `Auditable` exposes tamper-evident identity strings, `Exportable` provides standardized CSV serialization contracts. |
| **Java Collections Framework** | [`InventoryService`](iwms-core/src/main/java/com/iwms/service/InventoryService.java) | &bull; `HashMap` for O(1) catalog indexing and bin assignment<br>&bull; `TreeMap` for chronological time-travel inventory replay via `headMap(targetInstant)`<br>&bull; `ArrayDeque` for high-performance LIFO undo transaction stack<br>&bull; `PriorityQueue` with custom min/max comparators for alert urgency ranking<br>&bull; `LinkedHashMap` overriding `removeEldestEntry()` for an O(1) LRU recently-viewed SKU cache |
| **Concurrency & Thread-Safety** | [`InventoryService`](iwms-core/src/main/java/com/iwms/service/InventoryService.java) | `ReentrantReadWriteLock` with separate read locks for simultaneous non-blocking floor readers and write locks ensuring ACID consistency during stock transfers and movements. |
| **Java Streams & Functional Lambdas** | [`InventoryService`](iwms-core/src/main/java/com/iwms/service/InventoryService.java) | Pipeline operations utilizing `Collectors.groupingBy`, `summingInt`, `summingDouble`, custom comparator chains, and `flatMap` for warehouse valuation and movement analytics. |
| **Repository Pattern & Polymorphic JSON** | [`Repository`](iwms-core/src/main/java/com/iwms/repository/Repository.java)<br>[`FileBackedRepository`](iwms-core/src/main/java/com/iwms/repository/FileBackedRepository.java) | Generic repository interface with in-memory caching and JSON persistence. Uses Jackson `@JsonTypeInfo` and `@JsonSubTypes` to cleanly persist and reconstruct `Product` polymorphic subclass hierarchies across server restarts. |
| **Atomic File Persistence** | [`AtomicFileWriter`](iwms-core/src/main/java/com/iwms/storage/AtomicFileWriter.java) | Zero-corruption storage engine: writes data to `.tmp` file, calls OS disk sync `FileOutputStream.getFD().sync()`, and performs atomic file replacement via `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`. |
| **Write-Ahead Log (WAL)** | [`MovementLedger`](iwms-core/src/main/java/com/iwms/storage/MovementLedger.java) | Immutable append-only transaction ledger (`ledger.log`) recording stock movements before memory commit. Capable of full crash replay and historical state reconstruction. |
| **Audit Logging** | [`AuditLogger`](iwms-core/src/main/java/com/iwms/storage/AuditLogger.java) | Append-only security and operational audit trail (`audit.log`) tracking actor, action, target entity, outcome, and metadata for compliance. |
| **Safe Backup & Recovery** | [`BackupManager`](iwms-core/src/main/java/com/iwms/storage/BackupManager.java) | Automated and on-demand compressed ZIP archives of `data/` directory. Features Zip-Slip path traversal vulnerability protection and a registered JVM shutdown hook (`Runtime.getRuntime().addShutdownHook`) for automated snapshots. |
| **Catalog Seeder** | [`DataSeeder`](iwms-core/src/main/java/com/iwms/storage/DataSeeder.java) | Deterministic startup seeder initializing multi-warehouse topology (Warehouse &rarr; Zones &rarr; Bins) and >200 realistic products with initial stock, batches, and price points. |
| **User Hierarchy & Dynamic RBAC** | [`User`](iwms-core/src/main/java/com/iwms/model/User.java)<br>[`AdminUser`](iwms-core/src/main/java/com/iwms/model/AdminUser.java)<br>[`ManagerUser`](iwms-core/src/main/java/com/iwms/model/ManagerUser.java)<br>[`EmployeeUser`](iwms-core/src/main/java/com/iwms/model/EmployeeUser.java) | Polymorphic permission modeling using high-efficiency `EnumSet<Permission>`. Enforces strict separation: `EMPLOYEE` accounts can view quantities and bins but have cost prices and profit margins redacted across all APIs and PDFs. |
| **PBKDF2 Password Cryptography** | [`Pbkdf2Authenticator`](iwms-core/src/main/java/com/iwms/auth/Pbkdf2Authenticator.java) | `PBKDF2WithHmacSHA256` hashing (100,000 iterations, 16-byte secure random salt, constant-time `MessageDigest.isEqual` comparison). Automatically locks accounts for 15 minutes after 5 consecutive failed login attempts. |
| **Session Token Management** | [`SessionManager`](iwms-core/src/main/java/com/iwms/auth/SessionManager.java) | Cryptographically secure 32-byte URL-safe base64 tokens with 8-hour sliding window expiration and immediate revocation on logout. |
| **Idempotent Offline Sync Engine** | [`SyncEngine`](iwms-core/src/main/java/com/iwms/sync/SyncEngine.java) | Deduplicates incoming client actions via unique `syncId`. In the event of offline stock conflicts (e.g., negative deficit), records conflict entries into a review log for manager resolution while preserving ledger integrity. |
| **OpenPDF Floor Report Engine** | [`PdfReportGenerator`](iwms-core/src/main/java/com/iwms/report/PdfReportGenerator.java) | Generates high-contrast monochrome A4 count sheets optimized for thermal or low-ink warehouse floor printers, including count entry boxes, employee signature lines, and dynamic RBAC cost-price masking. |
| **CSV Catalog Exporter** | [`CsvReportGenerator`](iwms-core/src/main/java/com/iwms/report/CsvReportGenerator.java) | Export and import catalog data in standard CSV format with role-aware financial masking. |
| **REST API & Web Host** | [`com.iwms.api.controller.*`](iwms-web/src/main/java/com/iwms/api/controller/) | Spring Boot 3 controllers for Auth, Products, Stock In/Out/Transfer/Undo, Time-Travel, Warehouses, Sync, Reports, and Backups. |
| **Pure Java Custom Auth Filter** | [`CustomAuthFilter`](iwms-web/src/main/java/com/iwms/api/filter/CustomAuthFilter.java) | Lightweight HTTP filter executing token validation via `SessionManager` without heavy Spring Security dependencies, binding authenticated `User` context to request thread. |
| **Role-Aware DTO Projections** | [`ProductDto`](iwms-web/src/main/java/com/iwms/api/dto/ProductDto.java) | Server-side field filtering that omits `costPrice` when the requesting user has the `EMPLOYEE` role. |
| **Custom Domain Exceptions** | [`com.iwms.exception.*`](iwms-core/src/main/java/com/iwms/exception/) | Checked exceptions ([`InsufficientStockException`](iwms-core/src/main/java/com/iwms/exception/InsufficientStockException.java), [`DuplicateSkuException`](iwms-core/src/main/java/com/iwms/exception/DuplicateSkuException.java), [`StorageException`](iwms-core/src/main/java/com/iwms/exception/StorageException.java), [`SyncConflictException`](iwms-core/src/main/java/com/iwms/exception/SyncConflictException.java)) and unchecked exceptions ([`UnauthorizedException`](iwms-core/src/main/java/com/iwms/exception/UnauthorizedException.java), [`ForbiddenException`](iwms-core/src/main/java/com/iwms/exception/ForbiddenException.java)). |
| **Glassmorphism UI Shell** | [`App.tsx`](iwms-ui/src/App.tsx)<br>[`index.css`](iwms-ui/src/index.css) | Frosted glass design (`backdrop-blur-xl`), animated gradient ambient blobs, dark/light theme switcher with `localStorage` persistence, responsive mobile-friendly layout. |
| **Floating Glass Dock** | [`Dock.tsx`](iwms-ui/src/components/layout/Dock.tsx) | macOS-inspired floating navigation dock with tab scaling, hover tooltips, and live pending offline sync count badges. |
| **Client-Side RBAC & Quick Login** | [`LoginPage.tsx`](iwms-ui/src/pages/LoginPage.tsx)<br>[`AuthContext.tsx`](iwms-ui/src/context/AuthContext.tsx) | Responsive login screen with 1-click Admin, Manager, and Employee demo profile switchers and session state persistence. |
| **PWA Offline Storage & Sync Engine** | [`db.ts`](iwms-ui/src/storage/db.ts)<br>[`sync.ts`](iwms-ui/src/storage/sync.ts) | IndexedDB (`idb`) outbox write-queueing, optimistic local stock adjustments, automatic background drain upon `online` network detection, and manager conflict triage panel. |
| **2D Warehouse Topology Map** | [`WarehouseMapPage.tsx`](iwms-ui/src/pages/WarehouseMapPage.tsx) | Visual 2D floor grid showing Zones, Bins, utilization fill percentage bars, color-coded threshold alerts, and bin inspection drawer. |
| **Real-time Inventory Management** | [`DashboardPage.tsx`](iwms-ui/src/pages/DashboardPage.tsx)<br>[`InventoryPage.tsx`](iwms-ui/src/pages/InventoryPage.tsx) | Recharts category allocation, low-stock radar, searchable/filterable glass data table, pagination, and instant Stock In/Out/Transfer modal. |
| **Time-Travel Ledger & LIFO Undo** | [`MovementsPage.tsx`](iwms-ui/src/pages/MovementsPage.tsx) | Interactive scrubbable time-travel slider querying `TreeMap.headMap()` to reconstruct inventory levels at any past instant; 1-click `ArrayDeque` undo button. |
| **Purchase Order Receiving** | [`OrdersPage.tsx`](iwms-ui/src/pages/OrdersPage.tsx) | Purchase order creation, supplier tracking, delivery dates, and 1-click dock receiving with automatic stock-in execution. |
| **Guided Cycle Count & Reconciliation** | [`CycleCountPage.tsx`](iwms-ui/src/pages/CycleCountPage.tsx) | Physical floor audit sheet with real-time variance computation (`Counted - System`) and 1-click ledger reconciliation. |
| **OpenPDF Live Preview & ZIP Backups** | [`ReportsPage.tsx`](iwms-ui/src/pages/ReportsPage.tsx) | In-browser preview of monochrome A4 count sheets, CSV downloads, and on-demand zip-slip-safe compressed backups. |
| **Global Command Palette** | [`CommandPalette.tsx`](iwms-ui/src/components/CommandPalette.tsx) | `Ctrl + K` global modal for instant keyboard navigation across all screens and rapid SKU search. |

---

## Quickstart Guide

### Prerequisites
- **Java Development Kit (JDK):** Version 17 or higher (tested on JDK 17, 21, and 26).
- **Maven:** Version 3.8+ (or use the included `mvnw` wrapper).
- *(Optional)* **Node.js & npm:** Only needed if modifying frontend source files in `iwms-ui/`. The production static web assets are already compiled and bundled directly into the executable JAR!

---

### Step 1: Clone the Repository
```bash
git clone https://github.com/Subhomoy-byte/Inventory-Management-System.git
cd Inventory-Management-System
```

---

### Step 2: Build the Multi-Module Project
Run the Maven package command from the repository root:
```bash
mvn clean package
```
*(Windows PowerShell equivalent: `.\mvnw.cmd clean package`)*

This compiles all modules, runs all 44 JUnit 5 unit and integration tests, and bundles the application into a standalone fat JAR at `iwms-web/target/iwms.jar`.

---

### Step 3: Run the Standalone Executable JAR
Launch the system with a single command:
```bash
java -jar iwms-web/target/iwms.jar
```

The system will start in ~5 seconds and output:
```
Tomcat initialized with port 8443 (http)
Adding welcome page: class path resource [static/index.html]
Tomcat started on port 8443 (http) with context path ''
Started IwmsApplication in 6.03 seconds
```

---

### Step 4: Open in Your Browser
Open your browser and navigate to:
```
http://localhost:8443
```

---

## Pre-Seeded Demo User Profiles

On startup, IWMS automatically provisions three test accounts with pre-configured RBAC roles:

| Role | Username | Password | Permissions & System Privileges |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | **Full Administrative Privileges:** Unrestricted access, unmasked cost prices & profit margins, user management, ZIP backup creation, system settings. |
| **Manager** | `manager` | `manager123` | **Operational Management:** Stock movements, purchase order creation/receiving, cycle count reconciliation, conflict review, floor reports with financial columns. |
| **Employee** | `employee` | `emp123` | **Floor Operations:** Stock In/Out/Transfer, physical floor counts. **Strictly Redacted:** Cost prices and profit margins are masked across all screens, API responses, and generated PDF reports. |

> **Tip:** The Login screen features 1-click quick-fill buttons for each role.

---

## Local Area Network (LAN) & Mobile Access Guide

IWMS is designed to operate seamlessly across an offline warehouse or facility Local Area Network:

1. **Find Host PC IP Address:**
   - **Windows:** Run `ipconfig` in PowerShell (e.g., `192.168.1.50`).
   - **Linux / macOS:** Run `ifconfig` or `ip a`.

2. **Connect Floor Devices:**
   - Ensure warehouse tablets, barcode scanners, and mobile phones are connected to the same Wi-Fi router (no internet required).
   - On the mobile browser, visit:
     ```
     http://<HOST_IP>:8443
     ```
     *(Example: `http://192.168.1.50:8443`)*

3. **Offline Mode & Auto-Sync:**
   - If a floor worker walks into a dead zone or disconnected area, IWMS automatically switches to **Offline Mode**.
   - Workers can continue recording Stock In, Stock Out, and Transfers — operations are queued safely in browser IndexedDB.
   - When the worker returns within Wi-Fi range, IWMS automatically flushes the outbox queue to the server with zero data loss!

---

## Comprehensive REST API Endpoints

All endpoints (except login) require `Authorization: Bearer <token>`:

### Authentication
- `POST /api/auth/login`: Authenticate with username and password; returns session token and role details.
- `POST /api/auth/logout`: Revoke active session token.

### Products & Catalog
- `GET /api/products`: Retrieve all products (cost prices dynamically masked for `EMPLOYEE`).
- `GET /api/products/{id}`: Retrieve product by ID.
- `GET /api/products/search?query={q}`: Real-time search across SKUs, names, and descriptions.

### Stock Operations
- `POST /api/stock/in`: Record inbound stock receipt.
- `POST /api/stock/out`: Record outbound stock dispatch.
- `POST /api/stock/transfer`: Move stock between warehouse bins.
- `POST /api/stock/undo`: LIFO undo of the last stock movement.
- `GET /api/stock/movements`: Retrieve sequential transaction ledger.
- `GET /api/stock/asof?timestamp={iso}`: Query reconstructed inventory snapshot at any historical instant via `TreeMap.headMap()`.

### Warehouses & Topology
- `GET /api/warehouses`: Retrieve full warehouse topology hierarchy (Warehouse &rarr; Zones &rarr; Bins).

### Offline Sync
- `POST /api/sync/batch`: Process batch of queued offline transactions with idempotent deduplication.
- `GET /api/sync/conflicts`: Retrieve unresolved inventory deficit conflicts.

### Reports & Backups
- `GET /api/reports/summary.pdf`: Stream monochrome OpenPDF floor count sheet (role-aware cost masking).
- `GET /api/reports/catalog.csv`: Export catalog as CSV file.
- `POST /api/backups/create`: Generate timestamped ZIP backup of `data/` directory.

---

## Verification & Testing Suite

IWMS maintains **100% test pass rate across 44 unit and integration tests**:

```bash
mvn test
```

### Test Suite Breakdown
- `com.iwms.SmokeTest`: Reactor verification (1 test)
- `com.iwms.model.ProductHierarchyTest`: Encapsulation, builder validation, polymorphism, storage costs (7 tests)
- `com.iwms.service.StrategyTests`: Strategy patterns for FIFO/FEFO picking, alerts, reorders, pricing (6 tests)
- `com.iwms.service.InventoryServiceTest`: Concurrency, undo stack, time travel, LRU cache, stream analytics (8 tests)
- `com.iwms.storage.FilePersistenceTest`: Atomic file writes, WAL append & replay, audit logging, backup/restore, seeder (6 tests)
- `com.iwms.auth.AuthAndSyncTest`: PBKDF2 cryptography, lockout policy, RBAC hierarchy, offline sync deduplication (6 tests)
- `com.iwms.report.ReportGeneratorTest`: OpenPDF generation, employee cost price masking, CSV export/import (3 tests)
- `com.iwms.api.ApiControllerTests`: Spring Boot REST API integration, custom auth filter, role projections, PDF streaming (7 tests)

---

## Project Roadmap

- [x] **Project Inception & Architecture Specification** (Docs: [Design Specification](docs/superpowers/specs/2026-11-08-inventory-warehouse-management-system-design.md))
- [x] **Task 1: Maven Multi-Module Scaffolding** (`iwms-core`, `iwms-web`, Maven Wrapper, JUnit 5 SmokeTest verified)
- [x] **Task 2: Core Domain Model, Enums & Product Hierarchy** (7 JUnit 5 tests passing: Builder, Encapsulation, Polymorphism, Immutability)
- [x] **Task 3: Domain Strategy Interfaces & Business Logic** (6 JUnit 5 tests passing: FEFO/FIFO Pick, Low Stock/Expiry Alerts, MinMax/Velocity Reorders, Pricing)
- [x] **Task 4: Warehouse Entities, Collections & Inventory Service** (8 JUnit 5 tests passing: Concurrency, Undo stack, LRU cache, PriorityQueue alerts, Time travel timeline, Streams analytics)
- [x] **Task 5: File Persistence Layer (Atomic Writes, Ledger, Audit, Backup/Restore)** (6 JUnit 5 tests passing: Atomic writes, WAL ledger append & replay, audit logging, file-backed repository restart recovery, zip backup & restore, 200+ product catalog seeder)
- [x] **Task 6: Authentication, User RBAC & Offline Sync Engine** (6 JUnit 5 tests passing: PBKDF2 hashing, account lockout, RBAC permission hierarchy, token sessions, offline idempotent sync, deficit conflict handling)
- [x] **Task 7: OpenPDF Summary Report Generator** (3 JUnit 5 tests passing: Monochrome A4 PDF count sheet generation, RBAC employee price masking, CSV catalog export/import)
- [x] **Task 8: Spring Boot REST API, Custom Auth Filter & HTTPS Keystore** (7 SpringBoot integration tests passing: Authentication, Token verification, Role-based cost price redaction, Inbound/Outbound REST mutations, PDF streaming. Total 44 multi-module tests passing)
- [x] **Task 9: Frontend Architecture & Glassmorphism Design System** (`iwms-ui`: React 19 + TypeScript + Vite 6 + Tailwind CSS + Framer Motion, frosted glass styling, theme provider, floating navigation dock, responsive login page with instant demo roles)
- [x] **Task 10: Core Frontend Screens & PWA Offline Sync Engine** (`DashboardPage` with Recharts & KPI counters, `InventoryPage` with glass table, search, filters & price masking, `WarehouseMapPage` 2D zone & bin fill grid, `StockMovementModal` with optimistic offline queueing, `idb` IndexedDB outbox & `ConflictReviewPanel`)
- [x] **Task 11: Advanced Operational Features** (`MovementsPage` with time-travel slider querying `asof` timeline, `OrdersPage` with 1-click dock receiving, `CycleCountPage` with live variance reconciliation, `ReportsPage` with live OpenPDF preview & ZIP backup, `CommandPalette` with `Ctrl + K`)
- [x] **Task 12: Production Packaging, End-to-End Verification & Comprehensive Documentation** (Single executable JAR packaging, static asset bundling, 44/44 tests verified green)

---

## License
Open-source academic college project. Free for educational and commercial use under the MIT License.
