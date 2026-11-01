package org.example;

import java.util.Objects;

/** Represents a user-defined custom sorting rule. */
public class UserRule {
    public enum Type {
        FILENAME_CONTAINS,
        EXTENSION,
        REGEX
    }

    private Type type;
    private String pattern;
    private String targetCategory;

    public UserRule() {}

    public UserRule(Type type, String pattern, String targetCategory) {
        this.type = type;
        this.pattern = pattern;
        this.targetCategory = targetCategory;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getTargetCategory() {
        return targetCategory;
    }

    public void setTargetCategory(String targetCategory) {
        this.targetCategory = targetCategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRule userRule = (UserRule) o;
        return type == userRule.type && Objects.equals(pattern, userRule.pattern) && Objects.equals(targetCategory, userRule.targetCategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, pattern, targetCategory);
    }
}
