package com.timelogger;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Windows 11 Fluent style TabbedPaneUI with pill / indicator tabs,
 * smooth Segoe UI typography, and subtle hover/active highlights.
 */
public class FluentTabbedPaneUI extends BasicTabbedPaneUI {
    private Color tabBg = new Color(243, 243, 243);
    private Color tabSelectedBg = new Color(255, 255, 255);
    private Color tabText = new Color(32, 31, 30);
    private Color tabSelectedText = new Color(0, 103, 198);
    private Color accentColor = new Color(0, 103, 198);
    private Color hoverBg = new Color(234, 234, 234);

    public FluentTabbedPaneUI() {
    }

    public void updateColors(ThemeManager.ThemeColors colors, boolean isDark) {
        this.tabBg = colors.bg;
        this.tabSelectedBg = colors.cardBg;
        this.tabText = colors.text;
        this.tabSelectedText = colors.accent;
        this.accentColor = colors.accent;
        if (isDark) {
            this.hoverBg = new Color(44, 44, 44);
        } else {
            this.hoverBg = new Color(235, 235, 235);
        }
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(8, 18, 8, 18);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(6, 0, 0, 0);
    }

    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        return Math.max(38, super.calculateTabHeight(tabPlacement, tabIndex, fontHeight) + 10);
    }

    @Override
    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(tabBg);
        g2.fillRect(0, 0, tabPane.getWidth(), tabPane.getHeight());
        g2.dispose();
        super.paintTabArea(g, tabPlacement, selectedIndex);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padX = 3;
        int padY = 4;
        int pillX = x + padX;
        int pillY = y + padY;
        int pillW = w - (padX * 2);
        int pillH = h - (padY * 2);
        int radius = 8;

        if (isSelected) {
            // Selected pill tab
            g2.setColor(tabSelectedBg);
            g2.fillRoundRect(pillX, pillY, pillW, pillH, radius, radius);

            // Active underline indicator
            g2.setColor(accentColor);
            int barHeight = 3;
            int barMargin = 12;
            int barW = Math.max(16, pillW - (barMargin * 2));
            int barX = pillX + (pillW - barW) / 2;
            int barY = pillY + pillH - barHeight - 1;
            g2.fillRoundRect(barX, barY, barW, barHeight, 2, 2);
        } else {
            // Check hover (if rollover)
            if (getRolloverTab() == tabIndex) {
                g2.setColor(hoverBg);
                g2.fillRoundRect(pillX, pillY, pillW, pillH, radius, radius);
            }
        }

        g2.dispose();
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                             int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font useFont = new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 13);
        g2.setFont(useFont);
        g2.setColor(isSelected ? tabSelectedText : tabText);

        FontMetrics fm = g2.getFontMetrics(useFont);
        int textX = textRect.x + (textRect.width - fm.stringWidth(title)) / 2;
        int textY = textRect.y + ((textRect.height - fm.getHeight()) / 2) + fm.getAscent() - (isSelected ? 1 : 0);

        g2.drawString(title, textX, textY);
        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
        // Fluent tabs do not draw ugly sharp retro tab borders
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // Minimal subtle border separating tab bar from tab content
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(tabBg);
        g2.fillRect(0, 0, tabPane.getWidth(), tabPane.getHeight());
        g2.dispose();
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                       int tabIndex, Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected) {
        // No dotted focus rectangles
    }
}
