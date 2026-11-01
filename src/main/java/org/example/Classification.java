package org.example;

public record Classification(
        String category,
        double confidence,
        boolean ruleBased,
        String reason,
        String summary,
        String suggestedNewCategory
) {
    public Classification(String category, double confidence, boolean ruleBased) {
        this(category, confidence, ruleBased, "", "", null);
    }
}
