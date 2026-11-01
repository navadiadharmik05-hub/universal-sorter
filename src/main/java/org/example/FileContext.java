package org.example;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/** Immutable, privacy-conscious description supplied to content classifiers. */
public record FileContext(
        String name,
        String extension,
        String detectedMime,
        long size,
        Instant created,
        Instant modified,
        String sourceFolder,
        String extractedText,
        Map<String, String> metadata,
        String extractionMethod,
        java.util.Set<String> flags) {
    public FileContext {
        name = name == null ? "" : name;
        extension = extension == null ? "" : extension;
        detectedMime = detectedMime == null ? "application/octet-stream" : detectedMime;
        extractedText = extractedText == null ? "" : extractedText;
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
        flags = flags == null ? java.util.Set.of() : Collections.unmodifiableSet(flags);
    }
}
