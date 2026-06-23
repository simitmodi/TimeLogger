package com.timelogger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            AppFrame frame = new AppFrame();
            frame.setVisible(true);

            // Trigger GC shortly after UI rendering to reclaim startup/layout garbage
            javax.swing.Timer gcTimer = new javax.swing.Timer(1000, e -> System.gc());
            gcTimer.setRepeats(false);
            gcTimer.start();
        });
    }
}
