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

public class OpenAiCompatibleBackend implements AiBackend {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final Config config;

    public OpenAiCompatibleBackend(Config config) {
        this.config = config;
    }

    @Override
    public boolean isAvailable() {
        try {
            String baseUrl = config.getAiBaseUrl();
            String modelsUrl = baseUrl.replaceFirst("/chat/completions$", "").replaceFirst("/v1$", "/v1/models");
            if (!modelsUrl.endsWith("/models")) {
                modelsUrl = baseUrl.replaceAll("/+$", "") + "/models";
            }
            NetworkGuard.validateUrl(modelsUrl, config.isAllowLocalNetwork());
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .timeout(Duration.ofSeconds(3))
                    .GET();
            if (config.getAiApiKey() != null && !config.getAiApiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + config.getAiApiKey());
            }
            HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> listModels() {
        List<String> models = new ArrayList<>();
        try {
            String baseUrl = config.getAiBaseUrl();
            String modelsUrl = baseUrl.replaceFirst("/chat/completions$", "").replaceFirst("/v1$", "/v1/models");
            if (!modelsUrl.endsWith("/models")) {
                modelsUrl = baseUrl.replaceAll("/+$", "") + "/models";
            }
            NetworkGuard.validateUrl(modelsUrl, config.isAllowLocalNetwork());
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET();
            if (config.getAiApiKey() != null && !config.getAiApiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + config.getAiApiKey());
            }
            HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("data")) {
                    JsonArray arr = root.getAsJsonArray("data");
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject m = arr.get(i).getAsJsonObject();
                        if (m.has("id")) {
                            models.add(m.get("id").getAsString());
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
        classify(new FileContext("warmup.txt", ".txt", "text/plain", 10, null, null, "", "Say hi in JSON.", java.util.Map.of(), "warmup", java.util.Set.of()), List.of("Misc"), 0.5);
    }

    @Override
    public Classification classify(FileContext context, List<String> categories, double threshold) throws Exception {
        String url = config.getAiBaseUrl();
        if (!url.endsWith("/chat/completions")) {
            url = url.replaceAll("/+$", "") + "/chat/completions";
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
        payload.addProperty("temperature", 0);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json");
        if (config.getAiApiKey() != null && !config.getAiApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + config.getAiApiKey());
        }
        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload))).build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI-compatible server returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        String raw = "";
        if (root.has("choices")) {
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                if (choice.has("message")) {
                    JsonObject message = choice.getAsJsonObject("message");
                    if (message.has("content")) {
                        raw = message.get("content").getAsString();
                    }
                }
            }
        }
        if (raw.isBlank()) {
            raw = response.body();
        }

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

    private String buildPrompt(FileContext context, List<String> categories) {
        String content = context.extractedText();
        if ("compact".equals(config.getPromptSize()) && content != null && content.length() > 500) {
            content = content.substring(0, 500);
        }
        return "Classify this file into exactly one allowed category. Return JSON with keys: category, confidence, reason, summary. Allowed categories: " + categories +
                ". File name: " + context.name() +
                ". Extension: " + context.extension() +
                ". MIME: " + context.detectedMime() +
                ". Content preview: " + content;
    }
}
