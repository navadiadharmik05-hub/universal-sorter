package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Utility for atomic file writes by writing to a temporary file in the same directory and renaming. */
public final class AtomicJsonWriter {
    private AtomicJsonWriter() {}

    public static void writeJson(File targetFile, String jsonContent) throws IOException {
        Path target = targetFile.toPath().toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Path tempFile = Files.createTempFile(parent != null ? parent : Path.of("."), "atomic-", ".json");
        try {
            Files.writeString(tempFile, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
            // Fallback if atomic move fails across filesystems
            Files.writeString(target, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
