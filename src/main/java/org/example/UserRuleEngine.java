package org.example;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Evaluates persisted user rules before the built-in rule engine. */
public final class UserRuleEngine {
    private UserRuleEngine() {}

    public static String evaluate(File file, List<UserRule> rules, List<String> allowedCategories) {
        if (file == null || rules == null) return null;
        String fileName = file.getName();
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        for (UserRule rule : rules) {
            if (rule == null || rule.getType() == null || rule.getPattern() == null || rule.getTargetCategory() == null) continue;
            boolean matches;
            try {
                matches = switch (rule.getType()) {
                    case FILENAME_CONTAINS -> lowerName.contains(rule.getPattern().trim().toLowerCase(Locale.ROOT));
                    case EXTENSION -> lowerName.endsWith(normalizeExtension(rule.getPattern()));
                    case REGEX -> Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE).matcher(fileName).find();
                };
            } catch (RuntimeException ignored) {
                matches = false;
            }
            if (matches) return CategorySanitizer.sanitize(rule.getTargetCategory(), allowedCategories);
        }
        return null;
    }

    private static String normalizeExtension(String extension) {
        String value = extension.trim().toLowerCase(Locale.ROOT);
        return value.startsWith(".") ? value : "." + value;
    }
}
