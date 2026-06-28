package com.timelogger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static ServerSocket lockSocket;
    private static final int PORT = 54321;

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

            AppFrame frame = new AppFrame();
            frame.setVisible(true);
            AppFrameHolder.setInstance(frame);

            // Trigger GC shortly after UI rendering to reclaim startup/layout garbage
            javax.swing.Timer gcTimer = new javax.swing.Timer(1000, e -> System.gc());
            gcTimer.setRepeats(false);
            gcTimer.start();
        });
    }

    private static void startLockListener() {
        Thread thread = new Thread(() -> {
            while (true) {
                try (Socket client = lockSocket.accept()) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(client.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                    String line = reader.readLine();
                    if ("RESTORE".equals(line)) {
                        SwingUtilities.invokeLater(() -> {
                            AppFrame frame = AppFrameHolder.getInstance();
                            if (frame != null) {
                                frame.restoreFromTray();
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
