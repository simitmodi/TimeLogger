package com.timelogger;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {
    private boolean isHovered = false;
    private Color borderColor = Color.LIGHT_GRAY;
    private static final int ROUND_RADIUS = 8;

    public ModernButton(String text) {
        super(text);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Calculate background color based on state
        Color bg = getBackground();
        if (!isEnabled()) {
            bg = bg.darker();
        } else if (isHovered) {
            int r = bg.getRed();
            int gr = bg.getGreen();
            int b = bg.getBlue();
            if (r + gr + b > 382) { // Light theme bg -> darken slightly on hover
                bg = new Color(Math.max(0, r - 15), Math.max(0, gr - 15), Math.max(0, b - 15));
            } else { // Dark theme bg -> brighten slightly on hover
                bg = new Color(Math.min(255, r + 20), Math.min(255, gr + 20), Math.min(255, b + 20));
            }
        }

        // Draw background
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, width, height, ROUND_RADIUS, ROUND_RADIUS);

        // Draw border
        g2.setColor(borderColor);
        g2.drawRoundRect(0, 0, width - 1, height - 1, ROUND_RADIUS, ROUND_RADIUS);

        g2.dispose();

        // Let JButton paint text/icon
        super.paintComponent(g);
    }
}
