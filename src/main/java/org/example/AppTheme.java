package org.example;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared application theme colors, retro palette defaults, and artwork. */
public final class AppTheme {
    public static final Color WINDOW_BG = Color.decode("#2E4742");
    public static final Color PANEL = Color.decode("#3D5F58");
    public static final Color INK = Color.decode("#10201D");
    public static final Color CREAM = Color.decode("#F7F7F2");
    public static final Color CORAL = Color.decode("#E97B77");
    public static final Color CORAL_SHADOW = Color.decode("#671912");
    public static final Color AMBER = Color.decode("#F5B726");
    public static final Color SKY = Color.decode("#8BB2DE");
    public static final Color GHOST_BORDER = Color.decode("#C9D2D1");
    public static final Color SUCCESS = Color.decode("#8FD9A8");

    public static final List<Color> CHIP_COLORS = List.of(
            AMBER, SKY, CORAL, SUCCESS, Color.decode("#D4A5A5")
    );

    private static boolean installed;

    private AppTheme() {
    }

    /** Installs the global FlatLaf theme and component defaults once with the retro palette. */
    public static synchronized void install() {
        if (installed) {
            return;
        }

        Map<String, String> extraDefaults = new LinkedHashMap<>();
        extraDefaults.put("@background", "#2E4742");
        extraDefaults.put("@foreground", "#F7F7F2");
        extraDefaults.put("@accentColor", "#F5B726");
        extraDefaults.put("@selectionBackground", "#F5B726");
        extraDefaults.put("@selectionForeground", "#10201D");
        extraDefaults.put("@componentBackground", "#3D5F58");
        extraDefaults.put("@borderColor", "#C9D2D1");
        extraDefaults.put("Button.arc", "8");
        extraDefaults.put("Component.arc", "8");
        extraDefaults.put("TextComponent.arc", "8");
        FlatLaf.setGlobalExtraDefaults(extraDefaults);
        FlatDarkLaf.setup();

        UIManager.put("Panel.background", WINDOW_BG);
        UIManager.put("Panel.foreground", CREAM);
        UIManager.put("Component.focusColor", AMBER);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.borderColor", GHOST_BORDER);
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("Button.background", PANEL);
        UIManager.put("Button.foreground", CREAM);
        UIManager.put("Button.borderColor", GHOST_BORDER);
        UIManager.put("TextComponent.arc", 8);

        UIManager.put("TabbedPane.background", WINDOW_BG);
        UIManager.put("TabbedPane.foreground", GHOST_BORDER);
        UIManager.put("TabbedPane.selectedBackground", PANEL);
        UIManager.put("TabbedPane.selectedForeground", CREAM);
        UIManager.put("TabbedPane.hoverColor", PANEL);
        UIManager.put("TabbedPane.focusColor", AMBER);
        UIManager.put("TabbedPane.underlineColor", AMBER);
        UIManager.put("TabbedPane.inactiveUnderlineColor", GHOST_BORDER);
        UIManager.put("TabbedPane.tabHeight", 40);
        UIManager.put("TabbedPane.contentAreaColor", PANEL);
        UIManager.put("TabbedPane.contentBorderInsets", new java.awt.Insets(1, 0, 0, 0));

        UIManager.put("Table.background", PANEL);
        UIManager.put("Table.foreground", CREAM);
        UIManager.put("Table.alternateRowColor", Color.decode("#35534D"));
        UIManager.put("Table.selectionBackground", AMBER);
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("Table.gridColor", INK);
        UIManager.put("TableHeader.background", INK);
        UIManager.put("TableHeader.foreground", CREAM);
        UIManager.put("TableHeader.separatorColor", INK);
        UIManager.put("TableHeader.height", 32);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);

        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.track", WINDOW_BG);
        UIManager.put("ScrollBar.thumb", GHOST_BORDER);
        UIManager.put("ScrollBar.hoverThumbColor", AMBER);
        UIManager.put("ScrollBar.pressedThumbColor", CORAL);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.showButtons", false);

        UIManager.put("TextField.background", PANEL);
        UIManager.put("TextField.foreground", CREAM);
        UIManager.put("TextField.caretForeground", AMBER);
        UIManager.put("TextField.placeholderForeground", GHOST_BORDER);
        UIManager.put("TextArea.background", INK);
        UIManager.put("TextArea.foreground", CREAM);
        UIManager.put("TextArea.caretForeground", AMBER);
        UIManager.put("ComboBox.background", PANEL);
        UIManager.put("ComboBox.foreground", CREAM);
        UIManager.put("ComboBox.selectionBackground", AMBER);
        UIManager.put("ComboBox.selectionForeground", INK);
        UIManager.put("Spinner.background", PANEL);
        UIManager.put("Spinner.foreground", CREAM);
        UIManager.put("Spinner.arrowButtonBackground", PANEL);
        UIManager.put("Spinner.arrowButtonForeground", CREAM);

        UIManager.put("ProgressBar.background", PANEL);
        UIManager.put("ProgressBar.foreground", AMBER);
        UIManager.put("ProgressBar.selectionBackground", CREAM);
        UIManager.put("ProgressBar.selectionForeground", INK);
        UIManager.put("ProgressBar.arc", 8);

        UIManager.put("ToolTip.background", PANEL);
        UIManager.put("ToolTip.foreground", CREAM);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(INK, 2));
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.foreground", CREAM);
        UIManager.put("OptionPane.messageForeground", CREAM);
        UIManager.put("OptionPane.buttonAreaBorder", BorderFactory.createEmptyBorder(8, 8, 8, 8));
        UIManager.put("OptionPane.border", BorderFactory.createLineBorder(INK, 2));
        UIManager.put("Dialog.background", WINDOW_BG);
        UIManager.put("Label.foreground", CREAM);
        UIManager.put("defaultFont", Fonts.ui());

        installed = true;
    }

    /** Returns artwork image suitable for window icons. */
    public static BufferedImage createPumpkinImage(int size) {
        int dimension = Math.max(8, size);
        BufferedImage image = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int stemWidth = Math.max(2, dimension / 8);
            int stemHeight = Math.max(3, dimension / 5);
            int stemX = dimension / 2 - stemWidth / 2;
            int stemY = Math.max(1, dimension / 12);
            g.setColor(SUCCESS);
            g.fill(new RoundRectangle2D.Double(stemX, stemY, stemWidth, stemHeight, stemWidth, stemWidth));

            int bodyX = Math.max(1, dimension / 8);
            int bodyY = Math.max(3, dimension / 4);
            int bodyWidth = dimension - bodyX * 2;
            int bodyHeight = Math.max(5, dimension - bodyY - dimension / 10);
            g.setColor(CORAL);
            g.fillOval(bodyX + dimension / 8, bodyY, bodyWidth - dimension / 4, bodyHeight);
            g.fillOval(bodyX, bodyY + dimension / 12, bodyWidth / 2 + dimension / 10, bodyHeight - dimension / 8);
            g.fillOval(bodyX + bodyWidth / 2 - dimension / 10, bodyY + dimension / 12,
                    bodyWidth / 2 + dimension / 10, bodyHeight - dimension / 8);

            g.setColor(CORAL_SHADOW);
            g.setStroke(new BasicStroke(Math.max(1f, dimension / 18f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int leftRidge = bodyX + bodyWidth / 3;
            int rightRidge = bodyX + bodyWidth * 2 / 3;
            g.drawLine(leftRidge, bodyY + dimension / 8, leftRidge - dimension / 16, bodyY + bodyHeight - dimension / 8);
            g.drawLine(rightRidge, bodyY + dimension / 8, rightRidge + dimension / 16, bodyY + bodyHeight - dimension / 8);

            g.setColor(INK);
            int eyeSize = Math.max(2, dimension / 10);
            int eyeY = bodyY + bodyHeight / 3;
            g.fillOval(bodyX + bodyWidth / 3 - eyeSize / 2, eyeY, eyeSize, eyeSize);
            g.fillOval(bodyX + bodyWidth * 2 / 3 - eyeSize / 2, eyeY, eyeSize, eyeSize);
            int mouthWidth = Math.max(4, dimension / 3);
            int mouthHeight = Math.max(2, dimension / 10);
            g.fillRoundRect(dimension / 2 - mouthWidth / 2, bodyY + bodyHeight * 2 / 3,
                    mouthWidth, mouthHeight, mouthHeight, mouthHeight);
        } finally {
            g.dispose();
        }
        return image;
    }

    public static List<java.awt.Image> createPumpkinImages() {
        return Arrays.asList(
                createPumpkinImage(16),
                createPumpkinImage(32),
                createPumpkinImage(64)
        );
    }
}
