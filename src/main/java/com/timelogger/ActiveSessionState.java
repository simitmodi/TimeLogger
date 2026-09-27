package com.timelogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ActiveSessionState {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public enum SessionMode {
        STOPWATCH,
        TIMER
    }

    private final SessionMode mode;
    private final String subject;
    private final LocalDateTime sessionStart;
    private final long elapsedSeconds;
    private final int pauseCount;
    private final boolean isRunning;
    private final String activityType;
    private final String activityDetail;
    private final long timerTotalSeconds;
    private final long timerRemainingSeconds;
    private final LocalDateTime lastHeartbeat;

    public ActiveSessionState(
            SessionMode mode,
            String subject,
            LocalDateTime sessionStart,
            long elapsedSeconds,
            int pauseCount,
            boolean isRunning,
            String activityType,
            String activityDetail,
            long timerTotalSeconds,
            long timerRemainingSeconds,
            LocalDateTime lastHeartbeat) {
        this.mode = mode != null ? mode : SessionMode.STOPWATCH;
        this.subject = subject != null ? subject : "General";
        this.sessionStart = sessionStart != null ? sessionStart : LocalDateTime.now();
        this.elapsedSeconds = Math.max(0, elapsedSeconds);
        this.pauseCount = Math.max(0, pauseCount);
        this.isRunning = isRunning;
        this.activityType = activityType != null ? activityType : "General";
        this.activityDetail = activityDetail != null ? activityDetail : "";
        this.timerTotalSeconds = Math.max(0, timerTotalSeconds);
        this.timerRemainingSeconds = Math.max(0, timerRemainingSeconds);
        this.lastHeartbeat = lastHeartbeat != null ? lastHeartbeat : LocalDateTime.now();
    }

    public SessionMode getMode() { return mode; }
    public String getSubject() { return subject; }
    public LocalDateTime getSessionStart() { return sessionStart; }
    public long getElapsedSeconds() { return elapsedSeconds; }
    public int getPauseCount() { return pauseCount; }
    public boolean isRunning() { return isRunning; }
    public String getActivityType() { return activityType; }
    public String getActivityDetail() { return activityDetail; }
    public long getTimerTotalSeconds() { return timerTotalSeconds; }
    public long getTimerRemainingSeconds() { return timerRemainingSeconds; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }

    public Properties toProperties() {
        Properties props = new Properties();
        props.setProperty("mode", mode.name());
        props.setProperty("subject", subject);
        props.setProperty("sessionStart", sessionStart.format(ISO_FORMATTER));
        props.setProperty("elapsedSeconds", String.valueOf(elapsedSeconds));
        props.setProperty("pauseCount", String.valueOf(pauseCount));
        props.setProperty("isRunning", String.valueOf(isRunning));
        props.setProperty("activityType", activityType);
        props.setProperty("activityDetail", activityDetail);
        props.setProperty("timerTotalSeconds", String.valueOf(timerTotalSeconds));
        props.setProperty("timerRemainingSeconds", String.valueOf(timerRemainingSeconds));
        props.setProperty("lastHeartbeat", lastHeartbeat.format(ISO_FORMATTER));
        return props;
    }

    public static ActiveSessionState fromProperties(Properties props) {
        if (props == null || props.isEmpty()) return null;
        try {
            SessionMode mode = SessionMode.valueOf(props.getProperty("mode", "STOPWATCH"));
            String subject = props.getProperty("subject", "General");
            String startStr = props.getProperty("sessionStart");
            LocalDateTime start = startStr != null ? LocalDateTime.parse(startStr, ISO_FORMATTER) : LocalDateTime.now();
            long elapsed = Long.parseLong(props.getProperty("elapsedSeconds", "0"));
            int pauses = Integer.parseInt(props.getProperty("pauseCount", "0"));
            boolean running = Boolean.parseBoolean(props.getProperty("isRunning", "false"));
            String actType = props.getProperty("activityType", "General");
            String actDetail = props.getProperty("activityDetail", "");
            long timerTotal = Long.parseLong(props.getProperty("timerTotalSeconds", "0"));
            long timerRem = Long.parseLong(props.getProperty("timerRemainingSeconds", "0"));
            String hbStr = props.getProperty("lastHeartbeat");
            LocalDateTime hb = hbStr != null ? LocalDateTime.parse(hbStr, ISO_FORMATTER) : LocalDateTime.now();

            return new ActiveSessionState(
                mode, subject, start, elapsed, pauses, running,
                actType, actDetail, timerTotal, timerRem, hb
            );
        } catch (Exception e) {
            return null;
        }
    }
}
