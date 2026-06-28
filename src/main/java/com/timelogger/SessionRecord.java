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
    private final int pauseCount;
    private final int questionsSolved;

    public SessionRecord(SessionType type, String subject, LocalDateTime startTime, LocalDateTime endTime, long durationSeconds) {
        this(type, subject, startTime, endTime, durationSeconds, "", 0, 0);
    }

    public SessionRecord(SessionType type, String subject, LocalDateTime startTime, LocalDateTime endTime, long durationSeconds, String description) {
        this(type, subject, startTime, endTime, durationSeconds, description, 0, 0);
    }

    public SessionRecord(SessionType type, String subject, LocalDateTime startTime, LocalDateTime endTime, long durationSeconds, String description, int pauseCount) {
        this(type, subject, startTime, endTime, durationSeconds, description, pauseCount, 0);
    }

    public SessionRecord(SessionType type, String subject, LocalDateTime startTime, LocalDateTime endTime, long durationSeconds, String description, int pauseCount, int questionsSolved) {
        this.type = type;
        this.subject = subject;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationSeconds = durationSeconds;
        this.description = description;
        this.pauseCount = pauseCount;
        this.questionsSolved = questionsSolved;
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

    public int getPauseCount() {
        return pauseCount;
    }

    public int getQuestionsSolved() {
        return questionsSolved;
    }

    public String toStorageLine() {
        return String.join("|",
            type.name(),
            sanitize(subject),
            startTime.format(DATE_FORMAT),
            endTime.format(DATE_FORMAT),
            String.valueOf(durationSeconds),
            sanitize(description),
            String.valueOf(pauseCount),
            String.valueOf(questionsSolved)
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
        int pauseCount = 0;
        if (parts.length > 6 && !parts[6].isEmpty()) {
            try {
                pauseCount = Integer.parseInt(parts[6]);
            } catch (NumberFormatException ignored) {}
        }
        int questionsSolved = 0;
        if (parts.length > 7 && !parts[7].isEmpty()) {
            try {
                questionsSolved = Integer.parseInt(parts[7]);
            } catch (NumberFormatException ignored) {}
        }

        return new SessionRecord(type, subject, start, end, duration, description, pauseCount, questionsSolved);
    }

    private static String sanitize(String value) {
        return value.replace("|", "/").trim();
    }
}
