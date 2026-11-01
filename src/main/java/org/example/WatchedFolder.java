package org.example;

public class WatchedFolder {
    private String path;
    private boolean autoWatch;
    private boolean includeSubfolders;
    private String lastSorted;

    public WatchedFolder() {
        this.autoWatch = true;
        this.includeSubfolders = false;
    }

    public WatchedFolder(String path, boolean autoWatch, boolean includeSubfolders) {
        this.path = path;
        this.autoWatch = autoWatch;
        this.includeSubfolders = includeSubfolders;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isAutoWatch() {
        return autoWatch;
    }

    public void setAutoWatch(boolean autoWatch) {
        this.autoWatch = autoWatch;
    }

    public boolean isIncludeSubfolders() {
        return includeSubfolders;
    }

    public void setIncludeSubfolders(boolean includeSubfolders) {
        this.includeSubfolders = includeSubfolders;
    }

    public String getLastSorted() {
        return lastSorted;
    }

    public void setLastSorted(String lastSorted) {
        this.lastSorted = lastSorted;
    }
}
