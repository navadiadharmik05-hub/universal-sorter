package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase2And3Test {

    @Test
    void testUserRuleEngine(@TempDir Path tempDir) throws Exception {
        File f = tempDir.resolve("my_custom_report.xyz").toFile();
        Files.writeString(f.toPath(), "test content");

        List<UserRule> rules = List.of(
                new UserRule(UserRule.Type.EXTENSION, ".xyz", "Docs"),
                new UserRule(UserRule.Type.FILENAME_CONTAINS, "custom", "Code_Projects")
        );
        List<String> allowed = List.of("Docs", "Code_Projects", "Misc", "Needs_Review");

        String matched = UserRuleEngine.evaluate(f, rules, allowed);
        assertEquals("Docs", matched);
    }

    @Test
    void testClassificationCache(@TempDir Path tempDir) throws Exception {
        ClassificationCache cache = new ClassificationCache();
        File f = tempDir.resolve("cache_test.txt").toFile();
        Files.writeString(f.toPath(), "hello cache");

        Classification c = new Classification("Academics", 0.95, false);
        cache.put(f, "hello cache", c);

        Classification retrieved = cache.get(f, "hello cache");
        assertNotNull(retrieved);
        assertEquals("Academics", retrieved.category());
        assertEquals(0.95, retrieved.confidence());
    }

    @Test
    void testReviewQueueStore(@TempDir Path tempDir) {
        ReviewQueueStore store = new ReviewQueueStore();
        File f = tempDir.resolve("review.txt").toFile();
        ReviewItem item = new ReviewItem(f, "Misc", 0.4, false, "summary");

        store.add(item);
        assertEquals(1, store.getItems().size());
        assertEquals("review.txt", store.getItems().get(0).getFileName());

        store.remove(item);
        assertTrue(store.getItems().isEmpty());
    }

    @Test
    void testReviewQueueStorePersistence(@TempDir Path tempDir) throws Exception {
        File f = tempDir.resolve("test_review.pdf").toFile();
        Files.writeString(f.toPath(), "invoice content");
        ReviewQueueStore store = new ReviewQueueStore();
        ReviewItem item = new ReviewItem(f, "Finance_Invoices", 0.45, false, "invoice summary");
        store.add(item);

        assertEquals(1, store.getItems().size());
        assertEquals("Finance_Invoices", store.getItems().get(0).getProposedCategory());

        store.remove(item);
        assertTrue(store.getItems().isEmpty());
    }

    @Test
    void testMoveHistoryAndUndo(@TempDir Path tempDir) throws Exception {
        File sourceDir = tempDir.resolve("source").toFile();
        File targetDir = tempDir.resolve("target").toFile();
        sourceDir.mkdir();
        targetDir.mkdir();

        File sourceFile = new File(sourceDir, "test.txt");
        Files.writeString(sourceFile.toPath(), "hello undo");

        File historyFile = tempDir.resolve("history.json").toFile();
        MoveHistory history = new MoveHistory(historyFile);
        FileMover mover = new FileMover(history);

        Path moved = mover.move(sourceFile.toPath(), targetDir.toPath(), "Docs");
        assertFalse(sourceFile.exists());
        assertTrue(moved.toFile().exists());
        assertEquals(1, history.getRecords().size());

        MoveRecord undone = history.undoLastMove();
        assertNotNull(undone);
        assertTrue(sourceFile.exists());
        assertFalse(moved.toFile().exists());
        assertTrue(history.getRecords().isEmpty());
    }
}
