package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SafetyAndCorrectnessTest {

    @Test
    void testCategorySanitizer() {
        List<String> allowed = Arrays.asList("Academics", "Finance_Invoices", "Misc", "Needs_Review");

        assertEquals("Finance_Invoices", CategorySanitizer.sanitize("finance_invoices", allowed));
        assertEquals("Academics", CategorySanitizer.sanitize("  Academics  ", allowed));
        assertEquals("Needs_Review", CategorySanitizer.sanitize("UnknownCat", allowed));
        assertEquals("Needs_Review", CategorySanitizer.sanitize(null, allowed));
        assertEquals("Needs_Review", CategorySanitizer.sanitize("../etc/passwd", allowed));
        assertEquals("Needs_Review", CategorySanitizer.sanitize("C:\\Windows", allowed));
    }

    @Test
    void testAtomicJsonWriter(@TempDir Path tempDir) throws Exception {
        File target = tempDir.resolve("test.json").toFile();
        String content = "{\"test\": true}";
        AtomicJsonWriter.writeJson(target, content);

        assertTrue(target.exists());
        String read = Files.readString(target.toPath());
        assertEquals(content, read);
    }

    @Test
    void testFileStabilityIgnored() {
        assertTrue(FileStability.isIgnored(null));
        assertTrue(FileStability.isIgnored(Path.of("test.crdownload")));
        assertTrue(FileStability.isIgnored(Path.of("test.tmp")));
        assertTrue(FileStability.isIgnored(Path.of("test.part")));
        assertTrue(FileStability.isIgnored(Path.of("test.download")));
        assertTrue(FileStability.isIgnored(Path.of("test.partial")));
    }
}
