package org.example;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Performs non-destructive moves and records them for undo. */
public class FileMover {
    private final MoveHistory history;

    public FileMover(MoveHistory history) {
        this.history = history;
    }

    public Path move(Path source, Path categoryDirectory, String category) throws IOException {
        Files.createDirectories(categoryDirectory);
        Path destination = uniqueDestination(categoryDirectory, source.getFileName().toString());
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        history.record(new MoveRecord(source.toAbsolutePath().toString(), destination.toAbsolutePath().toString(), source.getFileName().toString(), category));
        return destination;
    }

    static Path uniqueDestination(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }

        String base = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }
        int counter = 1;
        do {
            candidate = directory.resolve(base + " (" + counter++ + ")" + extension);
        } while (Files.exists(candidate));
        return candidate;
    }
}
