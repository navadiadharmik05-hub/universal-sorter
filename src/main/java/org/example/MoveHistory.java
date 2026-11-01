package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoveHistory {
    private static final int MAX_HISTORY_RECORDS = 1000;
    private final File historyFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<MoveRecord> records;

    public MoveHistory() {
        this(new File("move_history.json"));
    }

    public MoveHistory(File historyFile) {
        this.historyFile = historyFile;
        this.records = load();
    }

    private List<MoveRecord> load() {
        if (!historyFile.exists()) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(historyFile)) {
            Type listType = new TypeToken<List<MoveRecord>>() { }.getType();
            List<MoveRecord> loaded = gson.fromJson(reader, listType);
            return loaded == null ? new ArrayList<>() : new ArrayList<>(loaded);
        } catch (Exception e) {
            System.err.println("Failed to load move history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized void record(MoveRecord record) {
        records.add(record);
        if (records.size() > MAX_HISTORY_RECORDS) {
            records.subList(0, records.size() - MAX_HISTORY_RECORDS).clear();
        }
        save();
    }

    public synchronized List<MoveRecord> getRecords() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    public synchronized MoveRecord undoLastMove() throws IOException {
        if (records.isEmpty()) {
            return null;
        }
        return undoRecord(records.size() - 1);
    }

    public synchronized MoveRecord undoRecord(int index) throws IOException {
        if (index < 0 || index >= records.size()) {
            return null;
        }
        MoveRecord record = records.get(index);
        Path target = Path.of(record.getTargetPath());
        Path source = Path.of(record.getSourcePath());

        if (!Files.exists(target)) {
            // If target missing, remove record and return null
            records.remove(index);
            save();
            throw new IOException("Moved file no longer exists: " + target);
        }
        if (Files.exists(source)) {
            // If original path is occupied, create a unique destination path for restoring
            Path parent = source.getParent();
            Files.createDirectories(parent);
            String fileName = source.getFileName().toString();
            source = FileMover.uniqueDestination(parent, fileName);
        }

        Files.createDirectories(source.getParent());
        try {
            Files.move(target, source, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(target, source, StandardCopyOption.REPLACE_EXISTING);
        }
        records.remove(index);
        save();
        return record;
    }

    private void save() {
        try {
            String json = gson.toJson(records);
            AtomicJsonWriter.writeJson(historyFile, json);
        } catch (IOException e) {
            System.err.println("Failed to save move history: " + e.getMessage());
        }
    }
}
