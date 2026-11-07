package com.iwms.storage;

import com.iwms.exception.StorageException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Low-level utility for atomic file persistence.
 * Prevents file corruption during unexpected crashes/power failures by:
 * 1. Writing to a sibling temporary file (.tmp).
 * 2. Flushing kernel OS write buffers via FileDescriptor.sync().
 * 3. Atomically renaming the temporary file over the destination file.
 */
public final class AtomicFileWriter {

    private AtomicFileWriter() {
    }

    public static void writeBytes(Path targetFile, byte[] data) throws StorageException {
        Path tempFile = null;
        try {
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            tempFile = targetFile.resolveSibling(targetFile.getFileName().toString() + ".tmp");

            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                fos.write(data);
                fos.flush();
                fos.getFD().sync(); // Ensure physical disk synchronization
            }

            try {
                Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
            throw new StorageException("Failed to atomically write to file: " + targetFile, e);
        }
    }

    public static void writeString(Path targetFile, String content) throws StorageException {
        writeBytes(targetFile, content.getBytes(StandardCharsets.UTF_8));
    }

    public static String readString(Path targetFile) throws StorageException {
        try {
            return Files.readString(targetFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Failed to read string from file: " + targetFile, e);
        }
    }

    public static byte[] readBytes(Path targetFile) throws StorageException {
        try {
            return Files.readAllBytes(targetFile);
        } catch (IOException e) {
            throw new StorageException("Failed to read bytes from file: " + targetFile, e);
        }
    }
}
