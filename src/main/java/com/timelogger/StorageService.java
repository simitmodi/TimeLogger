package com.timelogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StorageService {
    private static final List<String> DEFAULT_SUBJECTS = Arrays.asList("Math", "Science", "Language");

    private final Path appDirectory;
    private final Path subjectsFile;
    private final Path sessionsFile;

    public StorageService() {
        this.appDirectory = Paths.get(System.getProperty("user.dir"));
        this.subjectsFile = appDirectory.resolve("subjects.txt");
        this.sessionsFile = appDirectory.resolve("sessions.log");
        initialize();
    }

    public Path getAppDirectory() {
        return appDirectory;
    }

    public List<String> loadSubjects() {
        try {
            List<String> lines = Files.readAllLines(subjectsFile, StandardCharsets.UTF_8);
            List<String> subjects = lines.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

            if (subjects.isEmpty()) {
                saveSubjects(DEFAULT_SUBJECTS);
                return new ArrayList<>(DEFAULT_SUBJECTS);
            }

            return subjects;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read subjects", e);
        }
    }

    public void saveSubjects(List<String> subjects) {
        List<String> cleaned = subjects.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(Collectors.toList());

        if (cleaned.isEmpty()) {
            cleaned = new ArrayList<>(DEFAULT_SUBJECTS);
        }

        try {
            Files.write(subjectsFile, cleaned, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save subjects", e);
        }
    }

    public List<SessionRecord> loadSessions() {
        try {
            if (!Files.exists(sessionsFile)) {
                return new ArrayList<>();
            }

            List<String> lines = Files.readAllLines(sessionsFile, StandardCharsets.UTF_8);
            List<SessionRecord> sessions = new ArrayList<>();

            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    sessions.add(SessionRecord.fromStorageLine(line));
                } catch (Exception ignored) {
                }
            }

            return sessions;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read sessions", e);
        }
    }

    public void appendSession(SessionRecord record) {
        try {
            Files.writeString(sessionsFile, record.toStorageLine() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save session", e);
        }
    }

    public void saveSessions(List<SessionRecord> sessions) {
        List<String> lines = sessions.stream()
            .map(SessionRecord::toStorageLine)
            .collect(Collectors.toList());
        try {
            Files.write(sessionsFile, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save sessions", e);
        }
    }

    public int loadDailyGoalMinutes() {
        Path goalFile = appDirectory.resolve("goal.txt");
        try {
            if (!Files.exists(goalFile)) {
                saveDailyGoalMinutes(120); // Default to 120 minutes (2 hours)
                return 120;
            }
            String val = Files.readString(goalFile, StandardCharsets.UTF_8).trim();
            return Integer.parseInt(val);
        } catch (Exception e) {
            return 120;
        }
    }

    public void saveDailyGoalMinutes(int minutes) {
        Path goalFile = appDirectory.resolve("goal.txt");
        try {
            Files.writeString(goalFile, String.valueOf(minutes), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save daily goal", e);
        }
    }

    private void initialize() {
        try {
            Files.createDirectories(appDirectory);
            if (!Files.exists(subjectsFile)) {
                Files.write(subjectsFile, DEFAULT_SUBJECTS, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            if (!Files.exists(sessionsFile)) {
                Files.createFile(sessionsFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize storage", e);
        }
    }
}
