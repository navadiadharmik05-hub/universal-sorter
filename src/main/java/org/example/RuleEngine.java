package org.example;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/** Comprehensive data-driven rule engine covering hundreds of file types and extensions. */
public class RuleEngine {

    private static final Map<String, String> EXTENSION_MAP = Map.ofEntries(
            // Images
            Map.entry(".png", "Images"),
            Map.entry(".jpg", "Images"),
            Map.entry(".jpeg", "Images"),
            Map.entry(".gif", "Images"),
            Map.entry(".webp", "Images"),
            Map.entry(".svg", "Images"),
            Map.entry(".bmp", "Images"),
            Map.entry(".tiff", "Images"),
            Map.entry(".ico", "Images"),
            Map.entry(".psd", "Images"),
            Map.entry(".ai", "Images"),
            Map.entry(".heic", "Images"),
            Map.entry(".raw", "Images"),
            Map.entry(".cr2", "Images"),
            Map.entry(".nef", "Images"),

            // Videos
            Map.entry(".mp4", "Videos"),
            Map.entry(".mkv", "Videos"),
            Map.entry(".avi", "Videos"),
            Map.entry(".mov", "Videos"),
            Map.entry(".wmv", "Videos"),
            Map.entry(".webm", "Videos"),
            Map.entry(".flv", "Videos"),
            Map.entry(".m4v", "Videos"),
            Map.entry(".mpg", "Videos"),
            Map.entry(".mpeg", "Videos"),

            // Audio
            Map.entry(".mp3", "Audio"),
            Map.entry(".wav", "Audio"),
            Map.entry(".flac", "Audio"),
            Map.entry(".aac", "Audio"),
            Map.entry(".ogg", "Audio"),
            Map.entry(".m4a", "Audio"),
            Map.entry(".wma", "Audio"),
            Map.entry(".opus", "Audio"),
            Map.entry(".mid", "Audio"),

            // Spreadsheets
            Map.entry(".xlsx", "Spreadsheets"),
            Map.entry(".xls", "Spreadsheets"),
            Map.entry(".csv", "Spreadsheets"),
            Map.entry(".ods", "Spreadsheets"),
            Map.entry(".tsv", "Spreadsheets"),

            // Presentations
            Map.entry(".pptx", "Presentations"),
            Map.entry(".ppt", "Presentations"),
            Map.entry(".keynote", "Presentations"),
            Map.entry(".odp", "Presentations"),

            // Ebooks
            Map.entry(".epub", "Ebooks"),
            Map.entry(".mobi", "Ebooks"),
            Map.entry(".azw3", "Ebooks"),
            Map.entry(".fb2", "Ebooks"),

            // Archives
            Map.entry(".zip", "Archives"),
            Map.entry(".rar", "Archives"),
            Map.entry(".7z", "Archives"),
            Map.entry(".tar", "Archives"),
            Map.entry(".gz", "Archives"),
            Map.entry(".bz2", "Archives"),
            Map.entry(".xz", "Archives"),
            Map.entry(".iso", "Archives"),
            Map.entry(".dmg", "Archives"),

            // Installers
            Map.entry(".exe", "Installers"),
            Map.entry(".msi", "Installers"),
            Map.entry(".pkg", "Installers"),
            Map.entry(".deb", "Installers"),
            Map.entry(".rpm", "Installers"),
            Map.entry(".AppImage", "Installers"),

            // Design / CAD / 3D
            Map.entry(".fig", "Design"),
            Map.entry(".sketch", "Design"),
            Map.entry(".dwg", "Design"),
            Map.entry(".stl", "Design"),
            Map.entry(".obj", "Design"),
            Map.entry(".blend", "Design"),
            Map.entry(".step", "Design"),
            Map.entry(".iges", "Design"),

            // Fonts
            Map.entry(".ttf", "Fonts"),
            Map.entry(".otf", "Fonts"),
            Map.entry(".woff", "Fonts"),
            Map.entry(".woff2", "Fonts"),

            // Data / Configs
            Map.entry(".json", "Data"),
            Map.entry(".yaml", "Data"),
            Map.entry(".yml", "Data"),
            Map.entry(".toml", "Data"),
            Map.entry(".ini", "Data"),
            Map.entry(".env", "Data"),
            Map.entry(".config", "Data"),
            Map.entry(".sql", "Data"),
            Map.entry(".db", "Data"),
            Map.entry(".sqlite", "Data"),

            // Code
            Map.entry(".java", "Code_Projects"),
            Map.entry(".py", "Code_Projects"),
            Map.entry(".js", "Code_Projects"),
            Map.entry(".ts", "Code_Projects"),
            Map.entry(".cpp", "Code_Projects"),
            Map.entry(".c", "Code_Projects"),
            Map.entry(".h", "Code_Projects"),
            Map.entry(".cs", "Code_Projects"),
            Map.entry(".go", "Code_Projects"),
            Map.entry(".rs", "Code_Projects"),
            Map.entry(".rb", "Code_Projects"),
            Map.entry(".php", "Code_Projects"),
            Map.entry(".swift", "Code_Projects"),
            Map.entry(".kt", "Code_Projects"),
            Map.entry(".html", "Code_Projects"),
            Map.entry(".css", "Code_Projects"),
            Map.entry(".sh", "Code_Projects"),
            Map.entry(".bat", "Code_Projects"),

            // Logs
            Map.entry(".log", "Misc"),
            Map.entry(".out", "Misc"),
            Map.entry(".err", "Misc"),

            // Subtitles
            Map.entry(".srt", "Misc"),
            Map.entry(".sub", "Misc"),
            Map.entry(".vtt", "Misc"),

            // Certificates
            Map.entry(".pem", "Data"),
            Map.entry(".crt", "Data"),
            Map.entry(".cer", "Data"),
            Map.entry(".pfx", "Data"),

            // Torrents
            Map.entry(".torrent", "Misc")
    );

    /**
     * Attempts to categorize a file using extension or name rules before falling back to AI.
     * Returns null if no rule matches.
     */
    public static String evaluate(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);

        // Keyword rules for filename
        if (name.contains("invoice") || name.contains("bill") || name.contains("receipt") || name.contains("statement")) {
            return "Finance_Invoices";
        }
        if (name.contains("resume") || name.contains("cv") || name.contains("syllabus") || name.contains("assignment") || name.contains("thesis")) {
            return "Academics";
        }

        // Extension rules with support for double extensions like .tar.gz
        String ext = getExtension(name);
        if (ext.isEmpty()) return null;

        if (EXTENSION_MAP.containsKey(ext)) {
            return EXTENSION_MAP.get(ext);
        }

        if (ext.equals(".pdf") || ext.equals(".docx") || ext.equals(".doc") || ext.equals(".odt") || ext.equals(".rtf")) {
            if (name.contains("report") || name.contains("notes") || name.contains("memo") || name.contains("article")) {
                return "Docs";
            }
        }

        return null;
    }

    private static String getExtension(String name) {
        if (name.endsWith(".tar.gz")) {
            return ".tar.gz";
        }
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot) : "";
    }
}
