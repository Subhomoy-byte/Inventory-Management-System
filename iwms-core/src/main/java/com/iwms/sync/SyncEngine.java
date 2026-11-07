package com.iwms.sync;

import com.iwms.exception.InsufficientStockException;
import com.iwms.service.InventoryService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Offline Synchronization Engine for Progressive Web Apps (PWA) operating over LAN.
 * Guarantees idempotent deduplication of write-ahead queues via syncId caching
 * and captures stock deficits as reviewable manager conflicts rather than crashing.
 */
public class SyncEngine {

    public record SyncConflict(
            String conflictId,
            SyncOperation operation,
            String reason,
            Instant detectedAt
    ) {
    }

    private final InventoryService inventoryService;
    private final Set<String> processedSyncIds = ConcurrentHashMap.newKeySet();
    private final List<SyncConflict> pendingConflicts = new CopyOnWriteArrayList<>();

    public SyncEngine(InventoryService inventoryService) {
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService cannot be null");
    }

    /**
     * Processes a single offline sync operation idempotently.
     */
    public SyncResult processOperation(SyncOperation op) {
        Objects.requireNonNull(op, "SyncOperation cannot be null");

        // 1. Idempotency Check: deduplicate retransmitted mutations
        if (processedSyncIds.contains(op.syncId())) {
            return new SyncResult(op.syncId(), SyncStatus.DUPLICATE, "Operation already applied", Instant.now());
        }

        String opType = op.operationType() != null ? op.operationType().trim().toUpperCase() : "UNKNOWN";

        try {
            switch (opType) {
                case "IN" -> {
                    String targetBin = (op.toBinId() != null && !op.toBinId().isBlank()) ? op.toBinId() : "BIN-1";
                    String batch = (op.batchNumber() != null && !op.batchNumber().isBlank()) ? op.batchNumber() : "SYNC-BATCH";
                    inventoryService.stockIn(
                            op.sku(),
                            batch,
                            targetBin,
                            op.quantity(),
                            null,
                            op.reason(),
                            op.username()
                    );
                    processedSyncIds.add(op.syncId());
                    return new SyncResult(op.syncId(), SyncStatus.APPLIED, "Stock inbound processed", Instant.now());
                }
                case "OUT" -> {
                    int available = inventoryService.getCurrentStock(op.sku());
                    if (available < op.quantity()) {
                        return recordConflict(op, String.format("Insufficient stock deficit: available %d < requested %d", available, op.quantity()));
                    }

                    inventoryService.stockOut(op.sku(), op.quantity(), op.reason(), op.username());
                    processedSyncIds.add(op.syncId());
                    return new SyncResult(op.syncId(), SyncStatus.APPLIED, "Stock outbound processed", Instant.now());
                }
                case "TRANSFER" -> {
                    int available = inventoryService.getCurrentStock(op.sku());
                    if (available < op.quantity()) {
                        return recordConflict(op, String.format("Insufficient stock deficit for transfer: available %d < requested %d", available, op.quantity()));
                    }

                    String batch = (op.batchNumber() != null && !op.batchNumber().isBlank()) ? op.batchNumber() : "DEFAULT";
                    inventoryService.transferStock(
                            op.sku(),
                            batch,
                            op.fromBinId(),
                            op.toBinId(),
                            op.quantity(),
                            op.reason(),
                            op.username()
                    );
                    processedSyncIds.add(op.syncId());
                    return new SyncResult(op.syncId(), SyncStatus.APPLIED, "Stock transfer processed", Instant.now());
                }
                default -> {
                    return recordConflict(op, "Unsupported sync operation type: " + opType);
                }
            }
        } catch (InsufficientStockException e) {
            return recordConflict(op, "Insufficient stock deficit: " + e.getMessage());
        } catch (Exception e) {
            return recordConflict(op, "Operational failure during sync: " + e.getMessage());
        }
    }

    /**
     * Processes an ordered batch of offline operations sequentially.
     */
    public List<SyncResult> processBatch(List<SyncOperation> operations) {
        if (operations == null || operations.isEmpty()) {
            return List.of();
        }
        return operations.stream().map(this::processOperation).collect(Collectors.toList());
    }

    private SyncResult recordConflict(SyncOperation op, String reason) {
        String conflictId = "CONF-" + UUID.randomUUID().toString().substring(0, 8);
        SyncConflict conflict = new SyncConflict(conflictId, op, reason, Instant.now());
        pendingConflicts.add(conflict);
        return new SyncResult(op.syncId(), SyncStatus.CONFLICT, reason, Instant.now());
    }

    public List<SyncConflict> getPendingConflicts() {
        return new ArrayList<>(pendingConflicts);
    }

    public synchronized void resolveConflict(String conflictId, boolean approve, String resolutionNote, String managerUsername) {
        Optional<SyncConflict> found = pendingConflicts.stream()
                .filter(c -> c.conflictId().equalsIgnoreCase(conflictId))
                .findFirst();

        if (found.isPresent()) {
            SyncConflict conflict = found.get();
            if (approve) {
                // If manager approves, attempt forced application
                try {
                    SyncOperation op = conflict.operation();
                    if ("IN".equalsIgnoreCase(op.operationType())) {
                        inventoryService.stockIn(op.sku(), op.batchNumber(), op.toBinId(), op.quantity(), null, "Manager Approved: " + resolutionNote, managerUsername);
                    } else {
                        inventoryService.stockOut(op.sku(), op.quantity(), "Manager Approved: " + resolutionNote, managerUsername);
                    }
                    processedSyncIds.add(op.syncId());
                } catch (Exception ignored) {
                }
            }
            pendingConflicts.remove(conflict);
        }
    }

    public int getProcessedSyncCount() {
        return processedSyncIds.size();
    }
}
