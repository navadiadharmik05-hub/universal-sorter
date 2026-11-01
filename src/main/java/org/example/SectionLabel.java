package org.example;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Uppercase section heading with the three-color retro marker. */
public class SectionLabel extends JPanel {
    private final JLabel label;

    public SectionLabel(String text) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JPanel marker = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(AppTheme.AMBER);
                    g.fillRect(0, 0, 8, 8);
                    g.setColor(AppTheme.SKY);
                    g.fillRect(10, 0, 8, 8);
                    g.setColor(AppTheme.CORAL);
                    g.fillRect(20, 0, 8, 8);
                } finally {
                    g.dispose();
                }
            }
            @Override
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(28, 8);
            }
        };
        marker.setOpaque(false);
        add(marker);

        label = new JLabel(text == null ? "" : text.toUpperCase(java.util.Locale.ROOT));
        label.setFont(Fonts.label());
        label.setForeground(AppTheme.CREAM);
        add(label);
    }

    public JLabel getLabel() {
        return label;
    }
}
