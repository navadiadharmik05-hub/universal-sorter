package org.example;

import java.io.File;

/** Represents a file awaiting review or classified with low confidence. */
public class ReviewItem {
    private String filePath;
    private String fileName;
    private String proposedCategory;
    private double confidence;
    private boolean ruleBased;
    private String extractedSummary;
    private long timestamp;

    public ReviewItem() {}

    public ReviewItem(File file, String proposedCategory, double confidence, boolean ruleBased, String extractedSummary) {
        this.filePath = file.getAbsolutePath();
        this.fileName = file.getName();
        this.proposedCategory = proposedCategory;
        this.confidence = confidence;
        this.ruleBased = ruleBased;
        this.extractedSummary = extractedSummary;
        this.timestamp = System.currentTimeMillis();
    }

    public File getFile() {
        return new File(filePath);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getProposedCategory() {
        return proposedCategory;
    }

    public void setProposedCategory(String proposedCategory) {
        this.proposedCategory = proposedCategory;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isRuleBased() {
        return ruleBased;
    }

    public void setRuleBased(boolean ruleBased) {
        this.ruleBased = ruleBased;
    }

    public String getExtractedSummary() {
        return extractedSummary;
    }

    public void setExtractedSummary(String extractedSummary) {
        this.extractedSummary = extractedSummary;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
