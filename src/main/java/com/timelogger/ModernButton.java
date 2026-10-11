package com.timelogger;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern button styled with Windows 11 Fluent Design principles.
 * Supports button styles: STANDARD, PRIMARY, SUCCESS, DANGER, OUTLINE.
 * Uses pill/rounded corners (default 12px or fully rounded for actions),
 * Segoe UI font, smooth hover highlights, and subtle focus states.
 */
public class ModernButton extends JButton {

    public enum ButtonStyle {
        STANDARD,
        PRIMARY,
        SUCCESS,
        DANGER,
        OUTLINE
    }

    private boolean isHovered = false;
    private boolean isPressed = false;
    private Color borderColor = null;
    private int roundRadius = 10;
    private ButtonStyle buttonStyle = ButtonStyle.STANDARD;

    public ModernButton(String text) {
        this(text, ButtonStyle.STANDARD);
    }

    public ModernButton(String text, ButtonStyle style) {
        super(text);
        this.buttonStyle = style;
        setFont(new Font("Segoe UI", Font.BOLD, 12));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setMargin(new Insets(6, 14, 6, 14));
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

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
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = false;
                    repaint();
                }
            }
        });
    }

    public void setButtonStyle(ButtonStyle style) {
        this.buttonStyle = style;
        repaint();
    }

    public ButtonStyle getButtonStyle() {
        return buttonStyle;
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    public void setRoundRadius(int radius) {
        this.roundRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
        boolean isDark = (ThemeManager.loadTheme() == ThemeManager.AppTheme.DARK 
                       || ThemeManager.loadTheme() == ThemeManager.AppTheme.HIGH_CONTRAST);

        Color bg;
        Color fg;
        Color border;

        switch (buttonStyle) {
            case PRIMARY:
                bg = isDark ? new Color(0, 120, 212) : new Color(0, 103, 198);
                fg = Color.WHITE;
                border = bg;
                break;
            case SUCCESS:
                bg = isDark ? new Color(16, 137, 62) : new Color(16, 124, 65);
                fg = Color.WHITE;
                border = bg;
                break;
            case DANGER:
                bg = isDark ? new Color(196, 43, 28) : new Color(209, 52, 56);
                fg = Color.WHITE;
                border = bg;
                break;
            case OUTLINE:
                bg = new Color(0, 0, 0, 0);
                fg = colors.text;
                border = colors.border;
                break;
            case STANDARD:
            default:
                bg = getBackground() != null ? getBackground() : colors.buttonBg;
                fg = getForeground() != null ? getForeground() : colors.buttonFg;
                border = borderColor != null ? borderColor : colors.border;
                break;
        }

        // Adjust for disabled, hover, pressed
        if (!isEnabled()) {
            bg = isDark ? new Color(50, 50, 50) : new Color(230, 230, 230);
            fg = isDark ? new Color(120, 120, 120) : new Color(160, 160, 160);
            border = isDark ? new Color(60, 60, 60) : new Color(210, 210, 210);
        } else if (isPressed) {
            bg = darkenColor(bg, 0.15f);
        } else if (isHovered) {
            bg = lightenColor(bg, 0.10f);
        }

        // Draw pill background
        if (bg.getAlpha() > 0) {
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, width, height, roundRadius, roundRadius);
        }

        // Draw border
        if (border != null && border.getAlpha() > 0) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, width - 1, height - 1, roundRadius, roundRadius);
        }

        // Set text foreground before delegating to paintComponent for text/icon
        setForeground(fg);

        g2.dispose();
        super.paintComponent(g);
    }

    private Color darkenColor(Color c, float factor) {
        return new Color(
            Math.max((int) (c.getRed() * (1 - factor)), 0),
            Math.max((int) (c.getGreen() * (1 - factor)), 0),
            Math.max((int) (c.getBlue() * (1 - factor)), 0),
            c.getAlpha()
        );
    }

    private Color lightenColor(Color c, float factor) {
        return new Color(
            Math.min((int) (c.getRed() + (255 - c.getRed()) * factor), 255),
            Math.min((int) (c.getGreen() + (255 - c.getGreen()) * factor), 255),
            Math.min((int) (c.getBlue() + (255 - c.getBlue()) * factor), 255),
            c.getAlpha()
        );
    }
}
