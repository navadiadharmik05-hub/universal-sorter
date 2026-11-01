package org.example;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Centralized application font loading with safe platform fallbacks. */
public final class Fonts {
    private static final Logger LOGGER = Logger.getLogger(Fonts.class.getName());
    private static Font sans = new Font("Segoe UI", Font.PLAIN, 13);
    private static Font mono = new Font("Consolas", Font.PLAIN, 12);
    private static Font sansSemi = sans.deriveFont(Font.BOLD);
    private static Font monoSemi = mono.deriveFont(Font.BOLD);
    static {
        load();
    }
    private Fonts() { }
    private static void load() {
        try {
            Font sr = loadFont("/fonts/IBMPlexSans-Regular.ttf");
            Font ss = loadFont("/fonts/IBMPlexSans-SemiBold.ttf");
            Font mr = loadFont("/fonts/IBMPlexMono-Regular.ttf");
            Font ms = loadFont("/fonts/IBMPlexMono-SemiBold.ttf");
            if (sr != null) sans = sr;
            if (ss != null) sansSemi = ss;
            if (mr != null) mono = mr;
            if (ms != null) monoSemi = ms;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "IBM Plex fonts unavailable; using system fallbacks", e);
        }
    }
    private static Font loadFont(String path) throws Exception {
        try (InputStream in = Fonts.class.getResourceAsStream(path)) {
            if (in == null) return null;
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        }
    }
    public static Font ui() { return sans.deriveFont(13f); }
    public static Font label() { return monoSemi.deriveFont(Map.of(TextAttribute.TRACKING, 0.08f)).deriveFont(11f); }
    public static Font mono() { return mono.deriveFont(12f); }
    public static Font title() { return sansSemi.deriveFont(21f); }
}
