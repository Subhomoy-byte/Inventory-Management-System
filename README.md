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
| **Encapsulation** | `com.iwms.model.*` | All fields private; validation in constructors/setters; immutable `StockMovement`; unmodifiable collection views; password hash/salt never exposed |
| **Abstraction** | `com.iwms.model.Product`, `com.iwms.model.User` | Abstract classes with abstract methods (`calculateStorageCost()`, `getHandlingRules()`, `getPermissions()`) |
| **Inheritance & Polymorphism** | `com.iwms.model.PerishableProduct`, `ElectronicProduct`, `FragileProduct`, `BulkProduct`; `AdminUser`, `ManagerUser`, `EmployeeUser` | Subclasses implementing domain-specific behavior and role-based permission sets |
| **Design Patterns** | `Product.ProductBuilder`, `Repository<T, ID>`, Strategy interfaces | Builder pattern, Repository pattern, Strategy pattern (Pick, Alert, Reorder, Pricing) |
| **Interfaces (2+ Impls)** | `PickStrategy`, `AlertStrategy`, `PricingStrategy`, `ReorderStrategy`, `ReportGenerator`, `Authenticator`, `Repository` | Polymorphic strategies with runtime substitution (e.g. `FefoPickStrategy` vs `FifoPickStrategy`) |
| **Collections** | `com.iwms.service.*`, `com.iwms.repository.*` | `HashMap` (SKU index), `TreeMap` (time-travel views), `PriorityQueue` (alert priority), `ArrayDeque` (undo stack), `HashSet` (sync deduplication), `EnumSet` (permissions), `LinkedHashMap` (LRU cache) |
| **File Handling & Atomic Writes** | `com.iwms.storage.*` | `java.nio.file`, atomic file replacement (`ATOMIC_MOVE`), append-only `ledger.log` & `audit.log`, automated backup & restore |
| **Concurrency** | `com.iwms.service.InventoryService` | `ReentrantReadWriteLock` managing concurrent readers and serialized writers |
| **Custom Exceptions** | `com.iwms.exception.*` | `InsufficientStockException`, `DuplicateSkuException`, `StorageException`, `SyncConflictException`, `UnauthorizedException`, `ForbiddenException` |

---

## Project Status & Roadmap

- [x] **Project Inception & Architecture Specification** (Docs: [Design Specification](docs/superpowers/specs/2026-11-08-inventory-warehouse-management-system-design.md))
- [ ] **Phase 1: Java Core Domain & JUnit 5 Tests**
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
