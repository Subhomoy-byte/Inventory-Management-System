package com.iwms.storage;

import com.iwms.exception.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Automates full system data backups as timestamped ZIP archives.
 * Supports manual on-demand triggers and JVM shutdown hooks.
 */
public class BackupManager {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path dataDir;
    private final Path backupDir;

    public BackupManager(Path dataDir, Path backupDir) throws StorageException {
        this.dataDir = Objects.requireNonNull(dataDir, "Data dir cannot be null");
        this.backupDir = Objects.requireNonNull(backupDir, "Backup dir cannot be null");
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            throw new StorageException("Failed to initialize backup directories", e);
        }
    }

    /**
     * Creates a compressed ZIP backup of the data directory.
     */
    public Path createBackup(String label) throws StorageException {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String safeLabel = (label == null || label.isBlank()) ? "auto" : label.replaceAll("[^a-zA-Z0-9_-]", "_");
        String filename = String.format("backup_%s_%s.zip", timestamp, safeLabel);
        Path targetZip = backupDir.resolve(filename);

        try (OutputStream fos = Files.newOutputStream(targetZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            if (Files.exists(dataDir)) {
                try (var stream = Files.walk(dataDir)) {
                    List<Path> paths = stream.filter(p -> !Files.isDirectory(p)).toList();
                    for (Path path : paths) {
                        String relativeName = dataDir.relativize(path).toString().replace('\\', '/');
                        zos.putNextEntry(new ZipEntry(relativeName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                }
            }

            return targetZip;
        } catch (IOException e) {
            throw new StorageException("Failed to create backup: " + targetZip, e);
        }
    }

    /**
     * Lists existing backup archives ordered by creation date (newest first).
     */
    public List<Path> listBackups() throws StorageException {
        try {
            if (!Files.exists(backupDir)) return List.of();
            try (var stream = Files.list(backupDir)) {
                return stream.filter(p -> p.getFileName().toString().endsWith(".zip"))
                        .sorted(Comparator.comparing(Path::getFileName).reversed())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new StorageException("Failed to list backups in: " + backupDir, e);
        }
    }

    /**
     * Restores all files from a backup ZIP archive into the target data directory.
     */
    public void restoreBackup(Path backupZip, Path targetDataDir) throws StorageException {
        Objects.requireNonNull(backupZip, "Backup zip path cannot be null");
        Objects.requireNonNull(targetDataDir, "Target directory cannot be null");

        if (!Files.exists(backupZip)) {
            throw new StorageException("Backup file does not exist: " + backupZip);
        }

        try {
            Files.createDirectories(targetDataDir);
            try (InputStream fis = Files.newInputStream(backupZip);
                 ZipInputStream zis = new ZipInputStream(fis)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        zis.closeEntry();
                        continue;
                    }
                    Path resolvedPath = targetDataDir.resolve(entry.getName()).normalize();
                    // Prevent zip-slip vulnerability
                    if (!resolvedPath.startsWith(targetDataDir.normalize())) {
                        throw new StorageException("Malicious zip entry detected: " + entry.getName());
                    }
                    if (resolvedPath.getParent() != null) {
                        Files.createDirectories(resolvedPath.getParent());
                    }
                    Files.copy(zis, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new StorageException("Failed to restore backup from: " + backupZip, e);
        }
    }

    /**
     * Registers a JVM shutdown hook to automatically capture a snapshot on exit.
     */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                createBackup("shutdown");
            } catch (Exception e) {
                System.err.println("Failed to create shutdown backup: " + e.getMessage());
            }
        }, "iwms-shutdown-backup"));
    }
}
