package com.timelogger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single general reminder task inspired by Apple Reminders.
 * Includes title, notes, due date, due time, day period (Morning, Afternoon, Evening, etc.),
 * subject affiliation, priority, flag status, and completion state.
 */
public class ReminderItem {

    public enum TimePeriod {
        MORNING("Morning", LocalTime.of(9, 0)),
        AFTERNOON("Afternoon", LocalTime.of(14, 0)),
        EVENING("Evening", LocalTime.of(19, 0)),
        NIGHT("Night", LocalTime.of(21, 30)),
        ALL_DAY("Any Time", null);

        private final String displayName;
        private final LocalTime defaultTime;

        TimePeriod(String displayName, LocalTime defaultTime) {
            this.displayName = displayName;
            this.defaultTime = defaultTime;
        }

        public String getDisplayName() {
            return displayName;
        }

        public LocalTime getDefaultTime() {
            return defaultTime;
        }

        public static TimePeriod fromTime(LocalTime time) {
            if (time == null) return ALL_DAY;
            int h = time.getHour();
            if (h >= 5 && h < 12) return MORNING;
            if (h >= 12 && h < 17) return AFTERNOON;
            if (h >= 17 && h < 21) return EVENING;
            return NIGHT;
        }

        public static TimePeriod fromString(String name) {
            if (name == null) return ALL_DAY;
            for (TimePeriod p : values()) {
                if (p.name().equalsIgnoreCase(name) || p.displayName.equalsIgnoreCase(name)) {
                    return p;
                }
            }
            return ALL_DAY;
        }
    }

    public enum Priority {
        NONE("None", 0, ""),
        LOW("Low", 1, "!"),
        MEDIUM("Medium", 2, "!!"),
        HIGH("High", 3, "!!!");

        private final String displayName;
        private final int level;
        private final String badge;

        Priority(String displayName, int level, String badge) {
            this.displayName = displayName;
            this.level = level;
            this.badge = badge;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getLevel() {
            return level;
        }

        public String getBadge() {
            return badge;
        }

        public static Priority fromString(String str) {
            if (str == null) return NONE;
            for (Priority p : values()) {
                if (p.name().equalsIgnoreCase(str) || p.displayName.equalsIgnoreCase(str)) {
                    return p;
                }
            }
            return NONE;
        }
    }

    private String id;
    private String title;
    private String notes;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private TimePeriod period;
    private String subject;
    private Priority priority;
    private boolean flagged;
    private boolean completed;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private boolean notified;

    public ReminderItem() {
        this.id = UUID.randomUUID().toString();
        this.title = "";
        this.notes = "";
        this.dueDate = LocalDate.now();
        this.period = TimePeriod.MORNING;
        this.dueTime = TimePeriod.MORNING.getDefaultTime();
        this.subject = "General";
        this.priority = Priority.NONE;
        this.flagged = false;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.notified = false;
    }

    public ReminderItem(String title, LocalDate dueDate, TimePeriod period, String subject) {
        this();
        this.title = title;
        this.dueDate = dueDate != null ? dueDate : LocalDate.now();
        this.period = period != null ? period : TimePeriod.ALL_DAY;
        this.dueTime = this.period.getDefaultTime();
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
        if (dueTime != null && this.period == TimePeriod.ALL_DAY) {
            this.period = TimePeriod.fromTime(dueTime);
        }
    }

    public TimePeriod getPeriod() {
        return period;
    }

    public void setPeriod(TimePeriod period) {
        this.period = period;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && completedAt == null) {
            this.completedAt = LocalDateTime.now();
        } else if (!completed) {
            this.completedAt = null;
        }
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public boolean isDueNowOrPast() {
        if (completed || dueDate == null) return false;
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) return true;
        if (dueDate.isEqual(today)) {
            if (dueTime == null) return true; // Due sometime today
            return !LocalTime.now().isBefore(dueTime);
        }
        return false;
    }

    public boolean isDueToday() {
        return dueDate != null && dueDate.isEqual(LocalDate.now());
    }

    public boolean isOverdue() {
        if (completed || dueDate == null) return false;
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) return true;
        if (dueDate.isEqual(today) && dueTime != null) {
            return LocalTime.now().isAfter(dueTime);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReminderItem that = (ReminderItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
