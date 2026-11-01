package org.example;

import java.util.List;

/** Validates and sanitizes category names against security and configuration rules. */
public final class CategorySanitizer {
    private CategorySanitizer() {}

    public static String sanitize(String rawCategory, List<String> allowedCategories) {
        if (rawCategory == null) {
            return "Needs_Review";
        }
        String trimmed = rawCategory.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64) {
            return "Needs_Review";
        }
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("\0")) {
            return "Needs_Review";
        }
        if (allowedCategories != null) {
            for (String allowed : allowedCategories) {
                if (allowed.equalsIgnoreCase(trimmed)) {
                    return allowed;
                }
            }
        }
        return "Needs_Review";
    }
}
