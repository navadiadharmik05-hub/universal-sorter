package org.example;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** A flat, hard-shadow button for the retro application chrome. */
public class RetroButton extends JButton {
    public enum Variant { PRIMARY, GHOST, SUCCESS }

    private final Variant variant;
    private boolean pressed;

    public RetroButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(Fonts.ui().deriveFont(Font.BOLD, 13f));
        setMargin(new Insets(8, 14, 8, 14));
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(Math.max(120, getPreferredSize().width + 8), 38));
        addChangeListener(e -> {
            boolean nowPressed = getModel().isPressed();
            if (nowPressed != pressed) {
                pressed = nowPressed;
                repaint();
            }
        });
    }

    public Variant getVariant() {
        return variant;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int shadow = pressed ? 2 : 4;
            int offset = pressed ? 2 : 0;
            int width = getWidth() - shadow - 2;
            int height = getHeight() - shadow - 2;
            int radius = 8;

            g.translate(offset, offset);
            g.setColor(variant == Variant.PRIMARY ? AppTheme.CORAL_SHADOW : AppTheme.INK);
            g.fill(new RoundRectangle2D.Double(shadow, shadow, width, height, radius, radius));
            g.setColor(variant == Variant.PRIMARY ? AppTheme.CORAL : new Color(0, 0, 0, 0));
            if (variant == Variant.PRIMARY) {
                g.fill(new RoundRectangle2D.Double(0, 0, width, height, radius, radius));
            }
            g.setColor(variant == Variant.PRIMARY ? AppTheme.INK : AppTheme.GHOST_BORDER);
            g.setStroke(new java.awt.BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(1, 1, width - 2, height - 2, radius, radius));
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    @Override
    protected void paintBorder(Graphics graphics) {
        // The complete retro border and shadow are painted in paintComponent.
    }
}
