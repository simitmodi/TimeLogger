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
    private final Path questionTopicsFile;

    public StorageService() {
        this.appDirectory = Paths.get(System.getProperty("user.dir"));
        this.subjectsFile = appDirectory.resolve("subjects.txt");
        this.sessionsFile = appDirectory.resolve("sessions.log");
        this.questionTopicsFile = appDirectory.resolve("question_topics.txt");
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

    public boolean loadNotificationEnabled() {
        Path file = appDirectory.resolve("notifications_enabled.txt");
        try {
            if (!Files.exists(file)) {
                saveNotificationEnabled(true);
                return true;
            }
            return Boolean.parseBoolean(Files.readString(file, StandardCharsets.UTF_8).trim());
        } catch (Exception e) {
            return true;
        }
    }

    public void saveNotificationEnabled(boolean enabled) {
        Path file = appDirectory.resolve("notifications_enabled.txt");
        try {
            Files.writeString(file, String.valueOf(enabled), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
    }

    public boolean loadBreakRemindersEnabled() {
        Path file = appDirectory.resolve("break_reminders_enabled.txt");
        try {
            if (!Files.exists(file)) {
                saveBreakRemindersEnabled(true);
                return true;
            }
            return Boolean.parseBoolean(Files.readString(file, StandardCharsets.UTF_8).trim());
        } catch (Exception e) {
            return true;
        }
    }

    public void saveBreakRemindersEnabled(boolean enabled) {
        Path file = appDirectory.resolve("break_reminders_enabled.txt");
        try {
            Files.writeString(file, String.valueOf(enabled), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
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
            if (!Files.exists(questionTopicsFile)) {
                Files.write(questionTopicsFile, Arrays.asList("General", "Practice", "Revision"), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            performAutoBackup();
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize storage", e);
        }
    }

    private void performAutoBackup() {
        Path backupsDir = appDirectory.resolve("backups");
        try {
            Files.createDirectories(backupsDir);
            
            String todayStr = java.time.LocalDate.now().toString();
            Path backupZipFile = backupsDir.resolve("timelogger_backup_" + todayStr + ".zip");
            if (Files.exists(backupZipFile)) {
                return;
            }

            if (Files.exists(sessionsFile) || Files.exists(subjectsFile)) {
                try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(backupZipFile))) {
                    addToZip(zos, sessionsFile, "sessions.log");
                    addToZip(zos, subjectsFile, "subjects.txt");
                }
            }

            try (java.util.stream.Stream<Path> files = Files.list(backupsDir)) {
                List<Path> backupFiles = files
                    .filter(p -> p.getFileName().toString().startsWith("timelogger_backup_") && p.getFileName().toString().endsWith(".zip"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .collect(Collectors.toList());

                while (backupFiles.size() > 7) {
                    Files.deleteIfExists(backupFiles.remove(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String loadOpenRouterApiKey() {
        Path newFile = Paths.get(System.getProperty("user.home"), ".timelogger_openrouter_key");
        Path oldFile = appDirectory.resolve("openrouter_api_key.txt");
        
        // Migrate old file if it exists and new file doesn't
        if (Files.exists(oldFile)) {
            try {
                String key = Files.readString(oldFile, StandardCharsets.UTF_8).trim();
                if (!key.isEmpty()) {
                    Files.writeString(newFile, key, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
                Files.deleteIfExists(oldFile);
            } catch (Exception ignored) {}
        }
        
        try {
            if (!Files.exists(newFile)) {
                return "";
            }
            return Files.readString(newFile, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    public void saveOpenRouterApiKey(String apiKey) {
        Path newFile = Paths.get(System.getProperty("user.home"), ".timelogger_openrouter_key");
        Path oldFile = appDirectory.resolve("openrouter_api_key.txt");
        try {
            // Delete old file if present
            Files.deleteIfExists(oldFile);
            
            Files.writeString(newFile, apiKey.trim(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save OpenRouter API key", e);
        }
    }

    public String loadOpenRouterModel() {
        Path file = Paths.get(System.getProperty("user.home"), ".timelogger_openrouter_model");
        try {
            if (!Files.exists(file)) {
                return "google/gemma-4-26b-a4b-it:free";
            }
            return Files.readString(file, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "google/gemma-4-26b-a4b-it:free";
        }
    }

    public void saveOpenRouterModel(String model) {
        Path file = Paths.get(System.getProperty("user.home"), ".timelogger_openrouter_model");
        try {
            Files.writeString(file, model.trim(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save OpenRouter model selection", e);
        }
    }

    public List<String> loadQuestionTopics() {
        try {
            if (!Files.exists(questionTopicsFile)) {
                return new ArrayList<>(Arrays.asList("General", "Practice", "Revision"));
            }
            List<String> lines = Files.readAllLines(questionTopicsFile, StandardCharsets.UTF_8);
            return lines.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read question topics", e);
        }
    }

    public void saveQuestionTopics(List<String> topics) {
        List<String> cleaned = topics.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        try {
            Files.write(questionTopicsFile, cleaned, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save question topics", e);
        }
    }

    public String loadLastQuestionDesc(String type) {
        Path file = appDirectory.resolve("last_question_descs.properties");
        if (!Files.exists(file)) {
            return "";
        }
        java.util.Properties props = new java.util.Properties();
        try (java.io.Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
            return props.getProperty(type, "");
        } catch (Exception e) {
            return "";
        }
    }

    public void saveLastQuestionDesc(String type, String desc) {
        Path file = appDirectory.resolve("last_question_descs.properties");
        java.util.Properties props = new java.util.Properties();
        if (Files.exists(file)) {
            try (java.io.Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (Exception ignored) {}
        }
        props.setProperty(type, desc);
        try (java.io.Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            props.store(writer, null);
        } catch (Exception ignored) {}
    }


    private void addToZip(java.util.zip.ZipOutputStream zos, Path file, String zipEntryName) throws IOException {
        if (!Files.exists(file)) return;
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(zipEntryName);
        zos.putNextEntry(entry);
        Files.copy(file, zos);
        zos.closeEntry();
    }
}
