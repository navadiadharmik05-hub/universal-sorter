package org.example;

public class MoveRecord {
    private String sourcePath;
    private String targetPath;
    private String fileName;
    private String category;
    private long timestamp;

    public MoveRecord(String sourcePath, String targetPath, String fileName, String category) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.fileName = fileName;
        this.category = category;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSourcePath() { return sourcePath; }
    public String getTargetPath() { return targetPath; }
    public String getFileName() { return fileName; }
    public String getCategory() { return category; }
    public long getTimestamp() { return timestamp; }
}
