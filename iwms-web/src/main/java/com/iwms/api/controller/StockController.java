package com.iwms.api.controller;

import com.iwms.api.dto.StockInRequest;
import com.iwms.api.dto.StockOutRequest;
import com.iwms.api.dto.StockTransferRequest;
import com.iwms.api.filter.CustomAuthFilter;
import com.iwms.exception.InsufficientStockException;
import com.iwms.model.*;
import com.iwms.service.InventoryService;
import com.iwms.storage.AuditLogger;
import com.iwms.storage.MovementLedger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final InventoryService inventoryService;
    private final MovementLedger movementLedger;
    private final AuditLogger auditLogger;

    public StockController(InventoryService inventoryService,
                           MovementLedger movementLedger,
                           AuditLogger auditLogger) {
        this.inventoryService = inventoryService;
        this.movementLedger = movementLedger;
        this.auditLogger = auditLogger;
    }

    @GetMapping
    public ResponseEntity<Map<String, Integer>> getStockSummary() {
        Map<String, Integer> map = inventoryService.getAllProducts().stream()
                .collect(Collectors.toMap(Product::getSku, p -> inventoryService.getCurrentStock(p.getSku())));
        return ResponseEntity.ok(map);
    }

    @PostMapping("/in")
    public ResponseEntity<?> stockIn(@RequestBody StockInRequest req, HttpServletRequest request) throws Exception {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        String username = user != null ? user.getUsername() : "system";

        try {
            LocalDate expiry = (req.expiryDate() != null && !req.expiryDate().isBlank()) ? LocalDate.parse(req.expiryDate()) : null;
            StockMovement mov = inventoryService.stockIn(
                    req.sku(),
                    req.batchNumber(),
                    req.binId(),
                    req.quantity(),
                    expiry,
                    req.reason() != null ? req.reason() : "Inbound Receiving",
                    username
            );

            movementLedger.append(mov);
            auditLogger.log(username, "STOCK_IN", req.sku(), "SUCCESS", "Received " + req.quantity() + " units into " + req.binId());
            return ResponseEntity.ok(mov);
        } catch (Exception e) {
            auditLogger.log(username, "STOCK_IN", req.sku(), "FAILURE", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/out")
    public ResponseEntity<?> stockOut(@RequestBody StockOutRequest req, HttpServletRequest request) throws Exception {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        String username = user != null ? user.getUsername() : "system";

        try {
            StockMovement mov = inventoryService.stockOut(
                    req.sku(),
                    req.quantity(),
                    req.reason() != null ? req.reason() : "Outbound Dispatch",
                    username
            );

            movementLedger.append(mov);
            auditLogger.log(username, "STOCK_OUT", req.sku(), "SUCCESS", "Dispatched " + req.quantity() + " units");
            return ResponseEntity.ok(mov);
        } catch (InsufficientStockException e) {
            auditLogger.log(username, "STOCK_OUT", req.sku(), "DEFICIT", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            auditLogger.log(username, "STOCK_OUT", req.sku(), "FAILURE", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferStock(@RequestBody StockTransferRequest req, HttpServletRequest request) throws Exception {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        String username = user != null ? user.getUsername() : "system";

        try {
            StockMovement mov = inventoryService.transferStock(
                    req.sku(),
                    req.batchNumber(),
                    req.fromBinId(),
                    req.toBinId(),
                    req.quantity(),
                    req.reason() != null ? req.reason() : "Internal Bin Relocation",
                    username
            );

            movementLedger.append(mov);
            auditLogger.log(username, "STOCK_TRANSFER", req.sku(), "SUCCESS", "Transferred " + req.quantity() + " from " + req.fromBinId() + " to " + req.toBinId());
            return ResponseEntity.ok(mov);
        } catch (Exception e) {
            auditLogger.log(username, "STOCK_TRANSFER", req.sku(), "FAILURE", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/undo")
    public ResponseEntity<?> undoLastOperation(HttpServletRequest request) throws Exception {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        String username = user != null ? user.getUsername() : "system";

        try {
            StockMovement reversal = inventoryService.undoLastOperation(username);
            if (reversal == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Undo stack is empty"));
            }

            movementLedger.append(reversal);
            auditLogger.log(username, "UNDO", reversal.sku(), "SUCCESS", "Reversed operation " + reversal.movementId());
            return ResponseEntity.ok(reversal);
        } catch (Exception e) {
            auditLogger.log(username, "UNDO", "LEDGER", "FAILURE", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/asof")
    public ResponseEntity<?> getStockAsOf(@RequestParam String timestamp) {
        try {
            Instant asOf = Instant.parse(timestamp);
            List<StockMovement> pastMovements = inventoryService.getMovementsAsOf(asOf);

            // Reconstruct balances as of that timestamp
            Map<String, Integer> historicalBalances = new HashMap<>();
            for (StockMovement m : pastMovements) {
                int delta = (m.type() == MovementType.IN) ? m.quantity() : (m.type() == MovementType.OUT ? -m.quantity() : 0);
                historicalBalances.merge(m.sku(), delta, Integer::sum);
            }

            return ResponseEntity.ok(Map.of(
                    "asOf", asOf.toString(),
                    "totalMovementsCount", pastMovements.size(),
                    "stockBalances", historicalBalances
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ISO-8601 timestamp: " + timestamp));
        }
    }

    @GetMapping("/movements")
    public ResponseEntity<List<StockMovement>> getAllMovements() {
        return ResponseEntity.ok(inventoryService.getAllMovements());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> getPendingAlerts() {
        return ResponseEntity.ok(inventoryService.getPendingAlerts());
    }

    @GetMapping("/analytics/top-movers")
    public ResponseEntity<?> getTopMovers(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(inventoryService.getTopMovers(limit));
    }

    @GetMapping("/analytics/valuations")
    public ResponseEntity<?> getValuations(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        if (user == null || !user.hasPermission(Permission.VIEW_FINANCIALS)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Permission denied: VIEW_FINANCIALS required"));
        }
        return ResponseEntity.ok(inventoryService.calculateValuationByCategory());
    }
}
