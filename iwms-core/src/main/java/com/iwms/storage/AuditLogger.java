package com.iwms.storage;

import com.iwms.exception.StorageException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Append-only security and operational audit logger.
 * Captures all logins, permission checks, stock overrides, and administrative actions.
 */
public class AuditLogger {

    public record AuditEvent(
            Instant timestamp,
            String username,
            String action,
            String target,
            String outcome,
            String details
    ) {
    }

    private static final String DELIMITER = "\t";
    private static final String NULL_TOKEN = "\\N";

    private final Path auditFile;
    private final ReentrantLock logLock = new ReentrantLock();

    public AuditLogger(Path auditFile) throws StorageException {
        this.auditFile = Objects.requireNonNull(auditFile, "Audit file path cannot be null");
        try {
            if (auditFile.getParent() != null) {
                Files.createDirectories(auditFile.getParent());
            }
            if (!Files.exists(auditFile)) {
                Files.createFile(auditFile);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to initialize audit log file: " + auditFile, e);
        }
    }

    public void log(String username, String action, String target, String outcome, String details) throws StorageException {
        logLock.lock();
        try (BufferedWriter writer = Files.newBufferedWriter(
                auditFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE)) {

            String line = String.join(DELIMITER,
                    Instant.now().toString(),
                    safe(username),
                    safe(action),
                    safe(target),
                    safe(outcome),
                    safe(details)
            );

            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new StorageException("Failed to write audit event", e);
        } finally {
            logLock.unlock();
        }
    }

    public List<AuditEvent> getRecentEvents(int limit) throws StorageException {
        logLock.lock();
        try {
            if (!Files.exists(auditFile)) {
                return List.of();
            }

            List<AuditEvent> events = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(auditFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split(DELIMITER, -1);
                    if (parts.length >= 6) {
                        events.add(new AuditEvent(
                                Instant.parse(parts[0]),
                                unescape(parts[1]),
                                unescape(parts[2]),
                                unescape(parts[3]),
                                unescape(parts[4]),
                                unescape(parts[5])
                        ));
                    }
                }
            }

            if (events.size() > limit) {
                return events.subList(events.size() - limit, events.size());
            }
            return events;
        } catch (IOException e) {
            throw new StorageException("Failed to read audit log: " + auditFile, e);
        } finally {
            logLock.unlock();
        }
    }

    public List<AuditEvent> findByUser(String username) throws StorageException {
        return getRecentEvents(1000).stream()
                .filter(e -> username.equalsIgnoreCase(e.username()))
                .collect(Collectors.toList());
    }

    private String safe(String val) {
        return val == null ? NULL_TOKEN : val.replace("\t", " ").replace("\n", " ");
    }

    private String unescape(String val) {
        return NULL_TOKEN.equals(val) ? null : val;
    }
}
