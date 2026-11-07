package com.iwms.storage;

import com.iwms.exception.StorageException;
import com.iwms.model.MovementType;
import com.iwms.model.StockMovement;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Append-only write-ahead ledger for all stock movements.
 * Guarantees immutable history and provides crash-recovery state replay.
 */
public class MovementLedger {

    private static final String DELIMITER = "\t";
    private static final String NULL_TOKEN = "\\N";

    private final Path ledgerFile;
    private final ReentrantLock appendLock = new ReentrantLock();

    public MovementLedger(Path ledgerFile) throws StorageException {
        this.ledgerFile = Objects.requireNonNull(ledgerFile, "Ledger file path cannot be null");
        try {
            if (ledgerFile.getParent() != null) {
                Files.createDirectories(ledgerFile.getParent());
            }
            if (!Files.exists(ledgerFile)) {
                Files.createFile(ledgerFile);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to initialize movement ledger file: " + ledgerFile, e);
        }
    }

    /**
     * Appends an immutable stock movement to the ledger and flushes to disk.
     */
    public void append(StockMovement movement) throws StorageException {
        Objects.requireNonNull(movement, "StockMovement cannot be null");
        appendLock.lock();
        try (BufferedWriter writer = Files.newBufferedWriter(
                ledgerFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE)) {

            String line = String.join(DELIMITER,
                    movement.movementId(),
                    movement.timestamp().toString(),
                    movement.sku(),
                    safe(movement.batchNumber()),
                    movement.type().name(),
                    String.valueOf(movement.quantity()),
                    safe(movement.fromBinId()),
                    safe(movement.toBinId()),
                    safe(movement.reason()),
                    safe(movement.actingUsername())
            );

            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new StorageException("Failed to append movement to ledger: " + movement.movementId(), e);
        } finally {
            appendLock.unlock();
        }
    }

    /**
     * Reads all historical stock movements sequentially to rebuild domain state.
     */
    public List<StockMovement> replay() throws StorageException {
        appendLock.lock();
        try {
            if (!Files.exists(ledgerFile)) {
                return List.of();
            }

            List<StockMovement> movements = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(ledgerFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split(DELIMITER, -1);
                    if (parts.length >= 10) {
                        StockMovement mov = new StockMovement(
                                parts[0],
                                Instant.parse(parts[1]),
                                parts[2],
                                unescape(parts[3]),
                                MovementType.valueOf(parts[4]),
                                Integer.parseInt(parts[5]),
                                unescape(parts[6]),
                                unescape(parts[7]),
                                unescape(parts[8]),
                                unescape(parts[9])
                        );
                        movements.add(mov);
                    }
                }
            }
            return movements;
        } catch (IOException e) {
            throw new StorageException("Failed to replay movement ledger: " + ledgerFile, e);
        } finally {
            appendLock.unlock();
        }
    }

    public long countMovements() throws StorageException {
        appendLock.lock();
        try {
            if (!Files.exists(ledgerFile)) return 0;
            try (var lines = Files.lines(ledgerFile)) {
                return lines.filter(l -> !l.isBlank()).count();
            }
        } catch (IOException e) {
            throw new StorageException("Failed to count movements in ledger: " + ledgerFile, e);
        } finally {
            appendLock.unlock();
        }
    }

    private String safe(String val) {
        return val == null ? NULL_TOKEN : val.replace("\t", " ").replace("\n", " ");
    }

    private String unescape(String val) {
        return NULL_TOKEN.equals(val) ? null : val;
    }
}
