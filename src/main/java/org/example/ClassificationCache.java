package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/** Caches file classifications to avoid redundant LLM calls. */
public class ClassificationCache {
    private static final Logger LOGGER = Logger.getLogger(ClassificationCache.class.getName());
    private static final String CACHE_FILE = "classification_cache.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_CACHE_SIZE = 1000;

    public record CachedResult(String category, double confidence, boolean ruleBased, long timestamp) {}

    private final Map<String, CachedResult> cache = new HashMap<>();

    public ClassificationCache() {
        load();
    }

    private synchronized void load() {
        File file = new File(CACHE_FILE);
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Map<String, CachedResult> loaded = GSON.fromJson(reader, new TypeToken<Map<String, CachedResult>>(){}.getType());
            if (loaded != null) {
                cache.putAll(loaded);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load classification cache: " + e.getMessage());
        }
    }

    public synchronized void save() {
        try {
            String json = GSON.toJson(cache);
            AtomicJsonWriter.writeJson(new File(CACHE_FILE), json);
        } catch (Exception e) {
            LOGGER.warning("Failed to save classification cache: " + e.getMessage());
        }
    }

    public synchronized Classification get(File file, String content) {
        String key = computeKey(file, content);
        CachedResult res = cache.get(key);
        if (res != null) {
            return new Classification(res.category(), res.confidence(), res.ruleBased());
        }
        return null;
    }

    public synchronized void put(File file, String content, Classification classification) {
        if (classification == null) return;
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.clear();
        }
        String key = computeKey(file, content);
        cache.put(key, new CachedResult(classification.category(), classification.confidence(), classification.ruleBased(), System.currentTimeMillis()));
        save();
    }

    public synchronized void clear() {
        cache.clear();
        new File(CACHE_FILE).delete();
    }

    private String computeKey(File file, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = file.getName() + ":" + file.length() + ":" + file.lastModified() + ":" + (content != null ? content : "");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return file.getName() + ":" + file.lastModified();
        }
    }
}
