package com.iwms.api.controller;

import com.iwms.api.filter.CustomAuthFilter;
import com.iwms.model.Permission;
import com.iwms.model.User;
import com.iwms.storage.AuditLogger;
import com.iwms.sync.SyncEngine;
import com.iwms.sync.SyncOperation;
import com.iwms.sync.SyncResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncEngine syncEngine;
    private final AuditLogger auditLogger;

    public SyncController(SyncEngine syncEngine, AuditLogger auditLogger) {
        this.syncEngine = syncEngine;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/batch")
    public ResponseEntity<List<SyncResult>> processBatch(@RequestBody List<SyncOperation> operations,
                                                         HttpServletRequest request) throws Exception {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        String username = user != null ? user.getUsername() : "offline_client";

        List<SyncResult> results = syncEngine.processBatch(operations);
        auditLogger.log(username, "SYNC_BATCH", "OUTBOX", "PROCESSED", "Processed " + operations.size() + " offline operations");
        return ResponseEntity.ok(results);
    }

    @GetMapping("/conflicts")
    public ResponseEntity<?> getPendingConflicts(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        if (user == null || !user.hasPermission(Permission.STOCK_ADJUST)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Permission denied: Manager review required"));
        }
        return ResponseEntity.ok(syncEngine.getPendingConflicts());
    }

    @PostMapping("/conflicts/{conflictId}/resolve")
    public ResponseEntity<?> resolveConflict(@PathVariable String conflictId,
                                             @RequestBody Map<String, Object> body,
                                             HttpServletRequest request) throws Exception {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        if (user == null || !user.hasPermission(Permission.STOCK_ADJUST)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Permission denied: Manager resolution required"));
        }

        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String note = (String) body.getOrDefault("note", "Resolution by manager");

        syncEngine.resolveConflict(conflictId, approve, note, user.getUsername());
        auditLogger.log(user.getUsername(), "RESOLVE_CONFLICT", conflictId, approve ? "APPROVED" : "REJECTED", note);

        return ResponseEntity.ok(Map.of(
                "conflictId", conflictId,
                "status", approve ? "APPROVED" : "REJECTED",
                "resolvedBy", user.getUsername()
        ));
    }
}
