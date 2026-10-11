package com.timelogger;

import com.timelogger.web.WebServer;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

public class Main {
    private static ServerSocket lockSocket;
    private static final int PORT = 54321;
    private static WebServer webServerInstance;
    private static TrayIcon browserTrayIcon;
    private static volatile String activeMode = "DESKTOP"; // "DESKTOP" or "BROWSER"

    public static void main(String[] args) {
        // Try to acquire single-instance lock on local port 54321
        try {
            lockSocket = new ServerSocket(PORT, 10, InetAddress.getByName("127.0.0.1"));
            startLockListener();
        } catch (Exception e) {
            // Port already in use, meaning an instance is already running.
            // Connect to it, send a restore signal, and exit.
            try (Socket socket = new Socket("127.0.0.1", PORT)) {
                OutputStream os = socket.getOutputStream();
                os.write("RESTORE\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                os.flush();
            } catch (Exception ignored) {
            }
            System.exit(0);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            StorageService storage = new StorageService();
            String preferredMode = storage.loadPreferredLaunchMode();

            if ("BROWSER".equalsIgnoreCase(preferredMode)) {
                launchBrowserMode();
            } else if ("DESKTOP".equalsIgnoreCase(preferredMode)) {
                launchDesktopMode();
            } else {
                showStartupModeDialog(storage);
            }
        });
    }

    private static void showStartupModeDialog(StorageService storage) {
        // Custom Styled Startup Selection Dialog
        JDialog dialog = new JDialog((Frame) null, "Time Logger - Launch Mode", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(440, 240);
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel titleLabel = new JLabel("<html><h3 style='margin:0;'>Welcome to Time Logger</h3><p style='color:gray;margin-top:4px;'>How would you like to open TimeLogger today?</p></html>");
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));

        JButton desktopBtn = new JButton("🖥️  Open Desktop App (.exe)");
        desktopBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        desktopBtn.setFocusPainted(false);
        desktopBtn.setPreferredSize(new Dimension(380, 42));

        JButton browserBtn = new JButton("🌐  Open in Web Browser");
        browserBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        browserBtn.setFocusPainted(false);
        browserBtn.setPreferredSize(new Dimension(380, 42));

        centerPanel.add(desktopBtn);
        centerPanel.add(browserBtn);
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        JCheckBox rememberCheck = new JCheckBox("Remember my choice (can be changed in Settings)");
        rememberCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
        bottomPanel.add(rememberCheck);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        final boolean[] launched = {false};

        desktopBtn.addActionListener(e -> {
            if (rememberCheck.isSelected()) {
                storage.savePreferredLaunchMode("DESKTOP");
            }
            launched[0] = true;
            dialog.dispose();
            launchDesktopMode();
        });

        browserBtn.addActionListener(e -> {
            if (rememberCheck.isSelected()) {
                storage.savePreferredLaunchMode("BROWSER");
            }
            launched[0] = true;
            dialog.dispose();
            launchBrowserMode();
        });

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (!launched[0]) {
                    System.exit(0);
                }
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    public static void launchDesktopMode() {
        activeMode = "DESKTOP";
        if (webServerInstance != null) {
            webServerInstance.stop();
            webServerInstance = null;
        }
        cleanupBrowserTray();

        try {
            if (lockSocket == null || lockSocket.isClosed()) {
                lockSocket = new ServerSocket(PORT, 10, InetAddress.getByName("127.0.0.1"));
                startLockListener();
            }
        } catch (Exception ignored) {}

        AppFrame frame = new AppFrame();
        frame.setVisible(true);
        AppFrameHolder.setInstance(frame);
        frame.checkAndRecoverInterruptedSession();

        // Trigger GC shortly after UI rendering
        javax.swing.Timer gcTimer = new javax.swing.Timer(1000, e -> System.gc());
        gcTimer.setRepeats(false);
        gcTimer.start();
    }

    public static void launchBrowserMode() {
        activeMode = "BROWSER";
        AppFrame currentFrame = AppFrameHolder.getInstance();
        if (currentFrame != null) {
            currentFrame.dispose();
            AppFrameHolder.setInstance(null);
        }

        // Close existing single instance socket to release port 54321 for the WebServer
        try {
            if (lockSocket != null && !lockSocket.isClosed()) {
                lockSocket.close();
            }
        } catch (Exception ignored) {}

        StorageService storageService = new StorageService();
        ExportService exportService = new ExportService();

        try {
            webServerInstance = new WebServer(PORT, storageService, exportService,
                () -> SwingUtilities.invokeLater(Main::launchDesktopMode),
                () -> handleBrowserModeExit(storageService)
            );
            webServerInstance.start();

            // Open browser
            openBrowser("http://localhost:" + PORT);

            // Setup tray icon for browser mode
            initBrowserTray(storageService);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Failed to start Browser mode on port " + PORT + ":\n" + e.getMessage() + "\nFalling back to Desktop App.",
                "Error", JOptionPane.ERROR_MESSAGE);
            launchDesktopMode();
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initBrowserTray(StorageService storageService) {
        if (!SystemTray.isSupported()) return;
        cleanupBrowserTray();

        try {
            Image image = null;
            java.io.InputStream imgStream = Main.class.getResourceAsStream("/com/timelogger/icon.png");
            if (imgStream != null) {
                image = javax.imageio.ImageIO.read(imgStream);
            }
            if (image == null) {
                // Fallback icon
                int size = 16;
                java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = bi.createGraphics();
                g.setColor(new Color(13, 110, 253));
                g.fillOval(1, 1, 14, 14);
                g.dispose();
                image = bi;
            }

            browserTrayIcon = new TrayIcon(image, "Time Logger (Web Mode)");
            browserTrayIcon.setImageAutoSize(true);
            browserTrayIcon.addActionListener(e -> openBrowser("http://localhost:" + PORT));

            PopupMenu popup = new PopupMenu();
            MenuItem openBrowserItem = new MenuItem("Open in Browser");
            openBrowserItem.addActionListener(e -> openBrowser("http://localhost:" + PORT));
            popup.add(openBrowserItem);

            MenuItem switchDesktopItem = new MenuItem("Switch to Desktop App");
            switchDesktopItem.addActionListener(e -> SwingUtilities.invokeLater(Main::launchDesktopMode));
            popup.add(switchDesktopItem);

            popup.addSeparator();

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> handleBrowserModeExit(storageService));
            popup.add(exitItem);

            browserTrayIcon.setPopupMenu(popup);
            SystemTray.getSystemTray().add(browserTrayIcon);
        } catch (Exception ignored) {}
    }

    private static void cleanupBrowserTray() {
        if (SystemTray.isSupported() && browserTrayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(browserTrayIcon);
                browserTrayIcon = null;
            } catch (Exception ignored) {}
        }
    }

    private static void handleBrowserModeExit(StorageService storageService) {
        if (storageService.hasActiveSessionState()) {
            ActiveSessionState state = storageService.loadActiveSessionState();
            String sub = state != null ? state.getSubject() : "Study Session";
            int choice = JOptionPane.showConfirmDialog(
                null,
                "A study session is currently in progress (" + sub + ").\nDo you really want to close TimeLogger?\n(Your session will be safely preserved for recovery on next launch)",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        } else {
            int choice = JOptionPane.showConfirmDialog(
                null,
                "Do you really want to close TimeLogger?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        cleanupBrowserTray();
        if (webServerInstance != null) {
            webServerInstance.stop();
        }
        System.exit(0);
    }

    private static void startLockListener() {
        Thread thread = new Thread(() -> {
            while (lockSocket != null && !lockSocket.isClosed()) {
                try (Socket client = lockSocket.accept()) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(client.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                    String line = reader.readLine();
                    if ("RESTORE".equals(line)) {
                        SwingUtilities.invokeLater(() -> {
                            if ("BROWSER".equals(activeMode)) {
                                openBrowser("http://localhost:" + PORT);
                            } else {
                                AppFrame frame = AppFrameHolder.getInstance();
                                if (frame != null) {
                                    frame.restoreFromTray();
                                }
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}

class AppFrameHolder {
    private static AppFrame instance;
    public static synchronized void setInstance(AppFrame inst) { instance = inst; }
    public static synchronized AppFrame getInstance() { return instance; }
}
