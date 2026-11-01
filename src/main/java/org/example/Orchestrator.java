package org.example;

import java.io.File;

/** Orchestrates rule-first classification using pluggable AI backends (Ollama or OpenAI-compatible). */
public class Orchestrator {
    private final Config config;
    private final Extractor extractor;

    public Orchestrator(Config config) {
        this.config = config;
        this.extractor = new Extractor();
    }

    public boolean isAvailable() {
        if (config.isDisableAi()) {
            return false;
        }
        try {
            return getBackend().isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    public void warmUp() throws Exception {
        if (!config.isDisableAi()) {
            getBackend().warmUp();
        }
    }

    public AiBackend getBackend() {
        if ("openai-compatible".equalsIgnoreCase(config.getAiBackend())) {
            return new OpenAiCompatibleBackend(config);
        } else {
            return new OllamaBackend(config);
        }
    }

    public Classification classify(File file) throws Exception {
        String userRuleCategory = UserRuleEngine.evaluate(file, config.getUserRules(), config.getCategories());
        if (userRuleCategory != null) {
            return new Classification(userRuleCategory, 1.0, true, "Matched a user rule", "", null);
        }
        String ruleCategory = RuleEngine.evaluate(file);
        if (ruleCategory != null) {
            return new Classification(CategorySanitizer.sanitize(ruleCategory, config.getCategories()), 1.0, true, "Matched a file rule", "", null);
        }
        return classifyContext(extractor.createContext(file));
    }

    public Classification classifyContext(FileContext context) throws Exception {
        if (config.isDisableAi()) {
            return new Classification("Needs_Review", 0.0, false, "AI is disabled by configuration", "", null);
        }
        try {
            Classification classification = getBackend().classify(context, config.getCategories(), config.getConfidenceThreshold());
            if (!classification.ruleBased() && classification.confidence() < config.getConfidenceThreshold()) {
                return new Classification("Needs_Review", classification.confidence(), false,
                        classification.reason(), classification.summary(), classification.suggestedNewCategory());
            }
            return classification;
        } catch (Exception e) {
            return new Classification("Needs_Review", 0.0, false, "AI unavailable or offline error: " + e.getMessage(), "", null);
        }
    }

    public Classification classify(String content, String fileName) throws Exception {
        FileContext context = new FileContext(
                fileName,
                extension(fileName),
                "application/octet-stream",
                content.length(),
                null,
                null,
                "",
                content,
                java.util.Map.of(),
                "legacy",
                java.util.Set.of()
        );
        return classifyContext(context);
    }

    private static String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(index).toLowerCase() : "";
    }
}
