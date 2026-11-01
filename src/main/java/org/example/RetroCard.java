package org.example;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Panel with the application's hard-shadow card treatment. */
public class RetroCard extends JPanel {
    public RetroCard() {
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int shadow = 6;
            int width = getWidth() - shadow - 2;
            int height = getHeight() - shadow - 2;
            g.setColor(AppTheme.INK);
            g.fill(new RoundRectangle2D.Double(shadow, shadow, width, height, 8, 8));
            g.setColor(AppTheme.PANEL);
            g.fill(new RoundRectangle2D.Double(0, 0, width, height, 8, 8));
            g.setColor(AppTheme.INK);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(1, 1, width - 2, height - 2, 8, 8));
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }
}
