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
    private final Path activeSessionFile;
    private final Path remindersFile;

    public StorageService() {
        this.appDirectory = Paths.get(System.getProperty("user.dir"));
        this.subjectsFile = appDirectory.resolve("subjects.txt");
        this.sessionsFile = appDirectory.resolve("sessions.log");
        this.activeSessionFile = appDirectory.resolve("active_session.state");
        this.remindersFile = appDirectory.resolve("reminders.json");
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
            
            Path dppFile = getQuestionTopicsFile("DPP Questions");
            Path practiceFile = getQuestionTopicsFile("Practice Book Questions");
            Path pyqFile = getQuestionTopicsFile("Previous Year Questions");
            if (!Files.exists(dppFile)) {
                Files.write(dppFile, Arrays.asList("General", "Practice", "Revision"), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            if (!Files.exists(practiceFile)) {
                Files.write(practiceFile, Arrays.asList("General", "Practice", "Revision"), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            if (!Files.exists(pyqFile)) {
                Files.write(pyqFile, Arrays.asList("General", "Practice", "Revision"), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }

            Path revisionFile = getRevisionTopicsFile();
            if (!Files.exists(revisionFile)) {
                List<String> defaultTopics = new ArrayList<>();
                defaultTopics.add("General");
                defaultTopics.add("Theory Revision");
                defaultTopics.add("Formula Revision");
                defaultTopics.add("Short Notes");
                
                // Automatically populate existing revision topics from sessions.log
                List<SessionRecord> existingSessions = loadSessions();
                for (SessionRecord s : existingSessions) {
                    if (s.getSubject() != null && !s.getSubject().trim().isEmpty()) {
                        String desc = s.getDescription();
                        if (desc != null && desc.startsWith("Revision: ")) {
                            String topic = desc.substring("Revision: ".length()).trim();
                            if (!topic.isEmpty()) {
                                String entry = s.getSubject().trim() + ":" + topic;
                                if (!defaultTopics.contains(entry)) {
                                    defaultTopics.add(entry);
                                }
                            }
                        }
                    }
                }
                Files.write(revisionFile, defaultTopics, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }

            performAutoBackup();
            migrateLegacyTopics();
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize storage", e);
        }
    }

    private void migrateLegacyTopics() {
        migrateLegacyTopicsForType("DPP Questions");
        migrateLegacyTopicsForType("Practice Book Questions");
        migrateLegacyTopicsForType("Previous Year Questions");
    }

    private void migrateLegacyTopicsForType(String type) {
        Path file = getQuestionTopicsFile(type);
        if (!Files.exists(file)) return;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            boolean migrated = false;
            
            List<SessionRecord> sessions = loadSessions();
            
            for (String line : lines) {
                String trim = line.trim();
                if (trim.isEmpty()) continue;
                
                if (trim.contains(":")) {
                    if (!newLines.contains(trim)) {
                        newLines.add(trim);
                    }
                    continue;
                }
                
                if (trim.equalsIgnoreCase("General") || trim.equalsIgnoreCase("Practice") || trim.equalsIgnoreCase("Revision")) {
                    if (!newLines.contains(trim)) {
                        newLines.add(trim);
                    }
                    continue;
                }
                
                String mappedSubject = null;
                for (SessionRecord s : sessions) {
                    String desc = s.getDescription();
                    if (desc != null && desc.startsWith("Questions: ")) {
                        String content = desc.substring("Questions: ".length());
                        int commaIndex = content.indexOf(",");
                        if (commaIndex != -1) {
                            String qType = content.substring(0, commaIndex).trim();
                            if (qType.equals(type)) {
                                String qDesc = content.substring(commaIndex + 1).trim();
                                int solvedIdx = qDesc.indexOf(" (Solved:");
                                if (solvedIdx != -1) {
                                    qDesc = qDesc.substring(0, solvedIdx).trim();
                                }
                                if (qDesc.equalsIgnoreCase(trim)) {
                                    mappedSubject = s.getSubject();
                                    break;
                                }
                            }
                        }
                    }
                }
                
                if (mappedSubject != null) {
                    String prefixed = mappedSubject + ":" + trim;
                    if (!newLines.contains(prefixed)) {
                        newLines.add(prefixed);
                        migrated = true;
                    }
                } else {
                    if (!newLines.contains(trim)) {
                        newLines.add(trim);
                    }
                }
            }
            
            if (migrated) {
                Files.write(file, newLines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException ignored) {}
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

    private Path getQuestionTopicsFile(String type) {
        String filename = "question_topics_general.txt";
        if ("DPP Questions".equals(type)) {
            filename = "question_topics_dpp.txt";
        } else if ("Practice Book Questions".equals(type)) {
            filename = "question_topics_practice.txt";
        } else if ("Previous Year Questions".equals(type)) {
            filename = "question_topics_pyq.txt";
        }
        return appDirectory.resolve(filename);
    }

    public List<String> extractQuestionTopicsFromLogs(String type, String subject) {
        List<String> topics = new ArrayList<>();
        List<SessionRecord> sessions = loadSessions();
        for (SessionRecord s : sessions) {
            if (s.getSubject() != null && s.getSubject().equalsIgnoreCase(subject)) {
                String desc = s.getDescription();
                if (desc != null && desc.startsWith("Questions: ")) {
                    String content = desc.substring("Questions: ".length());
                    int commaIndex = content.indexOf(",");
                    if (commaIndex != -1) {
                        String qType = content.substring(0, commaIndex).trim();
                        if (qType.equals(type)) {
                            String qDesc = content.substring(commaIndex + 1).trim();
                            int solvedIdx = qDesc.indexOf(" (Solved:");
                            if (solvedIdx != -1) {
                                qDesc = qDesc.substring(0, solvedIdx).trim();
                            }
                            if (!qDesc.isEmpty() && !topics.contains(qDesc)) {
                                topics.add(qDesc);
                            }
                        }
                    }
                }
            }
        }
        return topics;
    }

    public List<String> loadQuestionTopics(String type, String subject) {
        Path file = getQuestionTopicsFile(type);
        try {
            List<String> topicsForSubject = new ArrayList<>();
            List<String> allLines = new ArrayList<>();
            if (Files.exists(file)) {
                allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } else {
                allLines.addAll(Arrays.asList("General", "Practice", "Revision"));
            }

            for (String line : allLines) {
                String trim = line.trim();
                if (trim.isEmpty()) continue;
                int colonIdx = trim.indexOf(":");
                if (colonIdx != -1) {
                    String sub = trim.substring(0, colonIdx).trim();
                    String top = trim.substring(colonIdx + 1).trim();
                    if (sub.equalsIgnoreCase(subject) && !topicsForSubject.contains(top)) {
                        topicsForSubject.add(top);
                    }
                } else {
                    // Legacy topic without subject - show for all subjects
                    if (!topicsForSubject.contains(trim)) {
                        topicsForSubject.add(trim);
                    }
                }
            }

            List<String> logTopics = extractQuestionTopicsFromLogs(type, subject);
            boolean changed = false;
            for (String lt : logTopics) {
                boolean exists = false;
                for (String t : topicsForSubject) {
                    if (t.equalsIgnoreCase(lt)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    topicsForSubject.add(lt);
                    changed = true;
                }
            }

            if (changed || !Files.exists(file)) {
                saveQuestionTopics(type, subject, topicsForSubject);
            }

            return topicsForSubject;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read question topics for " + type, e);
        }
    }

    public void saveQuestionTopics(String type, String subject, List<String> topics) {
        Path file = getQuestionTopicsFile(type);
        List<String> allLines = new ArrayList<>();
        if (Files.exists(file)) {
            try {
                allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } catch (IOException ignored) {}
        }
        
        List<String> preservedLines = new ArrayList<>();
        // Preserve all lines that are not for the current subject
        for (String line : allLines) {
            String trim = line.trim();
            if (trim.isEmpty()) continue;
            int colonIdx = trim.indexOf(":");
            if (colonIdx != -1) {
                String sub = trim.substring(0, colonIdx).trim();
                if (!sub.equalsIgnoreCase(subject)) {
                    preservedLines.add(trim);
                }
            } else {
                // Keep legacy lines without a subject
                preservedLines.add(trim);
            }
        }
        
        // Add new topics formatted as "Subject:Topic"
        for (String top : topics) {
            String cleanTop = top.trim();
            if (!cleanTop.isEmpty()) {
                preservedLines.add(subject + ":" + cleanTop);
            }
        }
        
        List<String> cleaned = preservedLines.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        try {
            Files.write(file, cleaned, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save question topics for " + type, e);
        }
    }

    public String loadLastQuestionDesc(String type, String subject) {
        Path file = appDirectory.resolve("last_question_descs.properties");
        if (!Files.exists(file)) {
            return "";
        }
        java.util.Properties props = new java.util.Properties();
        try (java.io.Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
            String val = props.getProperty(type + "_" + subject, "");
            if (val.isEmpty()) {
                val = props.getProperty(type, ""); // fallback to legacy key
            }
            return val;
        } catch (Exception e) {
            return "";
        }
    }

    public void saveLastQuestionDesc(String type, String subject, String desc) {
        Path file = appDirectory.resolve("last_question_descs.properties");
        java.util.Properties props = new java.util.Properties();
        if (Files.exists(file)) {
            try (java.io.Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (Exception ignored) {}
        }
        props.setProperty(type + "_" + subject, desc);
        try (java.io.Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            props.store(writer, null);
        } catch (Exception ignored) {}
    }

    public Path getRevisionTopicsFile() {
        return appDirectory.resolve("revision_topics.txt");
    }

    public List<String> extractRevisionTopicsFromLogs(String subject) {
        List<String> topics = new ArrayList<>();
        List<SessionRecord> sessions = loadSessions();
        for (SessionRecord s : sessions) {
            if (s.getSubject() != null && s.getSubject().equalsIgnoreCase(subject)) {
                String desc = s.getDescription();
                if (desc != null && desc.startsWith("Revision: ")) {
                    String rTopic = desc.substring("Revision: ".length()).trim();
                    if (!rTopic.isEmpty() && !topics.contains(rTopic)) {
                        topics.add(rTopic);
                    }
                }
            }
        }
        return topics;
    }

    public List<String> loadRevisionTopics(String subject) {
        Path file = getRevisionTopicsFile();
        try {
            List<String> topicsForSubject = new ArrayList<>();
            List<String> allLines = new ArrayList<>();
            if (Files.exists(file)) {
                allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } else {
                allLines.addAll(Arrays.asList("General", "Theory Revision", "Formula Revision", "Short Notes"));
            }

            for (String line : allLines) {
                String trim = line.trim();
                if (trim.isEmpty()) continue;
                int colonIdx = trim.indexOf(":");
                if (colonIdx != -1) {
                    String sub = trim.substring(0, colonIdx).trim();
                    String top = trim.substring(colonIdx + 1).trim();
                    if (sub.equalsIgnoreCase(subject) && !topicsForSubject.contains(top)) {
                        topicsForSubject.add(top);
                    }
                } else {
                    // Predefined / legacy topic without subject - show for all subjects
                    if (!topicsForSubject.contains(trim)) {
                        topicsForSubject.add(trim);
                    }
                }
            }

            // Also check logged sessions for any already logged topics
            List<String> logTopics = extractRevisionTopicsFromLogs(subject);
            boolean changed = false;
            for (String lt : logTopics) {
                boolean exists = false;
                for (String t : topicsForSubject) {
                    if (t.equalsIgnoreCase(lt)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    topicsForSubject.add(lt);
                    changed = true;
                }
            }

            if (changed || !Files.exists(file)) {
                saveRevisionTopics(subject, topicsForSubject);
            }

            return topicsForSubject;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read revision topics", e);
        }
    }

    public void saveRevisionTopics(String subject, List<String> topics) {
        Path file = getRevisionTopicsFile();
        List<String> allLines = new ArrayList<>();
        if (Files.exists(file)) {
            try {
                allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } catch (IOException ignored) {}
        }
        
        List<String> preservedLines = new ArrayList<>();
        // Preserve lines not matching this subject
        for (String line : allLines) {
            String trim = line.trim();
            if (trim.isEmpty()) continue;
            int colonIdx = trim.indexOf(":");
            if (colonIdx != -1) {
                String sub = trim.substring(0, colonIdx).trim();
                if (!sub.equalsIgnoreCase(subject)) {
                    preservedLines.add(trim);
                }
            } else {
                // Keep predefined lines without a subject
                preservedLines.add(trim);
            }
        }
        
        // Add new topics formatted as "Subject:Topic"
        for (String top : topics) {
            String cleanTop = top.trim();
            if (!cleanTop.isEmpty()) {
                // If it's one of the generic predefined topics without subject, keep as is, otherwise prefix subject
                if (cleanTop.equalsIgnoreCase("General") || cleanTop.equalsIgnoreCase("Theory Revision")
                        || cleanTop.equalsIgnoreCase("Formula Revision") || cleanTop.equalsIgnoreCase("Short Notes")) {
                    if (!preservedLines.contains(cleanTop)) {
                        preservedLines.add(cleanTop);
                    }
                } else {
                    preservedLines.add(subject + ":" + cleanTop);
                }
            }
        }
        
        List<String> cleaned = preservedLines.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        try {
            Files.write(file, cleaned, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save revision topics", e);
        }
    }

    public String loadLastRevisionTopic(String subject) {
        Path file = appDirectory.resolve("last_question_descs.properties");
        if (!Files.exists(file)) {
            return "";
        }
        java.util.Properties props = new java.util.Properties();
        try (java.io.Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
            return props.getProperty("Revision_" + subject, props.getProperty("Revision", ""));
        } catch (Exception e) {
            return "";
        }
    }

    public void saveLastRevisionTopic(String subject, String topic) {
        Path file = appDirectory.resolve("last_question_descs.properties");
        java.util.Properties props = new java.util.Properties();
        if (Files.exists(file)) {
            try (java.io.Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (Exception ignored) {}
        }
        props.setProperty("Revision_" + subject, topic);
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

    public synchronized void saveActiveSessionState(ActiveSessionState state) {
        if (state == null) {
            clearActiveSessionState();
            return;
        }
        Path tempFile = appDirectory.resolve("active_session.state.tmp");
        try {
            java.util.Properties props = state.toProperties();
            try (java.io.Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                props.store(writer, "TimeLogger Active Session State");
            }
            try {
                Files.move(tempFile, activeSessionFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tempFile, activeSessionFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception ignored) {}
        }
    }

    public synchronized ActiveSessionState loadActiveSessionState() {
        if (!hasActiveSessionState()) return null;
        java.util.Properties props = new java.util.Properties();
        try (java.io.Reader reader = Files.newBufferedReader(activeSessionFile, StandardCharsets.UTF_8)) {
            props.load(reader);
            return ActiveSessionState.fromProperties(props);
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized boolean hasActiveSessionState() {
        return Files.exists(activeSessionFile);
    }

    public synchronized void clearActiveSessionState() {
        try {
            Files.deleteIfExists(activeSessionFile);
        } catch (Exception ignored) {}
    }

    public String loadPreferredLaunchMode() {
        Path file = appDirectory.resolve("launch_mode.txt");
        try {
            if (Files.exists(file)) {
                return Files.readString(file, StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {}
        return "";
    }

    public void savePreferredLaunchMode(String mode) {
        Path file = appDirectory.resolve("launch_mode.txt");
        try {
            if (mode == null || mode.isEmpty()) {
                Files.deleteIfExists(file);
            } else {
                Files.writeString(file, mode, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception ignored) {}
    }

    public String loadStopwatchDisplayStyle() {
        Path file = appDirectory.resolve("stopwatch_style.txt");
        try {
            if (Files.exists(file)) {
                String val = Files.readString(file, StandardCharsets.UTF_8).trim();
                if ("ANALOG".equalsIgnoreCase(val) || "DIGITAL".equalsIgnoreCase(val)) {
                    return val.toUpperCase();
                }
            }
        } catch (Exception ignored) {}
        return "DIGITAL";
    }

    public void saveStopwatchDisplayStyle(String style) {
        Path file = appDirectory.resolve("stopwatch_style.txt");
        try {
            Files.writeString(file, style, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
    }

    public List<String> getSubjectsSortedByUsage() {
        List<String> allSubjects = loadSubjects();
        List<SessionRecord> sessions = loadSessions();
        java.util.Map<String, Long> subjectUsage = new java.util.HashMap<>();
        for (SessionRecord session : sessions) {
            String sub = session.getSubject();
            subjectUsage.put(sub, subjectUsage.getOrDefault(sub, 0L) + session.getDurationSeconds());
        }

        List<String> sorted = new ArrayList<>(allSubjects);
        sorted.sort((a, b) -> {
            long usageA = subjectUsage.getOrDefault(a, 0L);
            long usageB = subjectUsage.getOrDefault(b, 0L);
            if (usageA != usageB) {
                return Long.compare(usageB, usageA); // Most used first
            }
            return a.compareToIgnoreCase(b);
        });
        return sorted;
    }

    public synchronized List<ReminderItem> loadReminders() {
        List<ReminderItem> list = new ArrayList<>();
        if (!Files.exists(remindersFile)) {
            return list;
        }

        try {
            String content = Files.readString(remindersFile, StandardCharsets.UTF_8).trim();
            if (content.isEmpty() || !content.startsWith("[") || !content.endsWith("]")) {
                return list;
            }

            // Remove outer brackets
            String inner = content.substring(1, content.length() - 1).trim();
            if (inner.isEmpty()) {
                return list;
            }

            // Parse simple JSON objects
            List<String> objects = splitJsonObjects(inner);
            for (String objJson : objects) {
                ReminderItem item = parseReminderJson(objJson);
                if (item != null) {
                    list.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading reminders: " + e.getMessage());
        }
        return list;
    }

    public synchronized void saveReminders(List<ReminderItem> reminders) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < reminders.size(); i++) {
            ReminderItem item = reminders.get(i);
            sb.append("  ").append(serializeReminderJson(item));
            if (i < reminders.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");

        try {
            Files.writeString(remindersFile, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save reminders", e);
        }
    }

    private List<String> splitJsonObjects(String json) {
        List<String> list = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inQuotes = false;
        boolean escape = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        list.add(json.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }
        return list;
    }

    private ReminderItem parseReminderJson(String json) {
        try {
            ReminderItem item = new ReminderItem();
            java.util.Map<String, String> map = new java.util.HashMap<>();
            // Extract key-value pairs
            int len = json.length();
            int i = 0;
            while (i < len) {
                int keyStart = json.indexOf('"', i);
                if (keyStart == -1) break;
                int keyEnd = json.indexOf('"', keyStart + 1);
                if (keyEnd == -1) break;
                String key = json.substring(keyStart + 1, keyEnd);
                
                int colon = json.indexOf(':', keyEnd + 1);
                if (colon == -1) break;
                
                // Find start of value
                int valStart = colon + 1;
                while (valStart < len && Character.isWhitespace(json.charAt(valStart))) valStart++;
                if (valStart >= len) break;
                
                String val;
                if (json.charAt(valStart) == '"') {
                    StringBuilder valBuf = new StringBuilder();
                    int cur = valStart + 1;
                    boolean esc = false;
                    while (cur < len) {
                        char c = json.charAt(cur);
                        if (esc) {
                            if (c == 'n') valBuf.append('\n');
                            else if (c == 't') valBuf.append('\t');
                            else if (c == 'r') valBuf.append('\r');
                            else valBuf.append(c);
                            esc = false;
                        } else if (c == '\\') {
                            esc = true;
                        } else if (c == '"') {
                            break;
                        } else {
                            valBuf.append(c);
                        }
                        cur++;
                    }
                    val = valBuf.toString();
                    i = cur + 1;
                } else {
                    int valEnd = valStart;
                    while (valEnd < len && json.charAt(valEnd) != ',' && json.charAt(valEnd) != '}') {
                        valEnd++;
                    }
                    val = json.substring(valStart, valEnd).trim();
                    i = valEnd;
                }
                map.put(key, val);
            }

            if (map.containsKey("id")) item.setId(map.get("id"));
            if (map.containsKey("title")) item.setTitle(map.get("title"));
            if (map.containsKey("notes")) item.setNotes(map.get("notes"));
            if (map.containsKey("dueDate") && !map.get("dueDate").isEmpty() && !"null".equalsIgnoreCase(map.get("dueDate"))) {
                item.setDueDate(java.time.LocalDate.parse(map.get("dueDate")));
            }
            if (map.containsKey("dueTime") && !map.get("dueTime").isEmpty() && !"null".equalsIgnoreCase(map.get("dueTime"))) {
                item.setDueTime(java.time.LocalTime.parse(map.get("dueTime")));
            }
            if (map.containsKey("period")) {
                item.setPeriod(ReminderItem.TimePeriod.fromString(map.get("period")));
            }
            if (map.containsKey("subject")) {
                item.setSubject(map.get("subject"));
            }
            if (map.containsKey("priority")) {
                item.setPriority(ReminderItem.Priority.fromString(map.get("priority")));
            }
            if (map.containsKey("flagged")) {
                item.setFlagged(Boolean.parseBoolean(map.get("flagged")));
            }
            if (map.containsKey("completed")) {
                item.setCompleted(Boolean.parseBoolean(map.get("completed")));
            }
            if (map.containsKey("completedAt") && !map.get("completedAt").isEmpty() && !"null".equalsIgnoreCase(map.get("completedAt"))) {
                item.setCompletedAt(java.time.LocalDateTime.parse(map.get("completedAt")));
            }
            if (map.containsKey("createdAt") && !map.get("createdAt").isEmpty() && !"null".equalsIgnoreCase(map.get("createdAt"))) {
                item.setCreatedAt(java.time.LocalDateTime.parse(map.get("createdAt")));
            }
            if (map.containsKey("notified")) {
                item.setNotified(Boolean.parseBoolean(map.get("notified")));
            }

            return item;
        } catch (Exception e) {
            System.err.println("Error parsing reminder JSON: " + e.getMessage());
            return null;
        }
    }

    private String serializeReminderJson(ReminderItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(item.getId())).append("\",");
        sb.append("\"title\":\"").append(escapeJson(item.getTitle())).append("\",");
        sb.append("\"notes\":\"").append(escapeJson(item.getNotes())).append("\",");
        sb.append("\"dueDate\":").append(item.getDueDate() == null ? "null" : "\"" + item.getDueDate() + "\"").append(",");
        sb.append("\"dueTime\":").append(item.getDueTime() == null ? "null" : "\"" + item.getDueTime() + "\"").append(",");
        sb.append("\"period\":\"").append(item.getPeriod() == null ? "ALL_DAY" : item.getPeriod().name()).append("\",");
        sb.append("\"subject\":\"").append(escapeJson(item.getSubject())).append("\",");
        sb.append("\"priority\":\"").append(item.getPriority() == null ? "NONE" : item.getPriority().name()).append("\",");
        sb.append("\"flagged\":").append(item.isFlagged()).append(",");
        sb.append("\"completed\":").append(item.isCompleted()).append(",");
        sb.append("\"completedAt\":").append(item.getCompletedAt() == null ? "null" : "\"" + item.getCompletedAt() + "\"").append(",");
        sb.append("\"createdAt\":").append(item.getCreatedAt() == null ? "null" : "\"" + item.getCreatedAt() + "\"").append(",");
        sb.append("\"notified\":").append(item.isNotified());
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
