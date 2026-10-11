package com.timelogger;

import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Modern rounded border with an optional header title styled after Windows 11 Fluent cards.
 * Avoids retro notched borders and renders clean, antialiased rounded rectangles.
 */
public class FluentCardBorder extends AbstractBorder {
    private String title;
    private int cornerRadius = 12;
    private Color borderColor;
    private Color titleColor;
    private Font titleFont = new Font("Segoe UI", Font.BOLD, 12);
    private int topPadding = 12;
    private int sidePadding = 12;
    private int bottomPadding = 12;

    public FluentCardBorder(String title) {
        this.title = title;
    }

    public FluentCardBorder(String title, int cornerRadius) {
        this.title = title;
        this.cornerRadius = cornerRadius;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setColors(Color border, Color title) {
        this.borderColor = border;
        this.titleColor = title;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        int top = (title != null && !title.isEmpty()) ? 32 : topPadding;
        return new Insets(top, sidePadding, bottomPadding, sidePadding);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets i = getBorderInsets(c);
        insets.top = i.top;
        insets.left = i.left;
        insets.bottom = i.bottom;
        insets.right = i.right;
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
        Color border = borderColor != null ? borderColor : colors.border;
        Color text = titleColor != null ? titleColor : colors.text;

        // Draw card border outline
        g2.setColor(border);
        g2.drawRoundRect(x, y, width - 1, height - 1, cornerRadius, cornerRadius);

        // Draw card header title if present
        if (title != null && !title.isEmpty()) {
            g2.setFont(titleFont);
            g2.setColor(text);
            FontMetrics fm = g2.getFontMetrics(titleFont);
            int textX = x + sidePadding;
            int textY = y + 18;
            g2.drawString(title, textX, textY);

            // Optional subtle divider under title
            Color divider = new Color(border.getRed(), border.getGreen(), border.getBlue(), Math.min(255, (int)(border.getAlpha() * 0.8f)));
            g2.setColor(divider);
            g2.drawLine(x + 1, y + 26, x + width - 2, y + 26);
        }

        g2.dispose();
    }
}
