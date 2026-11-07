# Design Specification: Inventory & Warehouse Management System (IWMS)

**Date:** 2026-11-08  
**Author:** DeepMind Agent & Subhomoy  
**Status:** Approved for Implementation Planning  
**Target Repository:** [Subhomoy-byte/Inventory-Management-System](https://github.com/Subhomoy-byte/Inventory-Management-System)  

---

## 1. Executive Summary & Goals
The Inventory & Warehouse Management System (IWMS) is an offline-capable, LAN-accessible full-stack application designed to run from a single command (`java -jar iwms.jar`). It serves desktop and mobile browsers over HTTPS on a local area network without requiring an external internet connection or a SQL database.

### Core Objectives:
1. **Academic Java OOP Showcase:** The backend architecture acts as a primary academic deliverable, providing explicit, idiomatic demonstrations of Encapsulation, Abstraction, Inheritance, Polymorphism, Design Patterns, Interfaces with multiple implementations, Java Collections, Concurrency (`ReentrantReadWriteLock`), and atomic file-based persistence.
2. **Framework Decoupling:** Core domain logic (`model`, `service`, `repository`, `storage`) contains strictly **zero** Spring Framework or Jakarta dependencies.
3. **Offline-First PWA:** Service worker application shell caching combined with IndexedDB write-queueing and idempotent automatic sync on reconnect.
4. **Physical Warehouse Tooling:** OpenPDF monochrome floor count sheets and ZXing barcode/QR utilities.
5. **Continuous Documentation & Version Control:** Git commits and README updates after every delivery phase.

---

## 2. System Architecture & Modules

```
Inventory-Management-System/
├── pom.xml                               # Root parent aggregator
├── iwms-core/                            # Pure Java 17+ Domain Library (Zero Spring)
│   ├── pom.xml                           # Dependencies: Jackson (storage only), OpenPDF, ZXing, JUnit 5
│   └── src/
│       ├── main/java/com/iwms/
│       │   ├── model/                    # Entities, Records, Enums, Builders
│       │   ├── service/                  # Business logic & Domain Services
│       │   ├── repository/               # Repository interfaces & In-Memory / File-backed impls
│       │   ├── storage/                  # Atomic file operations, Ledger, Audit, Backup/Restore
│       │   ├── report/                   # PDF & CSV generation engine
│       │   ├── auth/                     # PBKDF2 hashing, Session store, Token verifier
│       │   ├── sync/                     # Offline sync engine & conflict resolver
│       │   ├── exception/                # Custom checked & unchecked domain exceptions
│       │   └── util/                     # Validation, date math, safe collections
│       └── test/java/com/iwms/           # Comprehensive JUnit 5 test suite
├── iwms-ui/                              # React + Vite + Tailwind CSS + PWA
│   ├── package.json
│   ├── vite.config.ts
│   └── src/                              # Glassmorphism UI, idb outbox, sync worker
└── iwms-web/                             # Spring Boot 3 HTTP Host & Static Server
    ├── pom.xml                           # Depends on iwms-core; Spring Boot Starter Web
    └── src/
        ├── main/java/com/iwms/api/       # REST Controllers, CustomAuthFilter, DTOs
        └── main/resources/
            ├── application.yml           # Port 8443, SSL Keystore, static paths
            └── static/                   # Bundled production build from iwms-ui
```

---

## 3. Java Domain Specification (OOP Academic Deliverables)

### 3.1 Packages
The package hierarchy is strictly separated into single-responsibility namespaces:
- `com.iwms.model`
- `com.iwms.service`
- `com.iwms.repository`
- `com.iwms.storage`
- `com.iwms.report`
- `com.iwms.auth`
- `com.iwms.sync`
- `com.iwms.exception`
- `com.iwms.util`

### 3.2 Product Hierarchy (Abstraction, Polymorphism & Encapsulation)
- **`abstract class Product implements Storable, Auditable, Exportable`:**
  - Private fields: `sku` (String), `name` (String), `category` (Category enum), `unitPrice` (double), `costPrice` (double), `minStockLevel` (int), `createdAt` (Instant), `createdBy` (String).
  - Validation: Constructor and setters reject blank SKUs, empty names, and negative prices or quantities.
  - Builder Pattern: Nested static `ProductBuilder` providing fluent creation.
  - Abstract Methods:
    - `public abstract double calculateStorageCost();`
    - `public abstract List<String> getHandlingRules();`
- **Subclasses:**
  1. `PerishableProduct`:
     - Fields: `shelfLifeDays` (int), `storageTempCelsius` (double), `requiresRefrigeration` (boolean).
     - Polymorphic behavior: Storage cost calculates cooling energy multiplier; handling rules enforce cold chain integrity and First-Expired-First-Out (FEFO) picking.
  2. `ElectronicProduct`:
     - Fields: `warrantyMonths` (int), `voltage` (int), `hasLithiumBattery` (boolean).
     - Polymorphic behavior: Storage cost includes ESD protection and climate control; handling rules mandate grounding precautions.
  3. `FragileProduct`:
     - Fields: `fragilityIndex` (1-10), `maxStackHeight` (int).
     - Polymorphic behavior: Surcharge added for single-tier rack placement; handling rules require bubble wrap and "Fragile" labeling.
  4. `BulkProduct`:
     - Fields: `weightKg` (double), `volumeCubicMeters` (double), `unitOfMeasure` (String).
     - Polymorphic behavior: Storage cost computed per cubic meter and floor loading capacity.

### 3.3 User & RBAC Hierarchy
- **`abstract class User`:**
  - Private fields: `id` (String), `username` (String), `passwordHash` (String), `salt` (byte[]), `fullName` (String), `role` (Role enum), `failedAttempts` (int), `lockedUntil` (Instant).
  - Encapsulation: `passwordHash` and `salt` have package-private or restricted visibility and are never exposed via API responses.
  - Abstract Method: `public abstract Set<Permission> getPermissions();`
- **Subclasses:**
  1. `AdminUser`: Returns `EnumSet.allOf(Permission.class)`.
  2. `ManagerUser`: Returns all permissions except `MANAGE_USERS` and `DELETE_PRODUCTS`.
  3. `EmployeeUser`: Returns `STOCK_IN`, `STOCK_OUT`, `STOCK_TRANSFER`, `CYCLE_COUNT`, `VIEW_ALERTS`, `VIEW_OWN_MOVEMENTS`, `GENERATE_FLOOR_REPORT`. Financial valuation permissions are strictly omitted.

### 3.4 Interfaces & Multiple Implementations

| Interface | Method Signature Sample | Impl 1 | Impl 2 |
| :--- | :--- | :--- | :--- |
| `Repository<T, ID>` | `Optional<T> findById(ID id); void save(T entity);` | `InMemoryRepository<T, ID>` | `FileBackedRepository<T, ID>` |
| `PickStrategy` | `List<Batch> selectBatches(List<Batch> batches, int qty);` | `FefoPickStrategy` | `FifoPickStrategy` |
| `AlertStrategy` | `List<Alert> evaluate(Product p, int currentStock, List<Batch> b);` | `LowStockAlertStrategy` | `ExpiringBatchAlertStrategy` |
| `PricingStrategy` | `double calculateCostOfGoodsSold(String sku, int qty);` | `FifoPricingStrategy` | `AverageCostPricingStrategy` |
| `ReorderStrategy` | `ReorderSuggestion evaluateReorder(Product p, int stock, int dailyDemand);` | `MinMaxReorderStrategy` | `VelocityReorderStrategy` |
| `ReportGenerator` | `byte[] generate(ReportContext ctx);` | `PdfReportGenerator` (OpenPDF) | `CsvReportGenerator` |
| `Authenticator` | `Optional<User> authenticate(String user, String pass);` | `Pbkdf2Authenticator` | `MockTestAuthenticator` |

### 3.5 Java Collections Strategy
1. **`HashMap<String, Product>`:** Primary $O(1)$ fast lookup index by SKU.
2. **`TreeMap<Instant, StockMovement>`:** Chronological stock movements index supporting time-travel inventory reconstruction.
3. **`ArrayList<T>`:** Sequential storage for batches, audit trail segments, and purchase order lines.
4. **`HashSet<String>`:** Stores processed offline sync operation IDs for idempotent deduplication.
5. **`EnumSet<Permission>`:** High-speed bitmask evaluation of user permissions.
6. **`PriorityQueue<Alert>`:** Min/max priority queue prioritizing critical stock alerts (out of stock, expiring within 48h).
7. **`Deque<StockOperation>` (`ArrayDeque`):** LIFO stack enabling one-click undo of the most recent stock movement.
8. **`LinkedHashMap<String, Product>`:** LRU-style cache tracking recently viewed items.
9. **`Map<String, Session>` (`ConcurrentHashMap`):** Thread-safe session registry.
10. **Streams & Lambdas:** Aggregate computations for Category Valuation, Top Movers, and ABC demand classification.

### 3.6 Concurrency & Thread-Safety
- An instance of `java.util.concurrent.locks.ReentrantReadWriteLock` governs core in-memory state:
  - `readLock()` acquired during inventory lookups, product searches, dashboard metrics, and PDF generation.
  - `writeLock()` acquired during stock updates (`IN`, `OUT`, `TRANSFER`, `ADJUST`), order fulfillment, and batch sync application.

### 3.7 Custom Checked & Unchecked Exceptions
- `InsufficientStockException` (Checked): Attempting to issue more stock than available in the specified batch/bin.
- `DuplicateSkuException` (Checked): Attempting to create a product with an existing SKU.
- `StorageException` (Checked): File I/O failure or corruption during atomic persistence.
- `SyncConflictException` (Checked): Discrepancy during offline batch operation processing.
- `UnauthorizedException` (Unchecked, HTTP 401): Missing, invalid, or expired session token.
- `ForbiddenException` (Unchecked, HTTP 403): User lacks permission for the requested action.

---

## 4. Persistence & File Handling (No Database)

### 4.1 Storage Layout (`data/`)
- `data/products.json`: Product records and metadata.
- `data/warehouses.json`: Warehouses, Zones, and Bins with capacity levels.
- `data/users.json`: User accounts, salted password hashes, roles.
- `data/suppliers.json`: Supplier contact details and lead times.
- `data/orders.json`: Purchase orders and status history.
- `data/ledger.log`: Append-only chronological stream of all stock movements.
- `data/audit.log`: Append-only compliance log: `[ISO-8601] [USER] [ACTION] [TARGET] [DETAILS]`.
- `data/backups/`: Directory for timestamped `.zip` archives.

### 4.2 Atomic File Writes
To prevent corruption caused by power interruptions or abnormal JVM exits:
1. Serialize payload into a temporary sibling file: `data/<file>.json.tmp`.
2. Flush and force writes to underlying physical storage: `FileChannel.force(true)`.
3. Atomically move to target path:
   ```java
   Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
   ```

### 4.3 Automated Backup & Restore
- A JVM shutdown hook (`Runtime.getRuntime().addShutdownHook`) archives the `data/` directory into a timestamped zip archive on clean shutdown.
- Manual triggers via `POST /api/backup` and `POST /api/restore` with file integrity verification.

---

## 5. Security & Authentication

### 5.1 Password Hashing
- Algorithm: Salted PBKDF2 with HMAC-SHA256 (`PBKDF2WithHmacSHA256`).
- Iterations: 100,000.
- Salt: 16 cryptographically secure random bytes generated via `SecureRandom`.
- Output: Base64-encoded salt and hash. Plaintext passwords are never stored or logged.

### 5.2 Session Management
- Tokens: 256-bit cryptographically secure random strings sent via `Authorization: Bearer <token>`.
- Expiry: 8-hour sliding window.
- Lockout: 5 consecutive failed attempts locks the target username for 5 minutes.

### 5.3 Custom Security Filter
- Implemented as a lightweight `OncePerRequestFilter` without Spring Security dependencies.
- Intercepts requests matching `/api/**` (excluding `/api/auth/login`).
- Extracts token, validates session, verifies required `Permission`, and sets thread-local security context.
- Returns standard JSON errors on failure (`401 Unauthorized` / `403 Forbidden`).

### 5.4 Server-Side Valuation Masking
- The backend DTO projection layer checks the authenticated user's permissions.
- If the user is an `EmployeeUser`, `costPrice`, `unitPrice`, and calculated stock valuation fields are sanitized and serialized as `null` or omitted before sending the HTTP response.

---

## 6. Offline PWA & Sync Engine

### 6.1 Service Worker & Cache
- Configured via `vite-plugin-pwa` with Workbox.
- App shell (HTML, bundled JavaScript, CSS, SVGs, and fonts) cached in Cache Storage.
- Zero external CDN dependencies ensures the web app loads fully without internet.

### 6.2 IndexedDB Offline Queue
- Powered by `idb`:
  - `products_cache`: Local copy of catalog and stock for offline browsing.
  - `outbox_queue`: Stores queued actions when network is unavailable.
- Each queued item contains:
  - `operationId`: Client-generated UUID v4.
  - `timestamp`: Client epoch millisecond timestamp.
  - `type`: `STOCK_IN`, `STOCK_OUT`, `STOCK_TRANSFER`, `STOCK_ADJUST`, `CYCLE_COUNT`.
  - `payload`: Target SKU, batch, bin, quantity, and notes.

### 6.3 Sync Protocol (`POST /api/sync`)
1. Reconnection to LAN triggers automatic sync.
2. Server validates `operationId` against in-memory `HashSet<String> processedSyncIds`:
   - Match found &rarr; returns `DUPLICATE` (prevents double posting).
   - Valid mutation &rarr; updates inventory, appends to `ledger.log`, registers ID in `processedSyncIds` &rarr; returns `APPLIED`.
   - Discrepancy &rarr; logs to manager review queue (`/api/sync/conflicts`) &rarr; returns `CONFLICT` with rationale.

---

## 7. PDF Report Generator (Warehouse Floor Sheet)

- **Technology:** OpenPDF (`com.github.librepdf:openpdf`).
- **Formatting:** A4 portrait, high-contrast monochrome design for clipboard legibility.
- **Sections:**
  1. Header: Warehouse Title, Date & Time, Operator Username.
  2. Inventory KPI Summary: Total SKUs, Low Stock Count, Expiring Soon Count, Today's In/Out.
  3. Action Lists: Urgent reorder suggestions and batches nearing expiration.
  4. Physical Count Sheet: Grouped by Zone and Bin, showing SKU, Product Name, System Quantity, and a blank "Physical Count" box with check-boxes.
  5. Movement Log: Summary of today's movements.
  6. Sign-off block: Supervisor signature and date line.
- **Role Scoping:** When generated by an Employee, valuation sections are omitted.

---

## 8. Frontend UI/UX Design

- **Theme & Aesthetic:** Modern Glassmorphism with dark theme default and light theme toggle. Translucent panels (`backdrop-blur-md`), subtle borders (`border-white/10`), soft shadows, and CSS-animated gradient mesh blobs.
- **Navigation:** Floating glass dock with magnification on hover; glass pill tabs with sliding active indicators.
- **Key Screens:**
  1. Login: Glass card, demo credentials quick-fill buttons.
  2. Dashboard: Live KPI cards, low-stock radar, category breakdown charts.
  3. Inventory: Virtualized/paginated table, quick search, multi-faceted filters.
  4. Warehouse Zone Map: Interactive visual grid with color-coded fill levels and bin inspection drawer.
  5. Movements & Time-Travel: Historical timeline with interactive slider reconstructing past stock states.
  6. Suppliers & Orders: Purchase order creation, status badges, one-click receiving.
  7. Reports & PDF Preview: In-browser preview modal and download.
  8. Offline Status & Conflicts: Glass status chip (Online / Offline / Syncing) and manager conflict resolution panel.
  9. Command Palette: Keyboard navigation via `Ctrl + K`.

---

## 9. Phased Delivery Roadmap

Each phase will be implemented, verified with tests, committed to Git, and pushed to GitHub with updated README documentation:

1. **Phase 1: Java Core Domain & JUnit 5 Tests**  
   Domain models, abstract classes, polymorphic subclasses, records, builder pattern, interfaces with 2+ implementations, collections, concurrency lock, custom exceptions, unit tests.
2. **Phase 2: File Persistence Layer**  
   Atomic file writers, JSON serializers, ledger stream, audit logger, shutdown backup hook, CSV import/export.
3. **Phase 3: Authentication, Session & REST API**  
   PBKDF2 authenticator, token session manager, custom security filter, Spring Boot controllers, idempotent sync engine.
4. **Phase 4: OpenPDF Summary Report Generator**  
   Server-side PDF generation, A4 floor count sheet, role-based valuation masking.
5. **Phase 5: Frontend Shell & Glassmorphism Design System**  
   Vite + React + Tailwind + Framer Motion setup, floating dock, navigation, theme toggle, login screen.
6. **Phase 6: Core Frontend Screens & PWA Offline Engine**  
   Inventory table, warehouse zone map, stock in/out/transfer modals, IndexedDB outbox queue, service worker.
7. **Phase 7: Advanced Business Logic (SHOULD Features)**  
   Time-travel stock slider, batch FEFO auto-selection, reorder suggestions, guided cycle count with variance.
8. **Phase 8: Motion, Refinement & Performance Pass**  
   Framer motion micro-interactions, 3D card tilt, virtualized tables, 60fps performance check.
9. **Phase 9: STRETCH Features & Barcode Scanning**  
   ZXing barcode/QR generation, camera scanner integration, connect-a-device LAN QR modal.
10. **Phase 10: Final Verification, Packaging & Comprehensive README**  
    Multi-module Maven packaging into single `iwms.jar`, test coverage verification, README "Where each Java concept lives" mapping table, final Git push.
