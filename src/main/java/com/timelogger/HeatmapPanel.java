package com.timelogger;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HeatmapPanel extends JPanel {
    private Map<LocalDate, Long> dailyDurations = new HashMap<>();

    public HeatmapPanel() {
        setPreferredSize(new Dimension(800, 180));
        setToolTipText(""); // Enables tooltip registration in Swing

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                updateTooltip(e.getX(), e.getY());
            }
        });
    }

    public void setData(Map<LocalDate, Long> dailyDurations) {
        this.dailyDurations = dailyDurations != null ? dailyDurations : new HashMap<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ThemeManager.AppTheme currentTheme = ThemeManager.loadTheme();
        ThemeManager.ThemeColors colors = ThemeManager.getColors(currentTheme);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background
        g2.setColor(colors.cardBg);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int leftMargin = 35;
        int topMargin = 25;
        int squareSize = 16;
        int gap = 4;
        int cellSpace = squareSize + gap;

        LocalDate today = LocalDate.now();
        int availableWidth = getWidth() - leftMargin - 20;
        int numWeeks = availableWidth / cellSpace;
        if (numWeeks < 9) {
            numWeeks = 9;
        }

        LocalDate startDate = today.minusDays((numWeeks - 1) * 7).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        // 1. Draw Month Labels
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String lastMonthName = "";
        for (int col = 0; col < numWeeks; col++) {
            LocalDate colStartDate = startDate.plusDays(col * 7);
            String monthName = colStartDate.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault());
            if (!monthName.equals(lastMonthName)) {
                int x = leftMargin + col * cellSpace;
                g2.setColor(colors.text);
                g2.drawString(monthName, x, topMargin - 8);
                lastMonthName = monthName;
            }
        }

        // 2. Draw Day Labels (Mon, Wed, Fri)
        String[] dayLabels = {"", "Mon", "", "Wed", "", "Fri", ""};
        for (int row = 0; row < 7; row++) {
            if (!dayLabels[row].isEmpty()) {
                int y = topMargin + row * cellSpace + squareSize - 4;
                g2.setColor(colors.text);
                g2.drawString(dayLabels[row], leftMargin - 30, y);
            }
        }

        // 3. Draw Squares
        for (int col = 0; col < numWeeks; col++) {
            for (int row = 0; row < 7; row++) {
                LocalDate cellDate = startDate.plusDays(col * 7 + row);
                if (cellDate.isAfter(today)) {
                    continue; // Future days are not drawn
                }

                int x = leftMargin + col * cellSpace;
                int y = topMargin + row * cellSpace;

                long seconds = dailyDurations.getOrDefault(cellDate, 0L);
                int level = getDurationLevel(seconds);
                g2.setColor(getLevelColor(level, colors));
                g2.fillRoundRect(x, y, squareSize, squareSize, 4, 4);
            }
        }

        // 4. Draw Legend (aligned to the right of the grid)
        int legendY = topMargin + 7 * cellSpace + 12;
        int legendXStart = leftMargin + numWeeks * cellSpace - 110;
        
        g2.setColor(colors.text);
        g2.drawString("Less", legendXStart - 30, legendY + 11);
        for (int lvl = 0; lvl < 5; lvl++) {
            g2.setColor(getLevelColor(lvl, colors));
            g2.fillRoundRect(legendXStart + lvl * (12 + 3), legendY, 12, 12, 3, 3);
        }
        g2.setColor(colors.text);
        g2.drawString("More", legendXStart + 5 * (12 + 3) + 5, legendY + 11);
    }

    private int getDurationLevel(long seconds) {
        if (seconds <= 0) return 0;
        if (seconds <= 3600) return 1;       // <= 1 hour
        if (seconds <= 10800) return 2;      // <= 3 hours
        if (seconds <= 21600) return 3;      // <= 6 hours
        return 4;                            // > 6 hours
    }

    private Color getLevelColor(int level, ThemeManager.ThemeColors colors) {
        Color base = colors.accent;
        switch (level) {
            case 0:
                return new Color(colors.text.getRed(), colors.text.getGreen(), colors.text.getBlue(), 25);
            case 1:
                return new Color(base.getRed(), base.getGreen(), base.getBlue(), 60);
            case 2:
                return new Color(base.getRed(), base.getGreen(), base.getBlue(), 120);
            case 3:
                return new Color(base.getRed(), base.getGreen(), base.getBlue(), 180);
            default:
                return base; // Full accent color
        }
    }

    private void updateTooltip(int mouseX, int mouseY) {
        ThemeManager.AppTheme currentTheme = ThemeManager.loadTheme();
        ThemeManager.ThemeColors colors = ThemeManager.getColors(currentTheme);

        int leftMargin = 35;
        int topMargin = 25;
        int squareSize = 16;
        int gap = 4;
        int cellSpace = squareSize + gap;

        LocalDate today = LocalDate.now();
        int availableWidth = getWidth() - leftMargin - 20;
        int numWeeks = availableWidth / cellSpace;
        if (numWeeks < 9) {
            numWeeks = 9;
        }

        LocalDate startDate = today.minusDays((numWeeks - 1) * 7).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        for (int col = 0; col < numWeeks; col++) {
            for (int row = 0; row < 7; row++) {
                int x = leftMargin + col * cellSpace;
                int y = topMargin + row * cellSpace;

                if (mouseX >= x && mouseX < x + squareSize && mouseY >= y && mouseY < y + squareSize) {
                    LocalDate cellDate = startDate.plusDays(col * 7 + row);
                    if (!cellDate.isAfter(today)) {
                        long seconds = dailyDurations.getOrDefault(cellDate, 0L);
                        String formattedDate = cellDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"));
                        String formattedDuration = formatDuration(seconds);
                        setToolTipText(formattedDate + ": " + formattedDuration + " tracked");
                        return;
                    }
                }
            }
        }
        setToolTipText(null);
    }

    private String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
