package com.iwms.api.controller;

import com.iwms.api.filter.CustomAuthFilter;
import com.iwms.model.Permission;
import com.iwms.model.User;
import com.iwms.storage.AuditLogger;
import com.iwms.storage.BackupManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final BackupManager backupManager;
    private final AuditLogger auditLogger;

    public BackupController(BackupManager backupManager, AuditLogger auditLogger) {
        this.backupManager = backupManager;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerBackup(@RequestBody(required = false) Map<String, String> body,
                                           HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        if (user == null || !user.hasPermission(Permission.MANAGE_SYSTEM)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Permission denied: MANAGE_SYSTEM required"));
        }

        try {
            String label = body != null ? body.getOrDefault("label", "manual") : "manual";
            Path backupZip = backupManager.createBackup(label);
            auditLogger.log(user.getUsername(), "BACKUP_CREATE", backupZip.getFileName().toString(), "SUCCESS", "Created manual backup");

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "filename", backupZip.getFileName().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listBackups(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        if (user == null || !user.hasPermission(Permission.MANAGE_SYSTEM)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Permission denied: MANAGE_SYSTEM required"));
        }

        try {
            List<String> list = backupManager.listBackups().stream()
                    .map(p -> p.getFileName().toString())
                    .toList();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
