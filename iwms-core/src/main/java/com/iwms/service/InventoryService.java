package com.iwms.service;

import com.iwms.exception.DuplicateSkuException;
import com.iwms.exception.InsufficientStockException;
import com.iwms.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * OOP Core: Thread-Safe Domain Inventory Service.
 * Demonstrates:
 * 1. Concurrency: ReentrantReadWriteLock for high-throughput concurrent readers and serialized writers.
 * 2. Collections: HashMap (SKU index), TreeMap (Time-travel ledger), ArrayDeque (Undo stack),
 *    PriorityQueue (Urgency alerts), LinkedHashMap (LRU cache), Streams/Lambdas for analytics.
 * 3. Strategy Pattern: Pluggable PickStrategy for batch allocation.
 */
public class InventoryService {

    // Concurrency: Read-Write Lock
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    // Collection 1: HashMap for O(1) product catalog index
    private final Map<String, Product> productCatalog = new HashMap<>();

    // Collection 2: HashMap with ArrayList for batch tracking
    private final Map<String, List<Batch>> productBatches = new HashMap<>();

    // Warehouse and Bin indexes
    private final Map<String, Warehouse> warehouses = new HashMap<>();
    private final Map<String, Bin> binIndex = new HashMap<>();

    // Collection 3: List for append-only sequential ledger and TreeMap for chronological time-travel views
    private final List<StockMovement> ledgerList = new ArrayList<>();
    private final TreeMap<Instant, List<StockMovement>> ledgerTimeline = new TreeMap<>();

    // Collection 4: ArrayDeque as a LIFO Undo Stack for rolling back stock operations
    private final Deque<StockMovement> undoStack = new ArrayDeque<>();

    // Collection 5: PriorityQueue for urgent alert ordering (Comparable Alert)
    private final PriorityQueue<Alert> alertQueue = new PriorityQueue<>();

    // Collection 6: LinkedHashMap with access-order for LRU recently viewed cache
    private static final int MAX_RECENT_ENTRIES = 20;
    private final Map<String, Product> recentlyViewed = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Product> eldest) {
            return size() > MAX_RECENT_ENTRIES;
        }
    };

    // Strategy instances
    private PickStrategy perishablePickStrategy = new FefoPickStrategy();
    private PickStrategy standardPickStrategy = new FifoPickStrategy();

    public InventoryService() {
    }

    private void recordMovement(StockMovement movement) {
        recordMovement(movement, true);
    }

    private void recordMovement(StockMovement movement, boolean recordInUndo) {
        ledgerList.add(movement);
        ledgerTimeline.computeIfAbsent(movement.timestamp(), k -> new ArrayList<>()).add(movement);
        if (recordInUndo) {
            undoStack.push(movement);
        }
    }

    public void addWarehouse(Warehouse warehouse) {
        writeLock.lock();
        try {
            Objects.requireNonNull(warehouse, "Warehouse cannot be null");
            warehouses.put(warehouse.getId(), warehouse);
            for (Zone zone : warehouse.getZones()) {
                for (Bin bin : zone.getBins()) {
                    binIndex.put(bin.getId(), bin);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public List<Warehouse> getWarehouses() {
        readLock.lock();
        try {
            return new ArrayList<>(warehouses.values());
        } finally {
            readLock.unlock();
        }
    }

    public Bin getBin(String binId) {
        readLock.lock();
        try {
            return binIndex.get(binId);
        } finally {
            readLock.unlock();
        }
    }

    public void registerProduct(Product product) throws DuplicateSkuException {
        writeLock.lock();
        try {
            Objects.requireNonNull(product, "Product cannot be null");
            if (productCatalog.containsKey(product.getSku())) {
                throw new DuplicateSkuException(product.getSku());
            }
            productCatalog.put(product.getSku(), product);
            productBatches.putIfAbsent(product.getSku(), new ArrayList<>());
        } finally {
            writeLock.unlock();
        }
    }

    public Product getProduct(String sku) {
        readLock.lock();
        try {
            return productCatalog.get(sku);
        } finally {
            readLock.unlock();
        }
    }

    public List<Product> getAllProducts() {
        readLock.lock();
        try {
            return new ArrayList<>(productCatalog.values());
        } finally {
            readLock.unlock();
        }
    }

    public int getCurrentStock(String sku) {
        readLock.lock();
        try {
            List<Batch> batches = productBatches.get(sku);
            if (batches == null || batches.isEmpty()) {
                return 0;
            }
            return batches.stream().mapToInt(Batch::getQuantity).sum();
        } finally {
            readLock.unlock();
        }
    }

    public List<Batch> getBatchesForProduct(String sku) {
        readLock.lock();
        try {
            List<Batch> batches = productBatches.get(sku);
            return batches != null ? new ArrayList<>(batches) : List.of();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Stock In Operation: Inbounds inventory to a target bin and batch.
     * Records immutable StockMovement, registers in timeline, and pushes to Undo stack.
     */
    public StockMovement stockIn(String sku, String batchNumber, String binId, int quantity,
                                 LocalDate expiryDate, String reason, String actingUsername)
            throws InsufficientStockException {
        writeLock.lock();
        try {
            Product product = productCatalog.get(sku);
            if (product == null) {
                throw new IllegalArgumentException("Unknown product SKU: " + sku);
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Stock in quantity must be positive");
            }

            Bin bin = binIndex.get(binId);
            if (bin == null) {
                throw new IllegalArgumentException("Unknown bin ID: " + binId);
            }

            // Update bin physical capacity
            bin.addStock(quantity, 0.0);

            // Update or create batch
            List<Batch> batches = productBatches.computeIfAbsent(sku, k -> new ArrayList<>());
            Optional<Batch> existingBatch = batches.stream()
                    .filter(b -> b.getBatchNumber().equalsIgnoreCase(batchNumber))
                    .findFirst();

            if (existingBatch.isPresent()) {
                existingBatch.get().adjustQuantity(quantity);
            } else {
                batches.add(new Batch(batchNumber, sku, LocalDate.now(), expiryDate, quantity));
            }

            // Create immutable movement record
            Instant now = Instant.now();
            StockMovement movement = new StockMovement(
                    "MOV-" + UUID.randomUUID().toString().substring(0, 8),
                    now,
                    sku,
                    batchNumber,
                    MovementType.IN,
                    quantity,
                    null,
                    binId,
                    reason,
                    actingUsername
            );

            // Record in collections
            recordMovement(movement);

            return movement;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Stock Out Operation: Dispatches stock using pluggable PickStrategy (FEFO for perishables, FIFO for others).
     */
    public StockMovement stockOut(String sku, int quantity, String reason, String actingUsername)
            throws InsufficientStockException {
        writeLock.lock();
        try {
            Product product = productCatalog.get(sku);
            if (product == null) {
                throw new IllegalArgumentException("Unknown product SKU: " + sku);
            }
            int available = getCurrentStock(sku);
            if (available < quantity) {
                throw new InsufficientStockException(sku, quantity, available);
            }

            List<Batch> batches = productBatches.get(sku);
            PickStrategy strategy = (product instanceof PerishableProduct) ? perishablePickStrategy : standardPickStrategy;
            List<Batch> selectedBatches = strategy.selectBatches(batches, quantity);

            int remainingToDeduct = quantity;
            String pickedBatchNumber = "MULTI";

            for (Batch batch : selectedBatches) {
                int deduct = Math.min(remainingToDeduct, batch.getQuantity());
                batch.adjustQuantity(-deduct);
                remainingToDeduct -= deduct;
                pickedBatchNumber = batch.getBatchNumber();
                if (remainingToDeduct <= 0) break;
            }

            // Deduct from available bins containing the product
            int binDeduct = quantity;
            for (Bin bin : binIndex.values()) {
                if (bin.getCurrentQuantity() > 0) {
                    int take = Math.min(binDeduct, bin.getCurrentQuantity());
                    bin.removeStock(take, 0.0);
                    binDeduct -= take;
                    if (binDeduct <= 0) break;
                }
            }

            Instant now = Instant.now();
            StockMovement movement = new StockMovement(
                    "MOV-" + UUID.randomUUID().toString().substring(0, 8),
                    now,
                    sku,
                    pickedBatchNumber,
                    MovementType.OUT,
                    quantity,
                    "WAREHOUSE",
                    null,
                    reason,
                    actingUsername
            );

            recordMovement(movement);

            return movement;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Stock Transfer Operation: Moves quantity from one bin to another.
     */
    public StockMovement transferStock(String sku, String batchNumber, String fromBinId, String toBinId,
                                       int quantity, String reason, String actingUsername)
            throws InsufficientStockException {
        writeLock.lock();
        try {
            Bin fromBin = binIndex.get(fromBinId);
            Bin toBin = binIndex.get(toBinId);

            if (fromBin == null || toBin == null) {
                throw new IllegalArgumentException("Invalid bin specified for transfer");
            }
            if (fromBin.getCurrentQuantity() < quantity) {
                throw new InsufficientStockException(sku, quantity, fromBin.getCurrentQuantity());
            }

            fromBin.removeStock(quantity, 0.0);
            toBin.addStock(quantity, 0.0);

            Instant now = Instant.now();
            StockMovement movement = new StockMovement(
                    "MOV-" + UUID.randomUUID().toString().substring(0, 8),
                    now,
                    sku,
                    batchNumber,
                    MovementType.TRANSFER,
                    quantity,
                    fromBinId,
                    toBinId,
                    reason,
                    actingUsername
            );

            recordMovement(movement);

            return movement;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Undo Operation: Uses Deque LIFO stack to roll back the most recent stock movement.
     */
    public StockMovement undoLastOperation(String actingUsername) throws InsufficientStockException {
        writeLock.lock();
        try {
            if (undoStack.isEmpty()) {
                return null;
            }

            StockMovement last = undoStack.pop();
            Instant now = Instant.now();
            StockMovement reversal;

            switch (last.type()) {
                case IN -> {
                    // Reversing an IN requires deducting the stock
                    int available = getCurrentStock(last.sku());
                    if (available < last.quantity()) {
                        throw new InsufficientStockException(last.sku(), last.quantity(), available);
                    }
                    List<Batch> batches = productBatches.get(last.sku());
                    if (batches != null) {
                        batches.stream()
                                .filter(b -> b.getBatchNumber().equalsIgnoreCase(last.batchNumber()))
                                .findFirst()
                                .ifPresent(b -> b.adjustQuantity(-last.quantity()));
                    }
                    if (last.toBinId() != null && binIndex.containsKey(last.toBinId())) {
                        binIndex.get(last.toBinId()).removeStock(last.quantity(), 0.0);
                    }

                    reversal = new StockMovement(
                            "UNDO-" + last.movementId(),
                            now,
                            last.sku(),
                            last.batchNumber(),
                            MovementType.OUT,
                            last.quantity(),
                            last.toBinId(),
                            null,
                            "Reversal of " + last.movementId(),
                            actingUsername
                    );
                }
                case OUT -> {
                    // Reversing an OUT requires putting stock back
                    List<Batch> batches = productBatches.computeIfAbsent(last.sku(), k -> new ArrayList<>());
                    Optional<Batch> batch = batches.stream()
                            .filter(b -> b.getBatchNumber().equalsIgnoreCase(last.batchNumber()))
                            .findFirst();
                    if (batch.isPresent()) {
                        batch.get().adjustQuantity(last.quantity());
                    } else {
                        batches.add(new Batch(last.batchNumber(), last.sku(), LocalDate.now(), null, last.quantity()));
                    }
                    // Restore to first available bin
                    binIndex.values().stream().findFirst().ifPresent(b -> b.addStock(last.quantity(), 0.0));

                    reversal = new StockMovement(
                            "UNDO-" + last.movementId(),
                            now,
                            last.sku(),
                            last.batchNumber(),
                            MovementType.IN,
                            last.quantity(),
                            null,
                            "RESTORED",
                            "Reversal of " + last.movementId(),
                            actingUsername
                    );
                }
                case TRANSFER -> {
                    // Reverse transfer bins
                    Bin fromBin = binIndex.get(last.toBinId()); // now source
                    Bin toBin = binIndex.get(last.fromBinId()); // now target
                    if (fromBin != null && toBin != null) {
                        fromBin.removeStock(last.quantity(), 0.0);
                        toBin.addStock(last.quantity(), 0.0);
                    }
                    reversal = new StockMovement(
                            "UNDO-" + last.movementId(),
                            now,
                            last.sku(),
                            last.batchNumber(),
                            MovementType.TRANSFER,
                            last.quantity(),
                            last.toBinId(),
                            last.fromBinId(),
                            "Reversal of " + last.movementId(),
                            actingUsername
                    );
                }
                default -> throw new UnsupportedOperationException("Undo not supported for: " + last.type());
            }

            recordMovement(reversal, false);
            return reversal;
        } finally {
            writeLock.unlock();
        }
    }

    public void triggerAlert(Alert alert) {
        writeLock.lock();
        try {
            Objects.requireNonNull(alert, "Alert cannot be null");
            alertQueue.offer(alert);
        } finally {
            writeLock.unlock();
        }
    }

    public List<Alert> getPendingAlerts() {
        readLock.lock();
        try {
            // Return ordered copy of alerts from PriorityQueue
            PriorityQueue<Alert> copy = new PriorityQueue<>(alertQueue);
            List<Alert> list = new ArrayList<>();
            while (!copy.isEmpty()) {
                list.add(copy.poll());
            }
            return list;
        } finally {
            readLock.unlock();
        }
    }

    public void recordProductView(String sku) {
        writeLock.lock();
        try {
            Product product = productCatalog.get(sku);
            if (product != null) {
                recentlyViewed.put(sku, product);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public List<Product> getRecentlyViewed() {
        readLock.lock();
        try {
            List<Product> list = new ArrayList<>(recentlyViewed.values());
            Collections.reverse(list); // Most recently viewed first
            return list;
        } finally {
            readLock.unlock();
        }
    }

    public List<StockMovement> getMovementsAsOf(Instant asOf) {
        readLock.lock();
        try {
            return ledgerTimeline.headMap(asOf, true).values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public List<StockMovement> getAllMovements() {
        readLock.lock();
        try {
            return new ArrayList<>(ledgerList);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Java Streams / Lambdas: Calculates aggregate valuation by category.
     */
    public Map<Category, Double> calculateValuationByCategory() {
        readLock.lock();
        try {
            return productCatalog.values().stream()
                    .collect(Collectors.groupingBy(
                            Product::getCategory,
                            Collectors.summingDouble(p -> getCurrentStock(p.getSku()) * p.getCostPrice())
                    ));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Java Streams / Lambdas: Computes top moving SKUs by total dispatched quantity.
     */
    public List<Map.Entry<String, Integer>> getTopMovers(int limit) {
        readLock.lock();
        try {
            return ledgerList.stream()
                    .filter(m -> m.type() == MovementType.OUT)
                    .collect(Collectors.groupingBy(StockMovement::sku, Collectors.summingInt(StockMovement::quantity)))
                    .entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .limit(limit)
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }
}
