package com.timelogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ThemeManager {
    public enum AppTheme {
        LIGHT,
        DARK,
        HIGH_CONTRAST
    }

    public static class ThemeColors {
        public final Color bg;
        public final Color cardBg;
        public final Color text;
        public final Color buttonBg;
        public final Color buttonFg;
        public final Color border;
        public final Color accent;

        public ThemeColors(Color bg, Color cardBg, Color text, Color buttonBg, Color buttonFg, Color border, Color accent) {
            this.bg = bg;
            this.cardBg = cardBg;
            this.text = text;
            this.buttonBg = buttonBg;
            this.buttonFg = buttonFg;
            this.border = border;
            this.accent = accent;
        }
    }

    public static final ThemeColors LIGHT_COLORS = new ThemeColors(
        new Color(248, 249, 250), // Soft gray-white
        new Color(255, 255, 255), // Pure white
        new Color(33, 37, 41),    // Dark slate gray
        new Color(233, 236, 239), // Light slate button
        new Color(33, 37, 41),
        new Color(222, 226, 230), // Border
        new Color(13, 110, 253)   // Accent royal blue
    );

    public static final ThemeColors DARK_COLORS = new ThemeColors(
        new Color(30, 30, 36),    // Deep charcoal
        new Color(45, 45, 52),    // Sleek slate card
        new Color(240, 240, 245), // Cool white text
        new Color(58, 58, 66),    // Slate button
        new Color(240, 240, 245),
        new Color(68, 68, 76),    // Border
        new Color(88, 166, 255)   // Accent neon blue
    );

    public static final ThemeColors HIGH_CONTRAST_COLORS = new ThemeColors(
        new Color(0, 0, 0),       // Black
        new Color(0, 0, 0),       // Black
        new Color(255, 255, 255), // White
        new Color(0, 0, 0),       // Black
        new Color(255, 255, 255), // White
        new Color(255, 255, 255), // White border
        new Color(255, 255, 0)    // Accent yellow
    );

    public static ThemeColors getColors(AppTheme theme) {
        switch (theme) {
            case DARK: return DARK_COLORS;
            case HIGH_CONTRAST: return HIGH_CONTRAST_COLORS;
            default: return LIGHT_COLORS;
        }
    }

    public static AppTheme loadTheme() {
        Path path = Paths.get(System.getProperty("user.dir"), "theme.txt");
        try {
            if (Files.exists(path)) {
                String val = Files.readString(path, StandardCharsets.UTF_8).trim();
                return AppTheme.valueOf(val);
            }
        } catch (Exception ignored) {
        }
        return AppTheme.LIGHT; // Default to Light
    }

    public static void saveTheme(AppTheme theme) {
        Path path = Paths.get(System.getProperty("user.dir"), "theme.txt");
        try {
            Files.writeString(path, theme.name(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    public static void applyTheme(Component c, ThemeColors colors) {
        if (c == null) return;

        if (c instanceof JFrame) {
            // Set global UIManager defaults so dialogs, scrollbars, text fields, and combo boxes align.
            javax.swing.UIManager.put("Panel.background", colors.bg);
            javax.swing.UIManager.put("OptionPane.background", colors.bg);
            javax.swing.UIManager.put("OptionPane.messageForeground", colors.text);
            javax.swing.UIManager.put("Label.foreground", colors.text);
            javax.swing.UIManager.put("Label.background", colors.bg);
            javax.swing.UIManager.put("TextField.background", colors.cardBg);
            javax.swing.UIManager.put("TextField.foreground", colors.text);
            javax.swing.UIManager.put("TextField.caretForeground", colors.text);
            javax.swing.UIManager.put("FormattedTextField.background", colors.cardBg);
            javax.swing.UIManager.put("FormattedTextField.foreground", colors.text);
            javax.swing.UIManager.put("FormattedTextField.caretForeground", colors.text);
            javax.swing.UIManager.put("ComboBox.background", colors.cardBg);
            javax.swing.UIManager.put("ComboBox.foreground", colors.text);
            javax.swing.UIManager.put("List.background", colors.cardBg);
            javax.swing.UIManager.put("List.foreground", colors.text);
            javax.swing.UIManager.put("List.selectionBackground", colors.accent);
            javax.swing.UIManager.put("List.selectionForeground", colors.bg);
            javax.swing.UIManager.put("Table.background", colors.cardBg);
            javax.swing.UIManager.put("Table.foreground", colors.text);
            javax.swing.UIManager.put("Table.gridColor", colors.border);
            javax.swing.UIManager.put("TableHeader.background", colors.bg);
            javax.swing.UIManager.put("TableHeader.foreground", colors.text);
            javax.swing.UIManager.put("TabbedPane.background", colors.bg);
            javax.swing.UIManager.put("TabbedPane.foreground", colors.text);
            javax.swing.UIManager.put("TabbedPane.selected", colors.cardBg);
            javax.swing.UIManager.put("Spinner.background", colors.cardBg);
            javax.swing.UIManager.put("Spinner.foreground", colors.text);
            javax.swing.UIManager.put("Button.background", colors.buttonBg);
            javax.swing.UIManager.put("Button.foreground", colors.buttonFg);
            javax.swing.UIManager.put("ScrollBar.background", colors.bg);
            javax.swing.UIManager.put("ScrollBar.foreground", colors.cardBg);
            javax.swing.UIManager.put("ScrollBar.thumb", colors.border);
            javax.swing.UIManager.put("ScrollBar.track", colors.bg);
        }

        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;

            // Handle titled borders
            if (jc.getBorder() instanceof TitledBorder) {
                TitledBorder tb = (TitledBorder) jc.getBorder();
                tb.setTitleColor(colors.text);
                tb.setBorder(BorderFactory.createLineBorder(colors.border, 1));
            }

            // Exclude PieChartPanel from background/foreground overrides
            if (jc instanceof PieChartPanel) {
                PieChartPanel pcp = (PieChartPanel) jc;
                pcp.setBackground(colors.cardBg);
                pcp.setTextColor(colors.text);
            } else if (jc instanceof ModernButton) {
                ModernButton mb = (ModernButton) jc;
                mb.setBackground(colors.buttonBg);
                mb.setForeground(colors.buttonFg);
                mb.setBorderColor(colors.border);
            } else if (jc instanceof JButton) {
                jc.setBackground(colors.buttonBg);
                jc.setForeground(colors.buttonFg);
                jc.setBorder(BorderFactory.createLineBorder(colors.border, 1));
            } else if (jc instanceof JTable) {
                JTable table = (JTable) jc;
                table.setBackground(colors.cardBg);
                table.setForeground(colors.text);
                table.setGridColor(colors.border);
                table.getTableHeader().setBackground(colors.bg);
                table.getTableHeader().setForeground(colors.text);
                table.getTableHeader().setBorder(BorderFactory.createLineBorder(colors.border, 1));
            } else if (jc instanceof JComboBox) {
                JComboBox<?> combo = (JComboBox<?>) jc;
                combo.setBackground(colors.cardBg);
                combo.setForeground(colors.text);
                combo.setBorder(BorderFactory.createLineBorder(colors.border, 1));
                combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        if (comp instanceof JComponent) {
                            JComponent jcomp = (JComponent) comp;
                            if (isSelected) {
                                jcomp.setBackground(colors.accent);
                                jcomp.setForeground(colors.bg);
                            } else {
                                jcomp.setBackground(colors.cardBg);
                                jcomp.setForeground(colors.text);
                            }
                        }
                        return comp;
                    }
                });
            } else if (jc instanceof javax.swing.text.JTextComponent) {
                javax.swing.text.JTextComponent tc = (javax.swing.text.JTextComponent) jc;
                tc.setBackground(colors.cardBg);
                tc.setForeground(colors.text);
                tc.setCaretColor(colors.text);
                tc.setBorder(BorderFactory.createLineBorder(colors.border, 1));
            } else if (jc instanceof JSpinner) {
                JSpinner spinner = (JSpinner) jc;
                spinner.setBackground(colors.cardBg);
                spinner.setForeground(colors.text);
                spinner.setBorder(BorderFactory.createLineBorder(colors.border, 1));
                JComponent editor = spinner.getEditor();
                applyTheme(editor, colors);
            } else if (jc instanceof JScrollPane) {
                jc.setBackground(colors.bg);
                jc.setBorder(BorderFactory.createLineBorder(colors.border, 1));
                JScrollPane sp = (JScrollPane) jc;
                sp.getViewport().setBackground(colors.bg);
                applyTheme(sp.getVerticalScrollBar(), colors);
                applyTheme(sp.getHorizontalScrollBar(), colors);
            } else if (jc instanceof JTabbedPane) {
                JTabbedPane tp = (JTabbedPane) jc;
                tp.setBackground(colors.bg);
                tp.setForeground(colors.text);
            } else if (jc instanceof JProgressBar) {
                JProgressBar pb = (JProgressBar) jc;
                pb.setBackground(colors.cardBg);
                pb.setForeground(colors.accent);
                pb.setBorder(BorderFactory.createLineBorder(colors.border, 1));
            } else if (jc instanceof JList) {
                JList<?> list = (JList<?>) jc;
                list.setBackground(colors.cardBg);
                list.setForeground(colors.text);
                list.setSelectionBackground(colors.accent);
                list.setSelectionForeground(colors.bg);
            } else {
                // Generic JComponent styling
                if (jc.isOpaque()) {
                    if (isInsideSummaryPanel(jc)) {
                        jc.setBackground(colors.cardBg);
                    } else {
                        jc.setBackground(colors.bg);
                    }
                }
                jc.setForeground(colors.text);
            }
        } else {
            // Non-JComponent fallback
            c.setBackground(colors.bg);
            c.setForeground(colors.text);
        }

        // Recurse down container tree
        if (c instanceof Container) {
            Container container = (Container) c;
            for (Component child : container.getComponents()) {
                applyTheme(child, colors);
            }
        }
    }

    private static boolean isInsideSummaryPanel(Component c) {
        Container p = c.getParent();
        while (p != null) {
            if ("summaryPanel".equals(p.getName())) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }
}
