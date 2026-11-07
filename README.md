# Inventory & Warehouse Management System (IWMS)

A full-stack, offline-capable inventory and warehouse management system designed to run on a local PC and serve mobile and desktop clients over a local area network (LAN) without requiring an internet connection or external SQL database.

## System Highlights
- **Single-Command Launch:** Packaged as a single self-contained executable JAR (`java -jar iwms.jar`).
- **Academic Java OOP Showcase:** Pure Java 17+ core domain with strict demonstrations of Encapsulation, Abstraction, Inheritance, Polymorphism, Design Patterns, Interfaces with multiple implementations, Java Collections, Concurrency (`ReentrantReadWriteLock`), and atomic file-based persistence.
- **Zero Framework Leakage:** Domain packages (`model`, `service`, `repository`, `storage`) contain zero Spring Framework or Jakarta imports.
- **Offline-First PWA:** React + Vite + Tailwind CSS glassmorphism UI with service worker caching, IndexedDB write-queueing, and automatic idempotent sync upon LAN reconnection.
- **Warehouse Floor Tooling:** OpenPDF monochrome A4 physical count sheets and ZXing barcode/QR utilities.

---

## Where Each Java Concept Lives

| Java Concept | Location in Codebase | Description & Usage |
| :--- | :--- | :--- |
| **Encapsulation** | [`com.iwms.model.Product`](iwms-core/src/main/java/com/iwms/model/Product.java), [`Batch`](iwms-core/src/main/java/com/iwms/model/Batch.java) | All fields private; validation in constructors/setters; immutable [`StockMovement`](iwms-core/src/main/java/com/iwms/model/StockMovement.java); unmodifiable collection views |
| **Abstraction** | [`com.iwms.model.Product`](iwms-core/src/main/java/com/iwms/model/Product.java) | Abstract class with abstract methods (`calculateStorageCost()`, `getHandlingRules()`) |
| **Inheritance & Polymorphism** | [`PerishableProduct`](iwms-core/src/main/java/com/iwms/model/PerishableProduct.java), [`ElectronicProduct`](iwms-core/src/main/java/com/iwms/model/ElectronicProduct.java), [`FragileProduct`](iwms-core/src/main/java/com/iwms/model/FragileProduct.java), [`BulkProduct`](iwms-core/src/main/java/com/iwms/model/BulkProduct.java) | Subclasses overriding storage cost calculation and specialized handling rules |
| **Design Patterns (Creational)** | [`Product.ProductBuilder`](iwms-core/src/main/java/com/iwms/model/Product.java) | Fluent recursive builder pattern enforcing parameter validation |
| **Design Patterns (Behavioral: Strategy)** | [`PickStrategy`](iwms-core/src/main/java/com/iwms/service/PickStrategy.java), [`AlertStrategy`](iwms-core/src/main/java/com/iwms/service/AlertStrategy.java), [`ReorderStrategy`](iwms-core/src/main/java/com/iwms/service/ReorderStrategy.java), [`PricingStrategy`](iwms-core/src/main/java/com/iwms/service/PricingStrategy.java) | Pluggable strategy algorithms with runtime substitution |
| **Interfaces with 2+ Impls** | Strategy implementations | [`FefoPickStrategy`](iwms-core/src/main/java/com/iwms/service/FefoPickStrategy.java) vs [`FifoPickStrategy`](iwms-core/src/main/java/com/iwms/service/FifoPickStrategy.java); [`LowStockAlertStrategy`](iwms-core/src/main/java/com/iwms/service/LowStockAlertStrategy.java) vs [`ExpiringBatchAlertStrategy`](iwms-core/src/main/java/com/iwms/service/ExpiringBatchAlertStrategy.java); [`MinMaxReorderStrategy`](iwms-core/src/main/java/com/iwms/service/MinMaxReorderStrategy.java) vs [`VelocityReorderStrategy`](iwms-core/src/main/java/com/iwms/service/VelocityReorderStrategy.java); [`AverageCostPricingStrategy`](iwms-core/src/main/java/com/iwms/service/AverageCostPricingStrategy.java) vs [`FifoPricingStrategy`](iwms-core/src/main/java/com/iwms/service/FifoPricingStrategy.java) |
| **Interfaces (Domain)** | [`Storable`](iwms-core/src/main/java/com/iwms/model/Storable.java), [`Auditable`](iwms-core/src/main/java/com/iwms/model/Auditable.java), [`Exportable`](iwms-core/src/main/java/com/iwms/model/Exportable.java) | Abstract behavioral contracts for storage, auditing, and CSV export |
| **Java Collections** | [`InventoryService`](iwms-core/src/main/java/com/iwms/service/InventoryService.java) | `HashMap` for O(1) catalog/bins; `TreeMap` for time-travel ledger (`headMap`); `ArrayDeque` for LIFO undo stack; `PriorityQueue` for alert urgency; `LinkedHashMap` for LRU recently-viewed cache |
| **Concurrency** | [`InventoryService`](iwms-core/src/main/java/com/iwms/service/InventoryService.java) | `ReentrantReadWriteLock` with separate readLock for concurrent readers and writeLock for atomic stock transitions |
| **Streams & Lambdas** | [`InventoryService`](iwms-core/src/main/java/com/iwms/service/InventoryService.java) | `Collectors.groupingBy`, `summingDouble`, `summingInt`, `flatMap`, custom comparator sorting for top movers and valuations |
| **Repository Pattern** | [`Repository`](iwms-core/src/main/java/com/iwms/repository/Repository.java), [`InMemoryRepository`](iwms-core/src/main/java/com/iwms/repository/InMemoryRepository.java), [`FileBackedRepository`](iwms-core/src/main/java/com/iwms/repository/FileBackedRepository.java) | Generic repository abstraction with in-memory concurrent caching and atomic JSON file persistence with polymorphic deserialization |
| **Atomic File Persistence** | [`AtomicFileWriter`](iwms-core/src/main/java/com/iwms/storage/AtomicFileWriter.java) | Prevents corruption via `.tmp` staging, OS disk cache sync (`getFD().sync()`), and atomic rename (`ATOMIC_MOVE`) |
| **Write-Ahead Log (WAL)** | [`MovementLedger`](iwms-core/src/main/java/com/iwms/storage/MovementLedger.java) | Append-only sequential TSV log for immutable stock movements and crash-recovery state replay |
| **Audit Logging** | [`AuditLogger`](iwms-core/src/main/java/com/iwms/storage/AuditLogger.java) | Append-only security and operational audit trail tracking actor, action, target, outcome, and metadata |
| **Backup & Recovery** | [`BackupManager`](iwms-core/src/main/java/com/iwms/storage/BackupManager.java) | Automated and on-demand compressed ZIP backups of `data/`, zip-slip prevention, and JVM shutdown hook |
| **Data Seeder** | [`DataSeeder`](iwms-core/src/main/java/com/iwms/storage/DataSeeder.java) | Idempotent bootstrap seeding of warehouse topology (Warehouse -> Zones -> Bins) and >200 realistic products with initial stock |
| **User Hierarchy & RBAC** | [`User`](iwms-core/src/main/java/com/iwms/model/User.java), [`AdminUser`](iwms-core/src/main/java/com/iwms/model/AdminUser.java), [`ManagerUser`](iwms-core/src/main/java/com/iwms/model/ManagerUser.java), [`EmployeeUser`](iwms-core/src/main/java/com/iwms/model/EmployeeUser.java) | Polymorphic permissions using high-performance `EnumSet<Permission>`; employee role strictly masks financial costs and margins |
| **PBKDF2 Cryptography** | [`Pbkdf2Authenticator`](iwms-core/src/main/java/com/iwms/auth/Pbkdf2Authenticator.java) | `PBKDF2WithHmacSHA256` (100,000 iterations, 16-byte salt, constant-time compare, 5-attempt automatic 15-minute account lockout) |
| **Session Management** | [`SessionManager`](iwms-core/src/main/java/com/iwms/auth/SessionManager.java) | Cryptographically secure 32-byte URL-safe tokens, 8-hour sliding window expiry, and immediate revocation |
| **Offline Sync Engine** | [`SyncEngine`](iwms-core/src/main/java/com/iwms/sync/SyncEngine.java) | Idempotent deduplication of queued client actions via `syncId`, automated conflict logging on inventory deficits for manager resolution |
| **Custom Exceptions** | [`com.iwms.exception.*`](iwms-core/src/main/java/com/iwms/exception/) | Checked exceptions ([`InsufficientStockException`](iwms-core/src/main/java/com/iwms/exception/InsufficientStockException.java), [`DuplicateSkuException`](iwms-core/src/main/java/com/iwms/exception/DuplicateSkuException.java), [`StorageException`](iwms-core/src/main/java/com/iwms/exception/StorageException.java), [`SyncConflictException`](iwms-core/src/main/java/com/iwms/exception/SyncConflictException.java)) and unchecked exceptions ([`UnauthorizedException`](iwms-core/src/main/java/com/iwms/exception/UnauthorizedException.java), [`ForbiddenException`](iwms-core/src/main/java/com/iwms/exception/ForbiddenException.java)) |

---

## Project Status & Roadmap

- [x] **Project Inception & Architecture Specification** (Docs: [Design Specification](docs/superpowers/specs/2026-11-08-inventory-warehouse-management-system-design.md))
- [x] **Task 1: Maven Multi-Module Scaffolding** (`iwms-core`, `iwms-web`, Maven Wrapper, JUnit 5 SmokeTest verified)
- [x] **Task 2: Core Domain Model, Enums & Product Hierarchy** (7 JUnit 5 tests passing: Builder, Encapsulation, Polymorphism, Immutability)
- [x] **Task 3: Domain Strategy Interfaces & Business Logic** (6 JUnit 5 tests passing: FEFO/FIFO Pick, Low Stock/Expiry Alerts, MinMax/Velocity Reorders, Pricing)
- [x] **Task 4: Warehouse Entities, Collections & Inventory Service** (8 JUnit 5 tests passing: Concurrency, Undo stack, LRU cache, PriorityQueue alerts, Time travel timeline, Streams analytics)
- [x] **Task 5: File Persistence Layer (Atomic Writes, Ledger, Audit, Backup/Restore)** (6 JUnit 5 tests passing: Atomic writes, WAL ledger append & replay, audit logging, file-backed repository restart recovery, zip backup & restore, 200+ product catalog seeder)
- [x] **Task 6: Authentication, User RBAC & Offline Sync Engine** (6 JUnit 5 tests passing: PBKDF2 hashing, account lockout, RBAC permission hierarchy, token sessions, offline idempotent sync, deficit conflict handling. Total 34 core tests passing)
- [x] **Phase 1: Java Core Domain & JUnit 5 Tests**
- [x] **Phase 2: File Persistence Layer (Ledger, Audit, Backup)**
- [/] **Phase 3: Authentication, Session & REST API**
- [ ] **Phase 4: OpenPDF Summary Report Generator**
- [ ] **Phase 5: Frontend Shell & Glassmorphism Design System**
- [ ] **Phase 6: Core Frontend Screens & PWA Offline Engine**
- [ ] **Phase 7: Advanced Business Logic (Time Travel, FEFO, Reorders)**
- [ ] **Phase 8: Motion, Refinement & Performance Pass**
- [ ] **Phase 9: STRETCH Features & Barcode Scanning**
- [ ] **Phase 10: Final Packaging & Verification**

---

## License
Academic College Project.
