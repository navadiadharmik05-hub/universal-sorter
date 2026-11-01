package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class OllamaBackend implements AiBackend {
    private static final Logger LOGGER = Logger.getLogger(OllamaBackend.class.getName());
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private static final Semaphore SEMAPHORE = new Semaphore(2);
    private final Config config;

    public OllamaBackend(Config config) {
        this.config = config;
    }

    @Override
    public boolean isAvailable() {
        try {
            String url = config.getAiBaseUrl().replaceFirst("/api/chat$", "").replaceFirst("/api/generate$", "").replaceFirst("/+$", "") + "/api/tags";
            NetworkGuard.validateUrl(url, config.isAllowLocalNetwork());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> listModels() {
        List<String> models = new ArrayList<>();
        try {
            String url = config.getAiBaseUrl().replaceFirst("/api/chat$", "").replaceFirst("/api/generate$", "").replaceFirst("/+$", "") + "/api/tags";
            NetworkGuard.validateUrl(url, config.isAllowLocalNetwork());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("models")) {
                    JsonArray arr = root.getAsJsonArray("models");
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject m = arr.get(i).getAsJsonObject();
                        if (m.has("name")) {
                            models.add(m.get("name").getAsString());
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        if (models.isEmpty()) {
            models.add(config.getOllamaModel());
        }
        return models;
    }

    @Override
    public void warmUp() throws Exception {
        long start = System.currentTimeMillis();
        classify(new FileContext("warmup.txt", ".txt", "text/plain", 10, null, null, "", "Say hi in JSON.", java.util.Map.of(), "warmup", java.util.Set.of()), List.of("Misc"), 0.5);
        double secs = (System.currentTimeMillis() - start) / 1000.0;
        LOGGER.info("Ollama warmed up in " + String.format("%.2f", secs) + " s");
    }

    @Override
    public Classification classify(FileContext context, List<String> categories, double threshold) throws Exception {
        String url = config.getAiBaseUrl();
        if (!url.endsWith("/api/chat")) {
            url = url.replaceFirst("/api/generate$", "").replaceFirst("/+$", "") + "/api/chat";
        }
        NetworkGuard.validateUrl(url, config.isAllowLocalNetwork());

        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getOllamaModel());
        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", buildPrompt(context, categories));
        messages.add(msg);
        payload.add("messages", messages);
        payload.addProperty("stream", false);
        payload.addProperty("keep_alive", config.getKeepAlive());
        payload.addProperty("think", false);
        payload.add("format", responseSchema(categories));

        JsonObject options = new JsonObject();
        options.addProperty("temperature", 0);
        options.addProperty("num_ctx", config.getAiContextSize());
        options.addProperty("num_predict", 256);
        payload.add("options", options);

        SEMAPHORE.acquire();
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.getAiTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400 && response.body().toLowerCase().contains("think")) {
                payload.remove("think");
                request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(config.getAiTimeoutSeconds()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                        .build();
                response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonObject envelope = JsonParser.parseString(response.body()).getAsJsonObject();
            String raw = "";
            if (envelope.has("message")) {
                JsonObject m = envelope.getAsJsonObject("message");
                if (m.has("content")) {
                    raw = m.get("content").getAsString();
                }
            }
            if (raw.isBlank() && envelope.has("response")) {
                raw = envelope.get("response").getAsString();
            }
            if (raw.isBlank()) {
                raw = response.body();
            }

            double secs = (System.currentTimeMillis() - start) / 1000.0;
            LOGGER.info("Classified " + context.name() + " in " + String.format("%.2f", secs) + " s");

            return parseClassification(raw, categories, threshold);
        } finally {
            SEMAPHORE.release();
        }
    }

    private String buildPrompt(FileContext context, List<String> categories) {
        String content = context.extractedText();
        int limit = 1500;
        String promptSize = config.getPromptSize();
        if ("compact".equals(promptSize)) {
            limit = 800;
        } else if ("large".equals(promptSize)) {
            limit = 3000;
        }
        if (content != null && content.length() > limit) {
            content = content.substring(0, limit);
        }
        return "Classify this file into exactly one allowed category. Return JSON matching the schema. Allowed categories: " + categories +
                ". File name: " + context.name() +
                ". Extension: " + context.extension() +
                ". MIME: " + context.detectedMime() +
                ". Content preview: " + content;
    }

    private JsonObject responseSchema(List<String> categories) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject catEnum = new JsonObject();
        catEnum.addProperty("type", "string");
        JsonArray values = new JsonArray();
        categories.forEach(values::add);
        values.add("Needs_Review");
        catEnum.add("enum", values);
        properties.add("category", catEnum);

        JsonObject confidence = new JsonObject();
        confidence.addProperty("type", "number");
        properties.add("confidence", confidence);

        JsonObject str = new JsonObject();
        str.addProperty("type", "string");
        properties.add("reason", str);
        properties.add("summary", str);

        schema.add("properties", properties);
        JsonArray req = new JsonArray();
        req.add("category");
        req.add("confidence");
        req.add("reason");
        req.add("summary");
        schema.add("required", req);
        return schema;
    }

    private Classification parseClassification(String raw, List<String> categories, double threshold) {
        JsonObject result = JsonRepair.parseAndRepair(raw);
        String category = result.has("category") ? result.get("category").getAsString() : "Misc";
        double confidence = result.has("confidence") ? result.get("confidence").getAsDouble() : 0;
        confidence = Math.max(0, Math.min(1, confidence));
        String reason = result.has("reason") ? result.get("reason").getAsString() : "";
        String summary = result.has("summary") ? result.get("summary").getAsString() : "";
        String suggested = result.has("suggested_new_category") && !result.get("suggested_new_category").isJsonNull()
                ? result.get("suggested_new_category").getAsString() : null;

        if (confidence < threshold) {
            category = "Needs_Review";
        }
        return new Classification(category, confidence, false, reason, summary, suggested);
    }
}
