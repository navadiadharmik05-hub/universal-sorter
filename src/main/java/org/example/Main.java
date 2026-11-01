package org.example;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        AppTheme.install();
        if (args.length > 0 && !args[0].isBlank()) {
            Path testPath = Path.of(args[0]).toAbsolutePath().normalize();
            if (!Files.isDirectory(testPath)) {
                JOptionPane.showMessageDialog(
                        null,
                        "Error: Provided watched folder path is not an existing directory:\n" + testPath,
                        "Invalid Watched Folder",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }

        SwingUtilities.invokeLater(() -> {
            Config config = Config.load();
            if (args.length > 0 && !args[0].isBlank()) {
                String overridePath = Path.of(args[0]).toAbsolutePath().normalize().toString();
                config.setWatchedFolderOverride(overridePath);
            }
            MoveHistory moveHistory = new MoveHistory();
            FileMover fileMover = new FileMover(moveHistory);
            Extractor extractor = new Extractor();
            Orchestrator orchestrator = new Orchestrator(config);
            MainWindow mainWindow = new MainWindow(config, moveHistory, extractor, orchestrator, fileMover);
            mainWindow.setVisible(true);

            CompletableFuture.supplyAsync(orchestrator::isAvailable)
                    .thenAccept(available -> {
                        if (!available) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                    mainWindow,
                                    "Ollama is unavailable at " + config.getOllamaUrl()
                                            + ". Rule-based sorting remains available; AI classification will retry when used.",
                                    "Ollama unavailable",
                                    JOptionPane.WARNING_MESSAGE));
                            mainWindow.setStatus("Status: Offline (Rule-based)");
                        } else {
                            mainWindow.setStatus("Loading model...");
                            CompletableFuture.runAsync(() -> {
                                try {
                                    orchestrator.warmUp();
                                    mainWindow.setStatus("Status: Ollama Connected");
                                } catch (Exception e) {
                                    LOGGER.log(Level.SEVERE, "Ollama warm-up failed", e);
                                    mainWindow.setStatus("Ollama Error: " + e.getMessage());
                                }
                            });
                        }
                    });
        });
    }
}
