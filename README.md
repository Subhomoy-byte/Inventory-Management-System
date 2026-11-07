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
| **Custom Exceptions** | [`com.iwms.exception.*`](iwms-core/src/main/java/com/iwms/exception/) | Checked exceptions ([`InsufficientStockException`](iwms-core/src/main/java/com/iwms/exception/InsufficientStockException.java), [`DuplicateSkuException`](iwms-core/src/main/java/com/iwms/exception/DuplicateSkuException.java), [`StorageException`](iwms-core/src/main/java/com/iwms/exception/StorageException.java), [`SyncConflictException`](iwms-core/src/main/java/com/iwms/exception/SyncConflictException.java)) and unchecked exceptions ([`UnauthorizedException`](iwms-core/src/main/java/com/iwms/exception/UnauthorizedException.java), [`ForbiddenException`](iwms-core/src/main/java/com/iwms/exception/ForbiddenException.java)) |

---

## Project Status & Roadmap

- [x] **Project Inception & Architecture Specification** (Docs: [Design Specification](docs/superpowers/specs/2026-11-08-inventory-warehouse-management-system-design.md))
- [x] **Task 1: Maven Multi-Module Scaffolding** (`iwms-core`, `iwms-web`, Maven Wrapper, JUnit 5 SmokeTest verified)
- [x] **Task 2: Core Domain Model, Enums & Product Hierarchy** (7 JUnit 5 tests passing: Builder, Encapsulation, Polymorphism, Immutability)
- [x] **Task 3: Domain Strategy Interfaces & Business Logic** (6 JUnit 5 tests passing: FEFO/FIFO Pick, Low Stock/Expiry Alerts, MinMax/Velocity Reorders, Pricing)
- [/] **Phase 1: Java Core Domain & JUnit 5 Tests**
- [ ] **Phase 2: File Persistence Layer (Ledger, Audit, Backup)**
- [ ] **Phase 3: Authentication, Session & REST API**
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
