package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Persistent application settings. */
public class Config {
    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
            "Academics",
            "Finance_Invoices",
            "Code_Projects",
            "Installers",
            "Images",
            "Videos",
            "Audio",
            "Archives",
            "Spreadsheets",
            "Presentations",
            "Ebooks",
            "Design",
            "Fonts",
            "Data",
            "Docs",
            "Needs_Review",
            "Misc"
    );

    private String configVersion = "1.2";
    private List<WatchedFolder> watchedFolders;
    private String watchedFolderPath; // Legacy field for migration from old config.json
    private String ollamaUrl;
    private String ollamaModel;
    private String aiBackend = "ollama";
    private String aiBaseUrl;
    private String aiApiKey = "";
    private boolean allowLocalNetwork = false;
    private boolean disableAi = false;
    private String promptSize = "normal";
    private int aiContextSize = 8192;
    private int aiTimeoutSeconds = 120;
    private String keepAlive = "30m";
    private int concurrency = 1;
    private String visionMode = "uninformative";
    private List<String> categories;
    private List<UserRule> userRules;
    private double confidenceThreshold;
    private boolean reviewMode;
    private boolean scanExistingFiles;
    private long stabilityIntervalMs = 3000;

    private transient String watchedFolderOverride;

    public Config() {
        String defaultPath = Paths.get(System.getProperty("user.home"), "Downloads").toString();
        watchedFolders = new ArrayList<>();
        watchedFolders.add(new WatchedFolder(defaultPath, true, false));
        ollamaUrl = "http://localhost:11434/api/generate";
        ollamaModel = "qwen3.5:4b";
        aiBackend = "ollama";
        aiBaseUrl = "http://localhost:11434/api/chat";
        aiApiKey = "";
        allowLocalNetwork = false;
        disableAi = false;
        promptSize = "normal";
        aiContextSize = 8192;
        aiTimeoutSeconds = 120;
        categories = new ArrayList<>(DEFAULT_CATEGORIES);
        userRules = new ArrayList<>();
        confidenceThreshold = 0.75;
        reviewMode = false;
        scanExistingFiles = false;
        stabilityIntervalMs = 3000;
    }

    public void exportSettings(File targetFile) throws IOException {
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(this);
        AtomicJsonWriter.writeJson(targetFile, json);
    }

    public static Config importSettings(File sourceFile) throws IOException {
        try (FileReader reader = new FileReader(sourceFile)) {
            Config loaded = new Gson().fromJson(reader, Config.class);
            if (loaded == null) {
                throw new IOException("Invalid JSON settings file");
            }
            loaded.applyDefaults();
            return loaded;
        }
    }

    public static Config load() {
        File file = new File("config.json");
        if (!file.exists()) {
            return new Config();
        }

        try (FileReader reader = new FileReader(file)) {
            Config loaded = new Gson().fromJson(reader, Config.class);
            if (loaded == null) {
                return new Config();
            }
            loaded.applyDefaults();
            return loaded;
        } catch (Exception e) {
            System.err.println("Failed to load config.json, using defaults: " + e.getMessage());
            return new Config();
        }
    }

    public void save() {
        try {
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(this);
            AtomicJsonWriter.writeJson(new File("config.json"), json);
        } catch (IOException e) {
            System.err.println("Failed to save config.json: " + e.getMessage());
        }
    }

    private void applyDefaults() {
        Config defaults = new Config();
        if (configVersion == null || configVersion.isBlank()) {
            configVersion = "1.2";
        }
        if (watchedFolders == null) {
            watchedFolders = new ArrayList<>();
        }
        if (watchedFolders.isEmpty()) {
            if (watchedFolderPath != null && !watchedFolderPath.isBlank()) {
                watchedFolders.add(new WatchedFolder(watchedFolderPath, true, false));
            } else {
                watchedFolders.addAll(defaults.watchedFolders);
            }
        }
        watchedFolderPath = null; // Clear legacy field after migration

        if (ollamaUrl == null || ollamaUrl.isBlank()) {
            ollamaUrl = defaults.ollamaUrl;
        }
        if (aiBaseUrl == null || aiBaseUrl.isBlank()) {
            aiBaseUrl = defaults.aiBaseUrl;
        }
        if (keepAlive == null || keepAlive.isBlank()) {
            keepAlive = defaults.keepAlive;
        }
        if (aiContextSize <= 0) {
            aiContextSize = defaults.aiContextSize;
        }
        if (concurrency <= 0) {
            concurrency = defaults.concurrency;
        }
        if (visionMode == null || visionMode.isBlank()) {
            visionMode = defaults.visionMode;
        }
        if (promptSize == null || promptSize.isBlank()) {
            promptSize = defaults.promptSize;
        }
        if (ollamaModel == null || ollamaModel.isBlank()) {
            ollamaModel = defaults.ollamaModel;
        }
        if (categories == null || categories.isEmpty()) {
            categories = new ArrayList<>(DEFAULT_CATEGORIES);
        } else {
            categories = new ArrayList<>(categories);
            for (String def : DEFAULT_CATEGORIES) {
                if (!categories.contains(def)) {
                    categories.add(def);
                }
            }
        }
        if (userRules == null) {
            userRules = new ArrayList<>();
        }
        if (confidenceThreshold < 0 || confidenceThreshold > 1) {
            confidenceThreshold = defaults.confidenceThreshold;
        }
        if (stabilityIntervalMs < 0) {
            stabilityIntervalMs = 3000;
        }
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    public List<WatchedFolder> getWatchedFolders() {
        if (watchedFolders == null) {
            watchedFolders = new ArrayList<>();
        }
        return watchedFolders;
    }

    public void setWatchedFolders(List<WatchedFolder> watchedFolders) {
        this.watchedFolders = watchedFolders != null ? watchedFolders : new ArrayList<>();
    }

    public String getWatchedFolderPath() {
        if (watchedFolderOverride != null && !watchedFolderOverride.isBlank()) {
            return watchedFolderOverride;
        }
        if (watchedFolders != null && !watchedFolders.isEmpty()) {
            return watchedFolders.get(0).getPath();
        }
        return Paths.get(System.getProperty("user.home"), "Downloads").toString();
    }

    public void setWatchedFolderPath(String watchedFolderPath) {
        if (watchedFolders == null) {
            watchedFolders = new ArrayList<>();
        }
        if (watchedFolders.isEmpty()) {
            watchedFolders.add(new WatchedFolder(watchedFolderPath, true, false));
        } else {
            watchedFolders.get(0).setPath(watchedFolderPath);
        }
    }

    public void setWatchedFolderOverride(String watchedFolderOverride) {
        this.watchedFolderOverride = watchedFolderOverride;
    }

    public boolean hasWatchedFolderOverride() {
        return watchedFolderOverride != null && !watchedFolderOverride.isBlank();
    }

    public String getOllamaUrl() {
        return ollamaUrl;
    }

    public void setOllamaUrl(String ollamaUrl) {
        this.ollamaUrl = ollamaUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public void setOllamaModel(String ollamaModel) {
        this.ollamaModel = ollamaModel;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<UserRule> getUserRules() {
        if (userRules == null) {
            userRules = new ArrayList<>();
        }
        return userRules;
    }

    public void setUserRules(List<UserRule> userRules) {
        this.userRules = userRules != null ? userRules : new ArrayList<>();
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public boolean isReviewMode() {
        return reviewMode;
    }

    public void setReviewMode(boolean reviewMode) {
        this.reviewMode = reviewMode;
    }

    public boolean isScanExistingFiles() {
        return scanExistingFiles;
    }

    public void setScanExistingFiles(boolean scanExistingFiles) {
        this.scanExistingFiles = scanExistingFiles;
    }

    public long getStabilityIntervalMs() {
        return stabilityIntervalMs;
    }

    public void setStabilityIntervalMs(long stabilityIntervalMs) {
        this.stabilityIntervalMs = stabilityIntervalMs;
    }

    public String getAiBackend() {
        return aiBackend != null ? aiBackend : "ollama";
    }

    public void setAiBackend(String aiBackend) {
        this.aiBackend = aiBackend;
    }

    public String getAiBaseUrl() {
        if (aiBaseUrl != null && !aiBaseUrl.isBlank()) {
            return aiBaseUrl;
        }
        return ollamaUrl != null ? ollamaUrl : "http://localhost:11434/api/chat";
    }

    public void setAiBaseUrl(String aiBaseUrl) {
        this.aiBaseUrl = aiBaseUrl;
    }

    public String getAiApiKey() {
        return aiApiKey != null ? aiApiKey : "";
    }

    public void setAiApiKey(String aiApiKey) {
        this.aiApiKey = aiApiKey;
    }

    public boolean isAllowLocalNetwork() {
        return allowLocalNetwork;
    }

    public void setAllowLocalNetwork(boolean allowLocalNetwork) {
        this.allowLocalNetwork = allowLocalNetwork;
    }

    public boolean isDisableAi() {
        return disableAi;
    }

    public void setDisableAi(boolean disableAi) {
        this.disableAi = disableAi;
    }

    public String getPromptSize() {
        return promptSize != null ? promptSize : "normal";
    }

    public void setPromptSize(String promptSize) {
        this.promptSize = promptSize;
    }

    public int getAiContextSize() {
        return aiContextSize > 0 ? aiContextSize : 8192;
    }

    public void setAiContextSize(int aiContextSize) {
        this.aiContextSize = aiContextSize;
    }

    public int getAiTimeoutSeconds() {
        return aiTimeoutSeconds > 0 ? aiTimeoutSeconds : 120;
    }

    public void setAiTimeoutSeconds(int aiTimeoutSeconds) {
        this.aiTimeoutSeconds = aiTimeoutSeconds;
    }

    public String getKeepAlive() {
        return keepAlive != null && !keepAlive.isBlank() ? keepAlive : "30m";
    }

    public void setKeepAlive(String keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getConcurrency() {
        return concurrency > 0 ? concurrency : 1;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = Math.max(1, Math.min(4, concurrency));
    }

    public String getVisionMode() {
        return visionMode != null && !visionMode.isBlank() ? visionMode : "uninformative";
    }

    public void setVisionMode(String visionMode) {
        this.visionMode = visionMode;
    }
}
