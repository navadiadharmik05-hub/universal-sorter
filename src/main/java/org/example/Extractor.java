package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Utility class for extracting text and building FileContext for classification. */
public class Extractor {

    public FileContext createContext(File file) {
        Path path = file.toPath();
        String name = file.getName();
        String extension = getExtension(name);
        long size = file.exists() ? file.length() : 0L;
        Instant created = Instant.now();
        Instant modified = Instant.now();
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            created = attrs.creationTime().toInstant();
            modified = attrs.lastModifiedTime().toInstant();
        } catch (Exception ignored) {}

        String detectedMime = detectMime(extension);
        String extractionMethod = "none";
        String extractedText = "";

        try {
            String lower = name.toLowerCase();
            if (lower.endsWith(".pdf")) {
                extractionMethod = "pdfbox";
                extractedText = extractFirstPageFromPdf(path);
            } else if (lower.endsWith(".docx") || lower.endsWith(".xlsx") || lower.endsWith(".pptx") || lower.endsWith(".zip")) {
                extractionMethod = "zip-xml";
                extractedText = extractTextFromZipXml(path);
            } else if (isTextFile(extension) || size < 512 * 1024) {
                extractionMethod = "text-lines";
                extractedText = extractFirst20Lines(path);
            }
        } catch (Exception e) {
            extractionMethod = "error: " + e.getMessage();
        }

        if (extractedText.length() > 2000) {
            extractedText = extractedText.substring(0, 2000);
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("absolutePath", file.getAbsolutePath());
        metadata.put("parentDir", file.getParent() != null ? file.getParent() : "");

        Set<String> flags = Set.of();
        if (size == 0) {
            flags = Set.of("zero-byte");
        }

        return new FileContext(
                name,
                extension,
                detectedMime,
                size,
                created,
                modified,
                file.getParent() != null ? file.getParent() : "",
                extractedText,
                metadata,
                extractionMethod,
                flags
        );
    }

    public String extract(Path path) {
        return createContext(path.toFile()).extractedText();
    }

    public String extractContent(File file) {
        return extract(file.toPath());
    }

    private static String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        return idx > 0 && idx < name.length() - 1 ? name.substring(idx).toLowerCase() : "";
    }

    private static String detectMime(String ext) {
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".txt" -> "text/plain";
            case ".csv" -> "text/csv";
            case ".json" -> "application/json";
            case ".xml" -> "application/xml";
            case ".html", ".htm" -> "text/html";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".zip" -> "application/zip";
            case ".mp3" -> "audio/mpeg";
            case ".mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }

    private static boolean isTextFile(String ext) {
        return Set.of(".txt", ".md", ".csv", ".json", ".xml", ".html", ".java", ".py", ".js", ".log", ".ini", ".properties").contains(ext);
    }

    private static String extractFirstPageFromPdf(Path pdfPath) throws IOException {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String text = stripper.getText(document);
            return text != null ? text.trim() : "";
        }
    }

    private static String extractTextFromZipXml(Path path) {
        StringBuilder sb = new StringBuilder();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            zip.stream().forEach(entry -> {
                String name = entry.getName().toLowerCase();
                if ((name.endsWith(".xml") || name.endsWith(".txt")) && sb.length() < 3000) {
                    try (InputStream is = zip.getInputStream(entry)) {
                        String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile(">([^<]+)<").matcher(xml);
                        while (m.find() && sb.length() < 3000) {
                            String t = m.group(1).trim();
                            if (!t.isEmpty()) {
                                sb.append(t).append(" ");
                            }
                        }
                    } catch (IOException ignored) {}
                }
            });
        } catch (Exception ignored) {}
        String res = sb.toString().trim();
        return res;
    }

    private static String extractFirst20Lines(Path filePath) throws IOException {
        List<String> allLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        return allLines.stream()
                .limit(30)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
