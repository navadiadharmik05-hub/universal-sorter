package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Robust JSON extraction and repair utility for AI responses. */
public class JsonRepair {
    public static JsonObject parseAndRepair(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Raw response is null");
        }
        String cleaned = raw
                .replaceAll("(?is)<think>.*?</think>", "")
                .replaceAll("(?is)<thinking>.*?</thinking>", "")
                .trim();
        int unclosedThink = Math.max(cleaned.toLowerCase().lastIndexOf("<think>"), cleaned.toLowerCase().lastIndexOf("<thinking>"));
        if (unclosedThink >= 0) {
            String possibleJson = cleaned.substring(0, unclosedThink).trim();
            if (possibleJson.contains("{")) {
                cleaned = possibleJson;
            }
        }
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        cleaned = cleaned.replaceAll(",\\s*([}\\]])", "$1");

        try {
            return JsonParser.parseString(cleaned).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON even after repair: " + raw, e);
        }
    }
}
