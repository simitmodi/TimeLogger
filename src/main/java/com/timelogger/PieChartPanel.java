package com.timelogger;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PieChartPanel extends JPanel {
    private final List<String> labels = new ArrayList<>();
    private final List<Long> values = new ArrayList<>();
    private final List<Color> colors = new ArrayList<>();

    private static final Color[] PALETTE = {
        new Color(79, 110, 242),   // Blue
        new Color(235, 87, 87),    // Red
        new Color(46, 204, 113),   // Green
        new Color(241, 196, 15),   // Yellow
        new Color(155, 89, 182),   // Purple
        new Color(230, 126, 34),   // Orange
        new Color(26, 188, 156),   // Teal
        new Color(52, 73, 94)      // Dark Slate
    };

    public PieChartPanel() {
        setPreferredSize(new Dimension(280, 140));
        setMinimumSize(new Dimension(150, 100));
        setBackground(Color.WHITE);
    }

    public void setData(Map<String, Long> data) {
        labels.clear();
        values.clear();
        colors.clear();

        int colorIndex = 0;
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            if (entry.getValue() > 0) {
                labels.add(entry.getKey());
                values.add(entry.getValue());
                colors.add(PALETTE[colorIndex % PALETTE.length]);
                colorIndex++;
            }
        }
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (values.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String msg = "No tracking data available";
            int msgW = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (width - msgW) / 2, height / 2);
            return;
        }

        long total = 0;
        for (long val : values) {
            total += val;
        }

        // Draw Pie (Left side)
        int padding = 15;
        int size = Math.min(width / 2 - padding, height - padding * 2);
        if (size < 10) size = 10;
        
        int x = padding + (width / 2 - padding - size) / 2;
        int y = padding + (height - padding * 2 - size) / 2;

        int startAngle = 0;
        for (int i = 0; i < values.size(); i++) {
            int arcAngle = (int) Math.round((double) values.get(i) * 360.0 / total);
            if (i == values.size() - 1) {
                arcAngle = 360 - startAngle;
            }

            g2.setColor(colors.get(i));
            g2.fillArc(x, y, size, size, startAngle, arcAngle);
            startAngle += arcAngle;
        }

        // Draw Legend (Right side)
        int legendX = width / 2 + 10;
        int legendY = padding + 10;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));

        for (int i = 0; i < labels.size(); i++) {
            if (legendY + 16 > height) {
                break; // Out of bounds
            }
            g2.setColor(colors.get(i));
            g2.fillRect(legendX, legendY, 10, 10);

            g2.setColor(Color.DARK_GRAY);
            String percentLabel = String.format("%s (%.0f%%)", labels.get(i), (double) values.get(i) * 100.0 / total);
            g2.drawString(percentLabel, legendX + 16, legendY + 9);
            legendY += 18;
        }
    }
}
