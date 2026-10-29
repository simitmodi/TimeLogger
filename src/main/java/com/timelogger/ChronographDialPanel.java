package com.timelogger;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * High-precision mechanical chronograph dial inspired by Apple Clock's mechanical stopwatch.
 * Features:
 * - 60-second main outer track with distinct 1-second ticks and 1/4-second mechanical sub-ticks
 * - Orange high-contrast sweeping chronograph seconds hand with counterweight and lollipop
 * - 60-minute mechanical inner sub-dial at top-center with sweeping blued/accent precision hand
 * - Brushed metallic dial bezel and subtle inner chapter rings
 * - Modern digital elapsed readout window integrated onto the dial face
 */
public class ChronographDialPanel extends JPanel {
    private long elapsedMillis = 0;
    private Color dialBg = new Color(255, 255, 255);
    private Color dialBorder = new Color(220, 224, 230);
    private Color tickColor = new Color(60, 64, 70);
    private Color subTickColor = new Color(180, 185, 195);
    private Color numberColor = new Color(30, 32, 36);
    private Color chronoHandColor = new Color(242, 100, 25); // Apple stopwatch energetic orange
    private Color subdialHandColor = new Color(0, 122, 255); // Clean precision blue
    private Color digitalWindowBg = new Color(240, 242, 246);
    private Color hourHandColor = new Color(40, 44, 52);
    private Color minuteHandColor = new Color(55, 60, 70);
    private Color handLumeColor = new Color(245, 248, 252);
    private final javax.swing.Timer clockRefreshTimer;

    public ChronographDialPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(800, 800));
        setMinimumSize(new Dimension(400, 400));
        setMaximumSize(new Dimension(800, 800));

        // Periodic timer so current time hands advance even when stopwatch is stopped
        clockRefreshTimer = new javax.swing.Timer(1000, e -> repaint());
        clockRefreshTimer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (clockRefreshTimer != null && clockRefreshTimer.isRunning()) {
            clockRefreshTimer.stop();
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (clockRefreshTimer != null && !clockRefreshTimer.isRunning()) {
            clockRefreshTimer.start();
        }
    }

    public void setElapsedMillis(long millis) {
        if (this.elapsedMillis != millis) {
            this.elapsedMillis = millis;
            repaint();
        }
    }

    public void updateThemeColors(ThemeManager.ThemeColors colors, boolean isDark) {
        if (isDark) {
            this.dialBg = new Color(28, 30, 34);
            this.dialBorder = new Color(60, 64, 72);
            this.tickColor = new Color(230, 235, 245);
            this.subTickColor = new Color(100, 106, 118);
            this.numberColor = new Color(240, 244, 250);
            this.chronoHandColor = new Color(255, 110, 35);
            this.subdialHandColor = new Color(96, 205, 255);
            this.digitalWindowBg = new Color(38, 42, 48);
            this.hourHandColor = new Color(220, 225, 235);
            this.minuteHandColor = new Color(200, 208, 220);
            this.handLumeColor = new Color(35, 40, 48);
        } else {
            this.dialBg = new Color(252, 253, 255);
            this.dialBorder = new Color(216, 220, 228);
            this.tickColor = new Color(45, 48, 55);
            this.subTickColor = new Color(175, 180, 190);
            this.numberColor = new Color(30, 32, 38);
            this.chronoHandColor = new Color(235, 87, 20);
            this.subdialHandColor = new Color(0, 102, 204);
            this.digitalWindowBg = new Color(240, 243, 247);
            this.hourHandColor = new Color(42, 46, 54);
            this.minuteHandColor = new Color(65, 70, 80);
            this.handLumeColor = new Color(248, 250, 255);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h);
        if (size < 40) {
            g2.dispose();
            return;
        }

        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = (size / 2.0) - 8.0;

        // 1. Dial Bezel & Face Background
        // Outer bezel ring
        g2.setColor(dialBorder);
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));

        // Dial face fill
        g2.setColor(dialBg);
        g2.fill(new Ellipse2D.Double(cx - radius + 1, cy - radius + 1, (radius - 1) * 2, (radius - 1) * 2));

        // Subtle concentric inner chapter ring line
        g2.setColor(subTickColor);
        g2.setStroke(new BasicStroke(0.8f));
        double innerTrackRadius = radius * 0.82;
        g2.draw(new Ellipse2D.Double(cx - innerTrackRadius, cy - innerTrackRadius, innerTrackRadius * 2, innerTrackRadius * 2));

        // 2. Outer Dial Markings: 60 Seconds Track
        Font numFont = new Font("Segoe UI", Font.BOLD, (int) Math.max(10, radius * 0.10));
        g2.setFont(numFont);
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6.0 - 90.0);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            boolean isMajor = (i % 5 == 0);
            double outerR = radius - 3;
            double innerR = isMajor ? (radius - radius * 0.10) : (radius - radius * 0.05);

            // Draw 4 sub-ticks between seconds for mechanical chronograph feel
            for (int sub = 1; sub <= 3; sub++) {
                double subAngle = Math.toRadians(i * 6.0 + sub * 1.5 - 90.0);
                double sCos = Math.cos(subAngle);
                double sSin = Math.sin(subAngle);
                double sOuter = radius - 3;
                double sInner = radius - radius * 0.03;
                g2.setColor(subTickColor);
                g2.setStroke(new BasicStroke(0.75f));
                g2.draw(new Line2D.Double(cx + sInner * sCos, cy + sInner * sSin, cx + sOuter * sCos, cy + sOuter * sSin));
            }

            g2.setColor(isMajor ? tickColor : subTickColor);
            g2.setStroke(new BasicStroke(isMajor ? 2.0f : 1.0f));
            g2.draw(new Line2D.Double(cx + innerR * cos, cy + innerR * sin, cx + outerR * cos, cy + outerR * sin));

            // Numbers: 05, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60
            if (isMajor && i != 0) {
                String numStr = String.format("%02d", i);
                double numR = radius * 0.74;
                double tx = cx + numR * cos;
                double ty = cy + numR * sin;
                int textW = fm.stringWidth(numStr);
                int textH = fm.getAscent() - 3;
                g2.setColor(numberColor);
                g2.drawString(numStr, (float) (tx - textW / 2.0), (float) (ty + textH / 2.0));
            } else if (isMajor && i == 0) {
                String numStr = "60";
                double numR = radius * 0.78;
                double tx = cx + numR * cos;
                double ty = cy + numR * sin;
                int textW = fm.stringWidth(numStr);
                int textH = fm.getAscent() - 3;
                g2.setColor(numberColor);
                g2.drawString(numStr, (float) (tx - textW / 2.0), (float) (ty + textH / 2.0));
            }
        }

        // 3. Top Sub-Dial: 60-Minute Counter
        double subR = radius * 0.28;
        double subCy = cy - radius * 0.32;
        draw60MinuteSubdial(g2, cx, subCy, subR);

        // 4. Digital Elapsed Readout Box below center
        drawDigitalWindow(g2, cx, cy + radius * 0.44, radius * 0.65, radius * 0.20);

        // 5. Real-Time Clock Hands (Hour & Minute hands beneath the chronograph needle)
        drawCurrentTimeHands(g2, cx, cy, radius);

        // 6. Main Chronograph Sweeping Seconds Hand (Top layer)
        drawChronoSecondsHand(g2, cx, cy, radius * 0.94);

        // Center pinion cap / jewel
        g2.setColor(numberColor);
        g2.fill(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10));
        g2.setColor(chronoHandColor);
        g2.fill(new Ellipse2D.Double(cx - 3, cy - 3, 6, 6));

        g2.dispose();
    }

    private void drawCurrentTimeHands(Graphics2D g2, double cx, double cy, double dialRadius) {
        java.time.LocalTime now = java.time.LocalTime.now();
        int hour = now.getHour() % 12;
        int minute = now.getMinute();
        int second = now.getSecond();

        // Exact fractional angles for realistic watch mechanical motion
        double hourAngleRad = Math.toRadians(((hour + minute / 60.0 + second / 3600.0) / 12.0) * 360.0 - 90.0);
        double minuteAngleRad = Math.toRadians(((minute + second / 60.0) / 60.0) * 360.0 - 90.0);

        // A. Real-time Hour Hand (Baton style with lume strip)
        double hourHandLen = dialRadius * 0.52;
        double hourHandHalfW = Math.max(2.2, dialRadius * 0.024);
        drawBatonHand(g2, cx, cy, hourAngleRad, hourHandLen, dialRadius * 0.12, hourHandHalfW, hourHandColor, handLumeColor);

        // B. Real-time Minute Hand (Longer, slender baton extending towards outer track)
        double minHandLen = dialRadius * 0.76;
        double minHandHalfW = Math.max(1.8, dialRadius * 0.018);
        drawBatonHand(g2, cx, cy, minuteAngleRad, minHandLen, dialRadius * 0.12, minHandHalfW, minuteHandColor, handLumeColor);
    }

    private void drawBatonHand(Graphics2D g2, double cx, double cy, double angleRad, double length, double tailLen, double halfW, Color bodyColor, Color lumeColor) {
        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(angleRad + Math.PI / 2.0); // 0 degrees pointing up (-Y direction)

        // Hand polygon: tapered tip with counterweight tail
        java.awt.geom.Path2D.Double handPath = new java.awt.geom.Path2D.Double();
        handPath.moveTo(-halfW, tailLen);
        handPath.lineTo(halfW, tailLen);
        handPath.lineTo(halfW * 0.9, -length + halfW * 2.0);
        handPath.lineTo(0, -length);
        handPath.lineTo(-halfW * 0.9, -length + halfW * 2.0);
        handPath.closePath();

        // Drop shadow for depth under the hands
        g2.setColor(new Color(0, 0, 0, 35));
        AffineTransform shadowT = new AffineTransform();
        shadowT.translate(1.2, 1.8);
        java.awt.Shape shadowShape = shadowT.createTransformedShape(handPath);
        g2.fill(shadowShape);

        // Hand body
        g2.setColor(bodyColor);
        g2.fill(handPath);
        g2.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 120));
        g2.setStroke(new BasicStroke(0.8f));
        g2.draw(handPath);

        // Luminous / contrasting central inlay strip
        double lumeW = Math.max(1.0, halfW * 0.45);
        double lumeStart = -tailLen * 0.2;
        double lumeEnd = -length + halfW * 3.0;
        if (lumeEnd < lumeStart) {
            java.awt.geom.RoundRectangle2D.Double lumeStrip = new java.awt.geom.RoundRectangle2D.Double(
                -lumeW / 2.0, lumeEnd, lumeW, (lumeStart - lumeEnd), 1.5, 1.5
            );
            g2.setColor(lumeColor);
            g2.fill(lumeStrip);
        }

        g2.setTransform(old);
    }

    private void draw60MinuteSubdial(Graphics2D g2, double scx, double scy, double sradius) {
        // Sub-dial circular background & boundary
        g2.setColor(dialBorder);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new Ellipse2D.Double(scx - sradius, scy - sradius, sradius * 2, sradius * 2));

        // Subdial ticks (60 minutes total, tick every 1 min, major every 5 or 10 min)
        Font subFont = new Font("Segoe UI", Font.BOLD, (int) Math.max(7, sradius * 0.22));
        g2.setFont(subFont);
        FontMetrics fm = g2.getFontMetrics();

        for (int m = 0; m < 60; m++) {
            double angle = Math.toRadians(m * 6.0 - 90.0);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            boolean isMajor10 = (m % 10 == 0);
            boolean isMedium5 = (m % 5 == 0);

            double outerR = sradius - 1;
            double innerR;
            if (isMajor10) {
                innerR = sradius - sradius * 0.26;
            } else if (isMedium5) {
                innerR = sradius - sradius * 0.18;
            } else {
                innerR = sradius - sradius * 0.11;
            }

            g2.setColor(isMedium5 ? tickColor : subTickColor);
            g2.setStroke(new BasicStroke(isMajor10 ? 1.4f : (isMedium5 ? 1.0f : 0.6f)));
            g2.draw(new Line2D.Double(scx + innerR * cos, scy + innerR * sin, scx + outerR * cos, scy + outerR * sin));

            // Numbers at 10, 20, 30, 40, 50, 60
            if (isMajor10) {
                String label = (m == 0) ? "60" : String.valueOf(m);
                double textR = sradius * 0.52;
                double tx = scx + textR * cos;
                double ty = scy + textR * sin;
                int tw = fm.stringWidth(label);
                int th = fm.getAscent() - 2;
                g2.setColor(numberColor);
                g2.drawString(label, (float) (tx - tw / 2.0), (float) (ty + th / 2.0));
            }
        }

        // Subdial hand (sweeps 60 minutes full rotation = 3600 seconds)
        long totalSeconds = elapsedMillis / 1000;
        double minutes = (totalSeconds % 3600) / 60.0; // 0 to 60 mins
        double handAngle = Math.toRadians((minutes / 60.0) * 360.0 - 90.0);

        AffineTransform old = g2.getTransform();
        g2.translate(scx, scy);
        g2.rotate(handAngle + Math.PI / 2.0);

        // Blued/accent precision hand
        g2.setColor(subdialHandColor);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(0, sradius * 0.20, 0, -sradius * 0.78));

        // Subdial pivot
        g2.fill(new Ellipse2D.Double(-2.5, -2.5, 5, 5));
        g2.setTransform(old);
    }

    private void drawDigitalWindow(Graphics2D g2, double dwx, double dwy, double dwidth, double dheight) {
        double x = dwx - dwidth / 2.0;
        double y = dwy - dheight / 2.0;

        // Modern rounded digital window
        g2.setColor(digitalWindowBg);
        g2.fillRoundRect((int) x, (int) y, (int) dwidth, (int) dheight, 10, 10);
        g2.setColor(dialBorder);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect((int) x, (int) y, (int) dwidth, (int) dheight, 10, 10);

        // Elapsed time formatted: HH:MM:SS.s or HH:MM:SS
        long totalSec = elapsedMillis / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        long tenths = (elapsedMillis % 1000) / 100;
        String text = String.format("%02d:%02d:%02d.%d", h, m, s, tenths);

        Font df = new Font("Segoe UI Semibold", Font.BOLD, (int) Math.max(11, dheight * 0.58));
        g2.setFont(df);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int th = fm.getAscent() - 2;
        g2.setColor(numberColor);
        g2.drawString(text, (float) (dwx - tw / 2.0), (float) (dwy + th / 2.0));
    }

    private void drawChronoSecondsHand(Graphics2D g2, double cx, double cy, double handLength) {
        // Continuous smooth high-precision sweep (60 seconds per full revolution)
        double secondsInRev = (elapsedMillis % 60000) / 1000.0;
        double angleRad = Math.toRadians((secondsInRev / 60.0) * 360.0 - 90.0);

        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(angleRad + Math.PI / 2.0);

        // Hand style: long slender needle with needle-tip, circular counterweight at the bottom tail
        g2.setColor(chronoHandColor);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Pointer extending forward
        g2.draw(new Line2D.Double(0, handLength * 0.22, 0, -handLength));

        // Lollipop / circular accent near counterweight tail
        double tailLen = handLength * 0.22;
        g2.fill(new Ellipse2D.Double(-3.5, tailLen - 7, 7, 7));

        g2.setTransform(old);
    }
}
