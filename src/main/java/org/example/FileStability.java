package org.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;

/** Checks file stability (size and last modified time unchanged over stability interval). */
public final class FileStability {
    private FileStability() {}

    public static boolean isIgnored(Path path) {
        if (path == null) return true;
        try {
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return true;
            if (Files.isSymbolicLink(path) || Files.isHidden(path)) return true;
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".crdownload") || name.endsWith(".tmp") || name.endsWith(".part")
                    || name.endsWith(".download") || name.endsWith(".partial")) {
                return true;
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    public static boolean waitUntilStable(Path path, long stabilityMillis, long pollIntervalMillis) {
        if (isIgnored(path)) return false;
        long maxAttempts = Math.max(1, stabilityMillis / Math.max(100, pollIntervalMillis));
        long prevSize = -1;
        long prevModified = -1;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                if (!Files.exists(path) || !Files.isRegularFile(path)) return false;
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                long size = attrs.size();
                long modified = attrs.lastModifiedTime().toMillis();

                if (size == prevSize && modified == prevModified) {
                    Thread.sleep(pollIntervalMillis);
                    // Double check after sleep
                    BasicFileAttributes attrs2 = Files.readAttributes(path, BasicFileAttributes.class);
                    if (attrs2.size() == size && attrs2.lastModifiedTime().toMillis() == modified) {
                        return true;
                    }
                }
                prevSize = size;
                prevModified = modified;
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
