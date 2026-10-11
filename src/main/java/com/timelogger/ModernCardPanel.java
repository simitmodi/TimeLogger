package com.timelogger;

import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Modern card container panel styled after Windows 11 Fluent mica/card design.
 * Features smooth antialiased rounded borders, clean elevation fills, and consistent padding.
 */
public class ModernCardPanel extends JPanel {
    private int cornerRadius = 12;
    private Color customBg = null;
    private Color customBorder = null;

    public ModernCardPanel() {
        this(new BorderLayout());
    }

    public ModernCardPanel(java.awt.LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public ModernCardPanel(int cornerRadius) {
        this(new BorderLayout());
        this.cornerRadius = cornerRadius;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setCustomBg(Color bg) {
        this.customBg = bg;
        repaint();
    }

    public void setCustomBorder(Color border) {
        this.customBorder = border;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Color bg = customBg != null ? customBg : getBackground();
        if (bg == null) {
            ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
            bg = colors.cardBg;
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        Color border = customBorder;
        if (border == null) {
            ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
            border = colors.border;
        }

        g2.setColor(border);
        g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);
    }
}
