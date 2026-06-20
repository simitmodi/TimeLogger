package com.timelogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SessionRecord {
    public enum SessionType {
        STOPWATCH,
        TIMER
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SessionType type;
    private final String subject;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long durationSeconds;
    private final String description;

    public SessionRecord(SessionType type, String subject, LocalDateTime startTime, LocalDateTime endTime, long durationSeconds) {
        this(type, subject, startTime, endTime, durationSeconds, "");
    }

    public SessionRecord(SessionType type, String subject, LocalDateTime startTime, LocalDateTime endTime, long durationSeconds, String description) {
        this.type = type;
        this.subject = subject;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationSeconds = durationSeconds;
        this.description = description;
    }

    public SessionType getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public String getDescription() {
        return description;
    }

    public String toStorageLine() {
        return String.join("|",
            type.name(),
            sanitize(subject),
            startTime.format(DATE_FORMAT),
            endTime.format(DATE_FORMAT),
            String.valueOf(durationSeconds),
            sanitize(description)
        );
    }

    public static SessionRecord fromStorageLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid session record format");
        }

        SessionType type = SessionType.valueOf(parts[0]);
        String subject = parts[1];
        LocalDateTime start = LocalDateTime.parse(parts[2], DATE_FORMAT);
        LocalDateTime end = LocalDateTime.parse(parts[3], DATE_FORMAT);
        long duration = Long.parseLong(parts[4]);
        String description = parts.length > 5 ? parts[5] : "";

        return new SessionRecord(type, subject, start, end, duration, description);
    }

    private static String sanitize(String value) {
        return value.replace("|", "/").trim();
    }
}
