package org.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** Scans and sorts existing files in the downloads folder. */
public class BackfillSorter {
    private static final Logger LOGGER = Logger.getLogger(BackfillSorter.class.getName());
    private final Config config;
    private final Extractor extractor;
    private final Orchestrator orchestrator;
    private final FileMover fileMover;

    public record Preview(File file, String category) { }

    public BackfillSorter(Config config, Extractor extractor, Orchestrator orchestrator, FileMover fileMover) {
        this.config = config;
        this.extractor = extractor;
        this.orchestrator = orchestrator;
        this.fileMover = fileMover;
    }

    public List<Preview> preview(Path folder, LocalDate from, LocalDate to, boolean useCreatedDate) {
        Path absolute = folder.toAbsolutePath().normalize();
        LOGGER.info(() -> "Backfill scan folder=" + absolute + ", exists=" + Files.exists(absolute)
                + ", from=" + from + ", to=" + to + ", mode=" + (useCreatedDate ? "created" : "modified"));
        List<Preview> result = new ArrayList<>();
        if (!Files.isDirectory(absolute)) {
            throw new IllegalArgumentException("Watched folder does not exist or is not a directory: " + absolute);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from date must not be after to date");
        }
        Instant start = from == null ? null : from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = to == null ? null : to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long entries = 0;
        try (Stream<Path> stream = Files.list(absolute)) {
            List<Path> paths = stream.toList();
            entries = paths.size();
            LOGGER.info("Files.list returned " + entries + " entries");
            for (Path path : paths) {
                try {
                    if (!Files.isRegularFile(path)) { LOGGER.info("Skip " + path + ": directory/non-regular"); continue; }
                    if (FileStability.isIgnored(path)) { LOGGER.info("Skip " + path + ": ignored/hidden/temp"); continue; }
                    if (config.getCategories().stream().anyMatch(c -> path.getParent().getFileName().toString().equalsIgnoreCase(c))) {
                        LOGGER.info("Skip " + path + ": already in category folder"); continue;
                    }
                    Instant date = useCreatedDate ? Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class).creationTime().toInstant()
                            : Files.getLastModifiedTime(path).toInstant();
                    if ((start != null && date.isBefore(start)) || (end != null && !date.isBefore(end))) {
                        LOGGER.info("Skip " + path + ": outside date range"); continue;
                    }
                    Classification classification;
                    try {
                        String rule = RuleEngine.evaluate(path.toFile());
                        classification = rule != null ? new Classification(rule, 1.0, true)
                                : orchestrator.classify(extractor.extractContent(path.toFile()), path.getFileName().toString());
                        LOGGER.info("Classification " + path + ": category=" + classification.category());
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Classification exception for " + path, e);
                        classification = new Classification("Misc", 0.0, false);
                    }
                    String category = CategorySanitizer.sanitize(classification.category(), config.getCategories());
                    if (classification.confidence() < config.getConfidenceThreshold() && !classification.ruleBased()) {
                        category = "Needs_Review";
                    }
                    result.add(new Preview(path.toFile(), category));
                    LOGGER.info("Include " + path + " -> " + category);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Exception evaluating scan entry " + path, e);
                    throw new IllegalStateException("Failed to scan " + path + ": " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Backfill scan failed for " + absolute, e);
            throw e instanceof RuntimeException r ? r : new IllegalStateException(e);
        }
        LOGGER.info("Preview rows added=" + result.size());
        return result;
    }

    public void movePreview(List<Preview> previews, BiConsumer<File, String> onProcessed) {
        LOGGER.info("Move button enabled=" + (previews != null && !previews.isEmpty()) + ", rows=" + (previews == null ? 0 : previews.size()));
        if (previews == null) return;
        for (Preview preview : previews) {
            try {
                Path source = preview.file().toPath();
                Path destination = fileMover.move(source, source.getParent().resolve(preview.category()), preview.category());
                LOGGER.info("Moved " + source + " -> " + destination);
                if (onProcessed != null) onProcessed.accept(preview.file(), preview.category());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Move failed for " + preview.file(), e);
                throw new IllegalStateException("Move failed for " + preview.file().getName() + ": " + e.getMessage(), e);
            }
        }
    }

    public void sortExistingFiles(Path folder, LocalDate from, LocalDate to, BiConsumer<File, String> onProcessed) {
        movePreview(preview(folder, from, to, false), onProcessed);
    }

    public void sortExistingFiles(Path folder, BiConsumer<File, String> onProcessed) {
        sortExistingFiles(folder, null, null, onProcessed);
    }
}
