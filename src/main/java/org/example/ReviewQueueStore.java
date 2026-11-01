package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Persists and manages items in the review queue. */
public class ReviewQueueStore {
    private static final Logger LOGGER = Logger.getLogger(ReviewQueueStore.class.getName());
    private static final String QUEUE_FILE = "review_queue.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<ReviewItem> items = new ArrayList<>();

    public ReviewQueueStore() {
        load();
    }

    public synchronized void load() {
        File file = new File(QUEUE_FILE);
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            List<ReviewItem> loaded = GSON.fromJson(reader, new TypeToken<List<ReviewItem>>(){}.getType());
            if (loaded != null) {
                items.clear();
                items.addAll(loaded);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load review queue: " + e.getMessage());
        }
    }

    public synchronized void save() {
        try {
            String json = GSON.toJson(items);
            AtomicJsonWriter.writeJson(new File(QUEUE_FILE), json);
        } catch (Exception e) {
            LOGGER.warning("Failed to save review queue: " + e.getMessage());
        }
    }

    public synchronized List<ReviewItem> getItems() {
        return new ArrayList<>(items);
    }

    public synchronized void add(ReviewItem item) {
        if (item == null) return;
        // Avoid duplicates by path
        items.removeIf(i -> i.getFilePath().equals(item.getFilePath()));
        items.add(item);
        save();
    }

    public synchronized void remove(ReviewItem item) {
        if (item == null) return;
        items.removeIf(i -> i.getFilePath().equals(item.getFilePath()));
        save();
    }

    public synchronized void clear() {
        items.clear();
        new File(QUEUE_FILE).delete();
    }
}
