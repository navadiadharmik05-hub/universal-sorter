package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FileMoverTest {

    @Test
    void testUniqueDestination(@TempDir Path tempDir) throws Exception {
        Path file1 = tempDir.resolve("report.pdf");
        Files.createFile(file1);

        Path unique = FileMover.uniqueDestination(tempDir, "report.pdf");
        assertEquals("report (1).pdf", unique.getFileName().toString());
    }
}
