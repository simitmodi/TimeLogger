package com.timelogger.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.timelogger.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class WebServer {
    private final int port;
    private final StorageService storageService;
    private final ExportService exportService;
    private HttpServer server;
    private volatile boolean running = false;
    private final Runnable onSwitchToDesktop;
    private final Runnable onExitApp;

    public WebServer(int port, StorageService storageService, ExportService exportService, Runnable onSwitchToDesktop, Runnable onExitApp) {
        this.port = port;
        this.storageService = storageService;
        this.exportService = exportService;
        this.onSwitchToDesktop = onSwitchToDesktop;
        this.onExitApp = onExitApp;
    }

    public synchronized void start() throws IOException {
        if (running) return;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Static routes
        server.createContext("/", new StaticHandler());

        // API routes
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/subjects", new SubjectsHandler());
        server.createContext("/api/question-topics", new QuestionTopicsHandler());
        server.createContext("/api/sessions", new SessionsHandler());
        server.createContext("/api/active-session", new ActiveSessionHandler());
        server.createContext("/api/goal", new GoalHandler());
        server.createContext("/api/theme", new ThemeHandler());
        server.createContext("/api/settings", new SettingsHandler());
        server.createContext("/api/insights", new InsightsHandler());
        server.createContext("/api/export", new ExportHandler());
        server.createContext("/api/switch-mode", new SwitchModeHandler());
        server.createContext("/api/exit", new ExitHandler());
        server.createContext("/api/ai/key", new AiKeyHandler());
        server.createContext("/api/ai/model", new AiModelHandler());
        server.createContext("/api/ai/chat", new AiChatHandler());
        server.createContext("/api/ai/clear-chat", new AiClearChatHandler());

        server.start();
        running = true;
    }

    public synchronized void stop() {
        if (!running) return;
        if (server != null) {
            server.stop(0);
        }
        running = false;
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }

    // --- Static Handler ---
    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Ensure safe path
            if (path.contains("..")) {
                sendResponse(exchange, 403, "text/plain", "Forbidden");
                return;
            }

            String resourcePath = "/web" + path;
            InputStream is = WebServer.class.getResourceAsStream(resourcePath);
            if (is == null) {
                // Try from local directory if running unbundled
                Path localFile = storageService.getAppDirectory().resolve("src/main/resources/web" + path);
                if (Files.exists(localFile)) {
                    is = Files.newInputStream(localFile);
                }
            }

            if (is == null) {
                sendResponse(exchange, 404, "text/plain", "Not Found");
                return;
            }

            String mimeType = "text/plain";
            if (path.endsWith(".html")) mimeType = "text/html; charset=utf-8";
            else if (path.endsWith(".css")) mimeType = "text/css; charset=utf-8";
            else if (path.endsWith(".js")) mimeType = "application/javascript; charset=utf-8";
            else if (path.endsWith(".png")) mimeType = "image/png";
            else if (path.endsWith(".ico")) mimeType = "image/x-icon";
            else if (path.endsWith(".svg")) mimeType = "image/svg+xml";

            byte[] bytes;
            try (InputStream stream = is) {
                bytes = stream.readAllBytes();
            }

            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // --- Status Handler ---
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"ok\",\"mode\":\"browser\",\"port\":" + port + "}");
        }
    }

    // --- Subjects Handler ---
    private class SubjectsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                List<String> list = storageService.getSubjectsSortedByUsage();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    json.append("\"").append(escapeJson(list.get(i))).append("\"");
                    if (i < list.size() - 1) json.append(",");
                }
                json.append("]");
                sendJson(exchange, 200, json.toString());
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> data = parseJsonSimple(body);
                String name = data.get("name");
                if (name != null && !name.trim().isEmpty()) {
                    name = name.trim();
                    List<String> list = new ArrayList<>(storageService.loadSubjects());
                    if (!list.contains(name)) {
                        list.add(name);
                        storageService.saveSubjects(list);
                    }
                    sendJson(exchange, 200, "{\"success\":true}");
                } else {
                    sendJson(exchange, 400, "{\"error\":\"Subject name is required\"}");
                }
            } else if ("DELETE".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                String name = params.get("name");
                if (name != null && !name.trim().isEmpty()) {
                    List<String> list = new ArrayList<>(storageService.loadSubjects());
                    list.remove(name.trim());
                    storageService.saveSubjects(list);
                    sendJson(exchange, 200, "{\"success\":true}");
                } else {
                    sendJson(exchange, 400, "{\"error\":\"Subject name is required\"}");
                }
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    // --- Question Topics Handler ---
    private class QuestionTopicsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                String qType = params.getOrDefault("type", "DPP Questions");
                String subject = params.getOrDefault("subject", "");
                List<String> topics = storageService.loadQuestionTopics(qType, subject);
                String lastDesc = storageService.loadLastQuestionDesc(qType, subject);

                StringBuilder json = new StringBuilder("{\"topics\":[");
                for (int i = 0; i < topics.size(); i++) {
                    json.append("\"").append(escapeJson(topics.get(i))).append("\"");
                    if (i < topics.size() - 1) json.append(",");
                }
                json.append("],\"lastDesc\":\"").append(escapeJson(lastDesc)).append("\"}");
                sendJson(exchange, 200, json.toString());
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> data = parseJsonSimple(body);
                String qType = data.get("type");
                String subject = data.get("subject");
                String topic = data.get("topic");
                if (qType != null && subject != null && topic != null && !topic.trim().isEmpty()) {
                    topic = topic.trim();
                    List<String> topics = new ArrayList<>(storageService.loadQuestionTopics(qType, subject));
                    if (!topics.contains(topic)) {
                        topics.add(topic);
                        storageService.saveQuestionTopics(qType, subject, topics);
                    }
                    storageService.saveLastQuestionDesc(qType, subject, topic);
                    sendJson(exchange, 200, "{\"success\":true}");
                } else {
                    sendJson(exchange, 400, "{\"error\":\"Missing required parameters\"}");
                }
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    // --- Sessions Handler ---
    private class SessionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                List<SessionRecord> list = storageService.loadSessions();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    SessionRecord s = list.get(i);
                    json.append(sessionRecordToJson(s, i));
                    if (i < list.size() - 1) json.append(",");
                }
                json.append("]");
                sendJson(exchange, 200, json.toString());
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> d = parseJsonSimple(body);
                try {
                    SessionRecord.SessionType type = SessionRecord.SessionType.valueOf(d.getOrDefault("type", "STOPWATCH"));
                    String subject = d.getOrDefault("subject", "General");
                    String startStr = d.get("startTime");
                    String endStr = d.get("endTime");
                    LocalDateTime start = startStr != null ? LocalDateTime.parse(startStr) : LocalDateTime.now();
                    LocalDateTime end = endStr != null ? LocalDateTime.parse(endStr) : LocalDateTime.now();
                    long duration = Long.parseLong(d.getOrDefault("durationSeconds", "0"));
                    String desc = d.getOrDefault("description", "");
                    int pauses = Integer.parseInt(d.getOrDefault("pauseCount", "0"));
                    int questions = Integer.parseInt(d.getOrDefault("questionsSolved", "0"));

                    SessionRecord rec = new SessionRecord(type, subject, start, end, duration, desc, pauses, questions);
                    storageService.appendSession(rec);
                    storageService.clearActiveSessionState();
                    sendJson(exchange, 200, "{\"success\":true}");
                } catch (Exception e) {
                    sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else if ("PUT".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> d = parseJsonSimple(body);
                try {
                    int index = Integer.parseInt(d.getOrDefault("index", "-1"));
                    List<SessionRecord> all = new ArrayList<>(storageService.loadSessions());
                    if (index >= 0 && index < all.size()) {
                        SessionRecord current = all.get(index);
                        SessionRecord.SessionType type = d.containsKey("type") ? SessionRecord.SessionType.valueOf(d.get("type")) : current.getType();
                        String subject = d.getOrDefault("subject", current.getSubject());
                        LocalDateTime start = d.containsKey("startTime") ? LocalDateTime.parse(d.get("startTime")) : current.getStartTime();
                        LocalDateTime end = d.containsKey("endTime") ? LocalDateTime.parse(d.get("endTime")) : current.getEndTime();
                        long duration = d.containsKey("durationSeconds") ? Long.parseLong(d.get("durationSeconds")) : current.getDurationSeconds();
                        String desc = d.getOrDefault("description", current.getDescription());
                        int pauses = d.containsKey("pauseCount") ? Integer.parseInt(d.get("pauseCount")) : current.getPauseCount();
                        int questions = d.containsKey("questionsSolved") ? Integer.parseInt(d.get("questionsSolved")) : current.getQuestionsSolved();

                        SessionRecord updated = new SessionRecord(type, subject, start, end, duration, desc, pauses, questions);
                        all.set(index, updated);
                        storageService.saveSessions(all);
                        sendJson(exchange, 200, "{\"success\":true}");
                        return;
                    }
                    sendJson(exchange, 400, "{\"error\":\"Invalid session index\"}");
                } catch (Exception e) {
                    sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else if ("DELETE".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                if ("true".equalsIgnoreCase(params.get("all"))) {
                    storageService.saveSessions(new ArrayList<>());
                    sendJson(exchange, 200, "{\"success\":true}");
                    return;
                }
                String idxStr = params.get("index");
                if (idxStr != null) {
                    try {
                        int index = Integer.parseInt(idxStr);
                        List<SessionRecord> all = new ArrayList<>(storageService.loadSessions());
                        if (index >= 0 && index < all.size()) {
                            all.remove(index);
                            storageService.saveSessions(all);
                            sendJson(exchange, 200, "{\"success\":true}");
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                sendJson(exchange, 400, "{\"error\":\"Invalid index\"}");
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    // --- Active Session Handler ---
    private class ActiveSessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                ActiveSessionState state = storageService.loadActiveSessionState();
                if (state == null) {
                    sendJson(exchange, 200, "null");
                } else {
                    StringBuilder json = new StringBuilder("{");
                    json.append("\"mode\":\"").append(state.getMode().name()).append("\",");
                    json.append("\"subject\":\"").append(escapeJson(state.getSubject())).append("\",");
                    json.append("\"sessionStart\":\"").append(state.getSessionStart().toString()).append("\",");
                    json.append("\"elapsedSeconds\":").append(state.getElapsedSeconds()).append(",");
                    json.append("\"pauseCount\":").append(state.getPauseCount()).append(",");
                    json.append("\"isRunning\":").append(state.isRunning()).append(",");
                    json.append("\"activityType\":\"").append(escapeJson(state.getActivityType())).append("\",");
                    json.append("\"activityDetail\":\"").append(escapeJson(state.getActivityDetail())).append("\",");
                    json.append("\"timerTotalSeconds\":").append(state.getTimerTotalSeconds()).append(",");
                    json.append("\"timerRemainingSeconds\":").append(state.getTimerRemainingSeconds()).append(",");
                    json.append("\"lastHeartbeat\":\"").append(state.getLastHeartbeat().toString()).append("\"");
                    json.append("}");
                    sendJson(exchange, 200, json.toString());
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> d = parseJsonSimple(body);
                try {
                    ActiveSessionState.SessionMode mode = ActiveSessionState.SessionMode.valueOf(d.getOrDefault("mode", "STOPWATCH"));
                    String subject = d.getOrDefault("subject", "General");
                    String startStr = d.get("sessionStart");
                    LocalDateTime start = startStr != null ? LocalDateTime.parse(startStr) : LocalDateTime.now();
                    long elapsed = Long.parseLong(d.getOrDefault("elapsedSeconds", "0"));
                    int pauses = Integer.parseInt(d.getOrDefault("pauseCount", "0"));
                    boolean isRunning = Boolean.parseBoolean(d.getOrDefault("isRunning", "false"));
                    String actType = d.getOrDefault("activityType", "General");
                    String actDetail = d.getOrDefault("activityDetail", "");
                    long timerTotal = Long.parseLong(d.getOrDefault("timerTotalSeconds", "0"));
                    long timerRem = Long.parseLong(d.getOrDefault("timerRemainingSeconds", "0"));

                    ActiveSessionState state = new ActiveSessionState(
                        mode, subject, start, elapsed, pauses, isRunning, actType, actDetail, timerTotal, timerRem, LocalDateTime.now()
                    );
                    storageService.saveActiveSessionState(state);
                    sendJson(exchange, 200, "{\"success\":true}");
                } catch (Exception e) {
                    sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else if ("DELETE".equals(method)) {
                storageService.clearActiveSessionState();
                sendJson(exchange, 200, "{\"success\":true}");
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    // --- Goal Handler ---
    private class GoalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                int goal = storageService.loadDailyGoalMinutes();
                sendJson(exchange, 200, "{\"dailyGoalMinutes\":" + goal + "}");
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> d = parseJsonSimple(body);
                try {
                    int goal = Integer.parseInt(d.getOrDefault("dailyGoalMinutes", "120"));
                    storageService.saveDailyGoalMinutes(goal);
                    sendJson(exchange, 200, "{\"success\":true}");
                } catch (Exception e) {
                    sendJson(exchange, 400, "{\"error\":\"Invalid goal\"}");
                }
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    // --- Theme Handler ---
    private class ThemeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                ThemeManager.AppTheme theme = ThemeManager.loadTheme();
                sendJson(exchange, 200, "{\"theme\":\"" + theme.name() + "\"}");
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> d = parseJsonSimple(body);
                try {
                    ThemeManager.AppTheme theme = ThemeManager.AppTheme.valueOf(d.getOrDefault("theme", "LIGHT"));
                    ThemeManager.saveTheme(theme);
                    sendJson(exchange, 200, "{\"success\":true}");
                } catch (Exception e) {
                    sendJson(exchange, 400, "{\"error\":\"Invalid theme\"}");
                }
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    // --- Settings Handler ---
    private class SettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                boolean notif = storageService.loadNotificationEnabled();
                boolean breakRem = storageService.loadBreakRemindersEnabled();
                String prefMode = storageService.loadPreferredLaunchMode();
                sendJson(exchange, 200, "{\"notificationsEnabled\":" + notif + ",\"breakRemindersEnabled\":" + breakRem + ",\"preferredLaunchMode\":\"" + escapeJson(prefMode) + "\"}");
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> d = parseJsonSimple(body);
                if (d.containsKey("notificationsEnabled")) {
                    storageService.saveNotificationEnabled(Boolean.parseBoolean(d.get("notificationsEnabled")));
                }
                if (d.containsKey("breakRemindersEnabled")) {
                    storageService.saveBreakRemindersEnabled(Boolean.parseBoolean(d.get("breakRemindersEnabled")));
                }
                if (d.containsKey("preferredLaunchMode")) {
                    storageService.savePreferredLaunchMode(d.get("preferredLaunchMode"));
                }
                sendJson(exchange, 200, "{\"success\":true}");
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    // --- Insights Handler ---
    private class InsightsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            List<SessionRecord> allSessions = storageService.loadSessions();
            LocalDate today = LocalDate.now();
            LocalDate sevenDaysAgo = today.minusDays(6);

            List<SessionRecord> recentSessions = allSessions.stream()
                .filter(s -> !s.getStartTime().toLocalDate().isBefore(sevenDaysAgo) && !s.getStartTime().toLocalDate().isAfter(today))
                .collect(Collectors.toList());

            double totalHours = recentSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum() / 3600.0;
            Set<LocalDate> activeDates = recentSessions.stream().map(s -> s.getStartTime().toLocalDate()).collect(Collectors.toSet());
            int activeDaysCount = activeDates.size();

            int consecutiveActiveDays = 0;
            LocalDate checkDate = today;
            if (!activeDates.contains(today)) checkDate = today.minusDays(1);
            while (activeDates.contains(checkDate)) {
                consecutiveActiveDays++;
                checkDate = checkDate.minusDays(1);
            }

            int lateNightSessions = 0;
            for (SessionRecord s : recentSessions) {
                int sh = s.getStartTime().getHour();
                int eh = s.getEndTime().getHour();
                if (sh >= 23 || sh < 5 || eh >= 23 || eh < 5) lateNightSessions++;
            }

            double totalBreakRatioSum = 0;
            int activeDaysWithSessions = 0;
            Map<LocalDate, List<SessionRecord>> sessionsByDate = recentSessions.stream()
                .collect(Collectors.groupingBy(s -> s.getStartTime().toLocalDate()));

            for (Map.Entry<LocalDate, List<SessionRecord>> entry : sessionsByDate.entrySet()) {
                List<SessionRecord> dailySessions = new ArrayList<>(entry.getValue());
                dailySessions.sort(Comparator.comparing(SessionRecord::getStartTime));
                long activeSeconds = dailySessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
                long breakSeconds = 0;
                for (int i = 0; i < dailySessions.size() - 1; i++) {
                    LocalDateTime endPrev = dailySessions.get(i).getEndTime();
                    LocalDateTime startNext = dailySessions.get(i + 1).getStartTime();
                    long gap = Duration.between(endPrev, startNext).toSeconds();
                    if (gap > 0) breakSeconds += gap;
                }
                if (activeSeconds > 0) {
                    totalBreakRatioSum += (double) breakSeconds / activeSeconds;
                    activeDaysWithSessions++;
                }
            }

            double avgBreakRatio = activeDaysWithSessions > 0 ? (totalBreakRatioSum / activeDaysWithSessions) : 1.0;
            double burnoutScore = totalHours * 1.5;
            if (activeDaysCount > 0 && avgBreakRatio < 0.15) burnoutScore += 20.0;
            int riskPercent = (int) Math.max(0, Math.min(100, burnoutScore));

            double dailyGoalHrs = storageService.loadDailyGoalMinutes() / 60.0;
            double targetHours = dailyGoalHrs * 7.0;
            double goalAchievementRate = targetHours > 0 ? Math.min(1.0, totalHours / targetHours) : 1.0;
            double pacingFactor = 1.0 - (riskPercent / 200.0);
            int prodIndex = (int) (goalAchievementRate * pacingFactor * 100.0);

            long totalSpanSeconds = 0;
            long totalActiveSeconds = 0;
            for (SessionRecord s : recentSessions) {
                totalSpanSeconds += Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
                totalActiveSeconds += s.getDurationSeconds();
            }
            long totalMidSessionBreaks = Math.max(0, totalSpanSeconds - totalActiveSeconds);
            double efficiencyVal = totalSpanSeconds > 0 ? ((double) totalActiveSeconds / totalSpanSeconds) * 100.0 : 100.0;

            double avgAttentionSpanMin = 0;
            double avgFocusScore = 0;
            if (!recentSessions.isEmpty()) {
                avgFocusScore = recentSessions.stream().mapToInt(WebServer.this::calculateSessionFocusScore).average().orElse(0.0);
                avgAttentionSpanMin = recentSessions.stream().mapToDouble(s -> {
                    long activeSec = s.getDurationSeconds();
                    int pauses = getSessionPauseCount(s);
                    double avgBlockMin = (activeSec / (double) (pauses + 1)) / 60.0;
                    double[] targets = getSessionOptimalMinTargets(s);
                    return avgBlockMin * (35.0 / targets[0]);
                }).average().orElse(0.0);
            }

            int recentXpGained = (int) Math.round(totalActiveSeconds / 60.0);
            int recentXpDeducted = (int) Math.round(totalMidSessionBreaks / 60.0 * 0.25);
            int recentNetXp = recentXpGained - recentXpDeducted;
            double avgRecentXpPerDay = activeDaysCount > 0 ? (double) recentNetXp / activeDaysCount : 0.0;

            StringBuilder json = new StringBuilder("{");
            json.append("\"burnoutRiskPercent\":").append(riskPercent).append(",");
            json.append("\"productivityIndex\":").append(prodIndex).append(",");
            json.append("\"totalHours\":").append(String.format(Locale.US, "%.1f", totalHours)).append(",");
            json.append("\"consecutiveActiveDays\":").append(consecutiveActiveDays).append(",");
            json.append("\"lateNightSessions\":").append(lateNightSessions).append(",");
            json.append("\"avgBreakRatioPercent\":").append(String.format(Locale.US, "%.1f", avgBreakRatio * 100)).append(",");
            json.append("\"midSessionBreakSeconds\":").append(totalMidSessionBreaks).append(",");
            json.append("\"efficiencyPercent\":").append(String.format(Locale.US, "%.1f", efficiencyVal)).append(",");
            json.append("\"attentionSpanMin\":").append(String.format(Locale.US, "%.1f", avgAttentionSpanMin)).append(",");
            json.append("\"focusScore\":").append((int) avgFocusScore).append(",");
            json.append("\"totalNetXp\":").append(recentNetXp).append(",");
            json.append("\"avgXpPerDay\":").append(String.format(Locale.US, "%.1f", avgRecentXpPerDay));
            json.append("}");

            sendJson(exchange, 200, json.toString());
        }
    }

    // --- Export Handler ---
    private class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);
            String period = params.getOrDefault("period", "weekly");

            List<SessionRecord> all = storageService.loadSessions();
            LocalDate today = LocalDate.now();
            List<SessionRecord> toExport;
            String reportTitle;

            if ("monthly".equalsIgnoreCase(period)) {
                LocalDate startMonth = today.withDayOfMonth(1);
                toExport = all.stream().filter(s -> !s.getStartTime().toLocalDate().isBefore(startMonth)).collect(Collectors.toList());
                reportTitle = "Monthly Report - " + today.getMonth().name() + " " + today.getYear();
            } else if ("custom".equalsIgnoreCase(period)) {
                String startStr = params.get("start");
                String endStr = params.get("end");
                LocalDate startD = (startStr != null && !startStr.isEmpty()) ? LocalDate.parse(startStr) : today.minusDays(7);
                LocalDate endD = (endStr != null && !endStr.isEmpty()) ? LocalDate.parse(endStr) : today;
                toExport = all.stream().filter(s -> {
                    LocalDate d = s.getStartTime().toLocalDate();
                    return !d.isBefore(startD) && !d.isAfter(endD);
                }).collect(Collectors.toList());
                reportTitle = "Custom Range Report (" + startD + " to " + endD + ")";
            } else {
                LocalDate startWeek = today.minusDays(6);
                toExport = all.stream().filter(s -> !s.getStartTime().toLocalDate().isBefore(startWeek)).collect(Collectors.toList());
                reportTitle = "Weekly Report (Last 7 Days)";
            }

            Path tempXlsx = Files.createTempFile("timelogger_export_", ".xlsx");
            try {
                exportService.exportReport(tempXlsx, toExport, reportTitle);
                byte[] bytes = Files.readAllBytes(tempXlsx);
                String filename = "TimeLogger_" + period.toLowerCase() + "_" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

                exchange.getResponseHeaders().set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } finally {
                try {
                    Files.deleteIfExists(tempXlsx);
                } catch (Exception ignored) {}
            }
        }
    }

    // --- Switch Mode Handler ---
    private class SwitchModeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            sendJson(exchange, 200, "{\"success\":true}");
            if (onSwitchToDesktop != null) {
                new Thread(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    onSwitchToDesktop.run();
                }).start();
            }
        }
    }

    // --- AI Key Handler ---
    private class AiKeyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String key = storageService.loadOpenRouterApiKey();
                boolean hasKey = key != null && !key.trim().isEmpty();
                sendJson(exchange, 200, "{\"hasKey\":" + hasKey + "}");
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readBody(exchange));
                String key = body.getOrDefault("key", "").trim();
                storageService.saveOpenRouterApiKey(key);
                sendJson(exchange, 200, "{\"success\":true}");
            } else {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    // --- AI Model Handler ---
    private class AiModelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String model = storageService.loadOpenRouterModel();
                sendJson(exchange, 200, "{\"model\":\"" + escapeJson(model) + "\"}");
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readBody(exchange));
                String model = body.getOrDefault("model", "").trim();
                if (!model.isEmpty()) {
                    storageService.saveOpenRouterModel(model);
                }
                sendJson(exchange, 200, "{\"success\":true}");
            } else {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    // --- AI Clear Chat Handler ---
    private class AiClearChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Path file = Paths.get(System.getProperty("user.home"), ".timelogger_chat_history");
                try {
                    Files.deleteIfExists(file);
                } catch (Exception ignored) {}
                sendJson(exchange, 200, "{\"success\":true}");
            } else {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    // --- AI Chat Handler ---
    private class AiChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Return chat history
                List<ChatTurn> history = loadChatHistory();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < history.size(); i++) {
                    ChatTurn turn = history.get(i);
                    if (i > 0) json.append(",");
                    json.append("{\"role\":\"").append(escapeJson(turn.role)).append("\",\"content\":\"")
                        .append(escapeJson(turn.text)).append("\"}");
                }
                json.append("]");
                sendJson(exchange, 200, json.toString());
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readBody(exchange));
                String message = body.getOrDefault("message", "").trim();
                if (message.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"Message cannot be empty\"}");
                    return;
                }

                String apiKey = storageService.loadOpenRouterApiKey();
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"OpenRouter API Key not configured. Please set your API key first.\"}");
                    return;
                }

                List<ChatTurn> history = loadChatHistory();
                history.add(new ChatTurn("user", message));
                saveChatHistory(history);

                String selectedModel = storageService.loadOpenRouterModel();
                String systemContext = compileAIPromptContext();

                try {
                    StringBuilder reqJson = new StringBuilder();
                    reqJson.append("{\n");
                    reqJson.append("  \"model\": \"").append(escapeJson(selectedModel)).append("\",\n");
                    reqJson.append("  \"messages\": [\n");
                    reqJson.append("    {\"role\": \"system\", \"content\": \"").append(escapeJson(systemContext)).append("\"}");
                    for (ChatTurn turn : history) {
                        reqJson.append(",\n");
                        reqJson.append("    {\"role\": \"").append(escapeJson(turn.role)).append("\", \"content\": \"").append(escapeJson(turn.text)).append("\"}");
                    }
                    reqJson.append("\n  ]\n");
                    reqJson.append("}");

                    java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .build();

                    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://openrouter.ai/api/v1/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("HTTP-Referer", "http://localhost")
                        .header("X-Title", "Time Logger")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(reqJson.toString(), StandardCharsets.UTF_8))
                        .build();

                    java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        String aiText = parseOpenRouterResponseText(response.body());
                        history.add(new ChatTurn("assistant", aiText));
                        saveChatHistory(history);
                        sendJson(exchange, 200, "{\"success\":true,\"reply\":\"" + escapeJson(aiText) + "\"}");
                    } else {
                        String errMsg = "API error " + response.statusCode();
                        String parsedErr = parseOpenRouterResponseText(response.body());
                        if (parsedErr != null && !parsedErr.isEmpty()) {
                            errMsg = parsedErr;
                        }
                        sendJson(exchange, 502, "{\"error\":\"" + escapeJson(errMsg) + "\"}");
                    }
                } catch (Exception ex) {
                    sendJson(exchange, 500, "{\"error\":\"" + escapeJson("Connection error: " + ex.getMessage()) + "\"}");
                }
            } else {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    private static class ChatTurn {
        final String role;
        final String text;
        ChatTurn(String role, String text) {
            this.role = role;
            this.text = text;
        }
    }

    private static List<ChatTurn> loadChatHistory() {
        List<ChatTurn> history = new ArrayList<>();
        Path file = Paths.get(System.getProperty("user.home"), ".timelogger_chat_history");
        try {
            if (!Files.exists(file)) return history;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                int colonIdx = line.indexOf(':');
                if (colonIdx != -1) {
                    String role = line.substring(0, colonIdx);
                    String encoded = line.substring(colonIdx + 1);
                    byte[] decoded = Base64.getDecoder().decode(encoded);
                    String text = new String(decoded, StandardCharsets.UTF_8);
                    if ("assistant".equals(role)) {
                        text = text.replaceAll("(?i)\\b(\\w+n)['\u2019](?:s|re|or)\\b", "$1't");
                    }
                    history.add(new ChatTurn(role, text));
                }
            }
        } catch (Exception ignored) {}
        return history;
    }

    private static void saveChatHistory(List<ChatTurn> history) {
        Path file = Paths.get(System.getProperty("user.home"), ".timelogger_chat_history");
        try {
            List<String> lines = new ArrayList<>();
            for (ChatTurn turn : history) {
                String encoded = Base64.getEncoder().encodeToString(turn.text.getBytes(StandardCharsets.UTF_8));
                lines.add(turn.role + ":" + encoded);
            }
            Files.write(file, lines, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
    }

    private String parseOpenRouterResponseText(String json) {
        if (json == null) return "";
        int choicesIdx = json.indexOf("\"choices\"");
        if (choicesIdx == -1) {
            int errorIdx = json.indexOf("\"error\"");
            if (errorIdx != -1) {
                int msgIdx = json.indexOf("\"message\"", errorIdx);
                if (msgIdx != -1) {
                    int cStart = json.indexOf("\"", msgIdx + 9);
                    if (cStart != -1) {
                        int cEnd = json.indexOf("\"", cStart + 1);
                        while (cEnd != -1 && json.charAt(cEnd - 1) == '\\') {
                            cEnd = json.indexOf("\"", cEnd + 1);
                        }
                        if (cEnd != -1) {
                            return "API Error: " + unescapeJsonSimple(json.substring(cStart + 1, cEnd));
                        }
                    }
                }
            }
            return "Error: Unexpected API response format.";
        }

        int contentIdx = json.indexOf("\"content\"", choicesIdx);
        if (contentIdx == -1) return "Error: Could not find content in choices.";

        int colonIdx = json.indexOf(":", contentIdx);
        if (colonIdx == -1) return "Error: Invalid JSON response format.";

        int valStart = json.indexOf("\"", colonIdx);
        if (valStart == -1) return "Error: Invalid JSON value format.";

        int valEnd = json.indexOf("\"", valStart + 1);
        while (valEnd != -1 && json.charAt(valEnd - 1) == '\\') {
            valEnd = json.indexOf("\"", valEnd + 1);
        }
        if (valEnd == -1) return "Error: Unterminated string in JSON response.";

        String unescaped = unescapeJsonSimple(json.substring(valStart + 1, valEnd));
        return unescaped.replaceAll("(?i)\\b(\\w+n)['\u2019](?:s|re|or)\\b", "$1't");
    }

    private static String unescapeJsonSimple(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/': sb.append('/'); i++; break;
                    case 'b': sb.append('\b'); i++; break;
                    case 'f': sb.append('\f'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'u':
                        if (i + 5 < str.length()) {
                            try {
                                char uc = (char) Integer.parseInt(str.substring(i + 2, i + 6), 16);
                                sb.append(uc);
                                i += 5;
                            } catch (Exception e) {
                                sb.append('\\');
                            }
                        } else {
                            sb.append('\\');
                        }
                        break;
                    default:
                        sb.append('\\');
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private String compileAIPromptContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the AI Study Coach for Time Logger. Below is the user's complete local study and tracking data. ");
        sb.append("Use this data to answer their questions accurately, provide personalized coaching, identify habit trends, ");
        sb.append("and suggest productivity improvements. There are no privacy restrictions on this data; analyze all of it.\n\n");

        List<SessionRecord> allSessions = storageService.loadSessions();
        long allTimeSessions = allSessions.size();
        double allTimeHours = allSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum() / 3600.0;
        String firstSessionDate = allSessions.isEmpty() ? "N/A" : allSessions.stream().map(SessionRecord::getStartTime).min(Comparator.naturalOrder()).get().toLocalDate().toString();
        String lastSessionDate = allSessions.isEmpty() ? "N/A" : allSessions.stream().map(SessionRecord::getStartTime).max(Comparator.naturalOrder()).get().toLocalDate().toString();
        List<String> subjects = storageService.loadSubjects();

        long allTimeActiveSec = allSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
        long allTimeSpanSec = 0;
        for (SessionRecord s : allSessions) {
            allTimeSpanSec += Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
        }
        long allTimeMidSessionBreaks = Math.max(0, allTimeSpanSec - allTimeActiveSec);
        int allTimeXpGained = (int) Math.round(allTimeActiveSec / 60.0);
        int allTimeXpDeducted = (int) Math.round(allTimeMidSessionBreaks / 60.0 * 0.25);
        int allTimeNetXp = allTimeXpGained - allTimeXpDeducted;

        sb.append("### ALL-TIME SUMMARY & SYSTEM METRICS:\n");
        sb.append("- Total Logged Sessions (All-Time): ").append(allTimeSessions).append("\n");
        sb.append("- Total Tracked Hours (All-Time): ").append(String.format(Locale.US, "%.2f hrs\n", allTimeHours));
        sb.append("- Total Net XP (All-Time): ").append(String.format(Locale.US, "%d XP (Gained: +%d, Deducted: -%d)\n", allTimeNetXp, allTimeXpGained, allTimeXpDeducted));
        sb.append("- Tracking Active Period: From ").append(firstSessionDate).append(" to ").append(lastSessionDate).append("\n");
        sb.append("- Configured Subject List: ").append(String.join(", ", subjects)).append("\n\n");

        sb.append("### STUDY METRICS OVERVIEW (Last 7 Days):\n");
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);

        List<SessionRecord> recentSessions = allSessions.stream()
            .filter(s -> !s.getStartTime().toLocalDate().isBefore(sevenDaysAgo) && !s.getStartTime().toLocalDate().isAfter(today))
            .collect(Collectors.toList());

        double totalHours = recentSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum() / 3600.0;
        sb.append("- Total Hours Tracked: ").append(String.format(Locale.US, "%.2f hrs\n", totalHours));

        Set<LocalDate> activeDates = recentSessions.stream().map(s -> s.getStartTime().toLocalDate()).collect(Collectors.toSet());
        sb.append("- Active Days: ").append(activeDates.size()).append(" / 7 days\n");

        // Calculate Goal Streaks
        int dailyGoal = storageService.loadDailyGoalMinutes();
        Map<LocalDate, Long> secondsPerDay = new HashMap<>();
        for (SessionRecord s : allSessions) {
            LocalDate d = s.getStartTime().toLocalDate();
            secondsPerDay.put(d, secondsPerDay.getOrDefault(d, 0L) + s.getDurationSeconds());
        }
        int todayMins = (int) (secondsPerDay.getOrDefault(today, 0L) / 60);
        int currentStreak = 0;
        java.util.function.Predicate<LocalDate> metGoal = d -> (secondsPerDay.getOrDefault(d, 0L) / 60) >= dailyGoal;
        if (metGoal.test(today)) {
            LocalDate d = today;
            while (metGoal.test(d)) { currentStreak++; d = d.minusDays(1); }
        } else if (metGoal.test(today.minusDays(1))) {
            LocalDate d = today.minusDays(1);
            while (metGoal.test(d)) { currentStreak++; d = d.minusDays(1); }
        }

        int maxStreak = 0;
        int running = 0;
        LocalDate earliest = allSessions.stream().map(s -> s.getStartTime().toLocalDate()).min(LocalDate::compareTo).orElse(today);
        LocalDate dCheck = earliest;
        while (!dCheck.isAfter(today)) {
            if (metGoal.test(dCheck)) {
                running++;
                maxStreak = Math.max(maxStreak, running);
            } else {
                running = 0;
            }
            dCheck = dCheck.plusDays(1);
        }

        sb.append("- Current Streak: ").append(currentStreak).append(" days\n");
        sb.append("- Max Streak: ").append(maxStreak).append(" days\n");
        sb.append("- Today's Goal Progress: ").append(todayMins).append(" / ").append(dailyGoal).append(" mins (Goal Met: ").append(todayMins >= dailyGoal).append(")\n");

        // Burnout & Productivity
        double totalBreakRatioSum = 0;
        int activeDaysWithSessions = 0;
        Map<LocalDate, List<SessionRecord>> sessionsByDate = recentSessions.stream().collect(Collectors.groupingBy(s -> s.getStartTime().toLocalDate()));
        for (Map.Entry<LocalDate, List<SessionRecord>> entry : sessionsByDate.entrySet()) {
            List<SessionRecord> daily = new ArrayList<>(entry.getValue());
            daily.sort(Comparator.comparing(SessionRecord::getStartTime));
            long actSec = daily.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
            long brkSec = 0;
            for (int i = 0; i < daily.size() - 1; i++) {
                long gap = Duration.between(daily.get(i).getEndTime(), daily.get(i + 1).getStartTime()).toSeconds();
                if (gap > 0) brkSec += gap;
            }
            if (actSec > 0) {
                totalBreakRatioSum += (double) brkSec / actSec;
                activeDaysWithSessions++;
            }
        }
        double avgBreakRatio = activeDaysWithSessions > 0 ? (totalBreakRatioSum / activeDaysWithSessions) : 1.0;
        double burnoutScore = totalHours * 1.5;
        if (activeDates.size() > 0 && avgBreakRatio < 0.15) burnoutScore += 20.0;
        int riskPercent = (int) Math.max(0, Math.min(100, burnoutScore));
        sb.append("- Burnout Risk Score: ").append(riskPercent).append("%\n");

        double targetHours = (dailyGoal / 60.0) * 7.0;
        double goalAchievementRate = targetHours > 0 ? Math.min(1.0, totalHours / targetHours) : 1.0;
        double pacingFactor = 1.0 - (riskPercent / 200.0);
        int prodIndex = (int) (goalAchievementRate * pacingFactor * 100.0);
        sb.append("- Productivity Index: ").append(prodIndex).append(" / 100\n");

        double avgAttentionSpanMin = 0;
        double avgFocusScore = 0;
        if (!recentSessions.isEmpty()) {
            avgFocusScore = recentSessions.stream().mapToInt(this::calculateSessionFocusScore).average().orElse(0.0);
            avgAttentionSpanMin = recentSessions.stream().mapToDouble(s -> {
                long act = s.getDurationSeconds();
                int p = getSessionPauseCount(s);
                double blk = (act / (double) (p + 1)) / 60.0;
                double[] tgts = getSessionOptimalMinTargets(s);
                return blk * (35.0 / tgts[0]);
            }).average().orElse(0.0);
        }
        sb.append("- Avg Attention Span: ").append(String.format(Locale.US, "%.1f mins\n", avgAttentionSpanMin));
        sb.append("- Cognitive Focus Score: ").append(String.format(Locale.US, "%.1f / 100\n", avgFocusScore));

        long recentSpanSec = 0;
        for (SessionRecord s : recentSessions) {
            recentSpanSec += Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
        }
        long totalRecentActive = recentSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
        long recentMidBreaks = Math.max(0, recentSpanSec - totalRecentActive);
        int recentXpGained = (int) Math.round(totalRecentActive / 60.0);
        int recentXpDeducted = (int) Math.round(recentMidBreaks / 60.0 * 0.25);
        int recentNetXp = recentXpGained - recentXpDeducted;
        double avgRecentXp = activeDates.size() > 0 ? (double) recentNetXp / activeDates.size() : 0.0;
        sb.append("- 7-Day Net XP: ").append(String.format(Locale.US, "%d XP (Gained: +%d, Deducted: -%d)\n", recentNetXp, recentXpGained, recentXpDeducted));
        sb.append("- Avg XP / Active Day: ").append(String.format(Locale.US, "%.1f XP/day\n", avgRecentXp));

        sb.append("\n### SUBJECT & CHAPTER BREAKDOWN (Last 7 Days):\n");
        Map<String, Long> subjectDurations = new HashMap<>();
        for (SessionRecord s : recentSessions) {
            String subj = s.getSubject();
            subjectDurations.put(subj, subjectDurations.getOrDefault(subj, 0L) + s.getDurationSeconds());
        }
        for (Map.Entry<String, Long> entry : subjectDurations.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(formatSeconds(entry.getValue())).append("\n");
        }

        sb.append("\n### RECENT SESSION LOGS (Last 120 Records):\n");
        List<SessionRecord> sortedAll = new ArrayList<>(allSessions);
        sortedAll.sort((s1, s2) -> s2.getStartTime().compareTo(s1.getStartTime()));
        int limit = Math.min(120, sortedAll.size());
        for (int i = 0; i < limit; i++) {
            SessionRecord s = sortedAll.get(i);
            sb.append(String.format("- [%s] Subj: %s, Duration: %s, Type: %s, Desc: %s (Pauses: %d)\n",
                s.getStartTime().toLocalDate().toString(),
                s.getSubject(),
                formatSeconds(s.getDurationSeconds()),
                s.getType().name(),
                s.getDescription() != null ? s.getDescription() : "",
                s.getPauseCount()));
        }

        sb.append("\nWhen answering, respond concisely using markdown formatting. High contrast or bullet points are encouraged. Do not refer to internal implementation details unless asked.");
        return sb.toString();
    }

    private static String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // --- Exit Handler ---
    private class ExitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }
            sendJson(exchange, 200, "{\"success\":true}");
            if (onExitApp != null) {
                new Thread(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    onExitApp.run();
                }).start();
            }
        }
    }

    // --- Helper Focus / Targets Functions ---
    private int getSessionPauseCount(SessionRecord s) {
        if (s.getPauseCount() > 0) return s.getPauseCount();
        long span = Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
        long active = s.getDurationSeconds();
        long breakSec = Math.max(0, span - active);
        if (breakSec == 0) return 0;
        return (int) Math.max(1, breakSec / 300);
    }

    private double[] getSessionOptimalMinTargets(SessionRecord s) {
        String desc = s.getDescription() != null ? s.getDescription() : "";
        double minTarget = 25.0;
        double maxTarget = 120.0;
        if (desc.startsWith("Lecture: ")) {
            minTarget = 45.0;
            maxTarget = 150.0;
        } else if (desc.startsWith("Revision: ")) {
            minTarget = 20.0;
            maxTarget = 50.0;
        } else if (desc.startsWith("Questions: ")) {
            String lower = desc.toLowerCase();
            if (lower.contains("dpp") || lower.contains("daily practice") || lower.contains("solved: 1") || lower.contains("solved: 2") || lower.contains("solved: 3") || lower.contains("solved: 4") || lower.contains("solved: 5") || lower.contains("solved: 6") || lower.contains("solved: 7") || lower.contains("solved: 8") || lower.contains("solved: 9") || (lower.contains("solved: 1") && !lower.contains("solved: 16") && !lower.contains("solved: 17") && !lower.contains("solved: 18") && !lower.contains("solved: 19"))) {
                minTarget = 15.0;
                maxTarget = 40.0;
            } else {
                minTarget = 25.0;
                maxTarget = 75.0;
            }
        }
        return new double[]{minTarget, maxTarget};
    }

    private int calculateSessionFocusScore(SessionRecord s) {
        long activeSec = s.getDurationSeconds();
        if (activeSec < 60) return 100;
        double[] targets = getSessionOptimalMinTargets(s);
        double minTarget = targets[0];
        double maxTarget = targets[1];
        int pauses = getSessionPauseCount(s);
        double avgBlockMin = (activeSec / (double) (pauses + 1)) / 60.0;
        double blockScore;
        if (avgBlockMin >= minTarget && avgBlockMin <= maxTarget) {
            blockScore = 100;
        } else if (avgBlockMin < minTarget) {
            blockScore = Math.max(20, 100 - (minTarget - avgBlockMin) * (80.0 / minTarget));
        } else {
            blockScore = Math.max(50, 100 - (avgBlockMin - maxTarget) * 0.5);
        }
        long spanSec = Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
        double efficiency = spanSec > 0 ? ((double) activeSec / spanSec) * 100.0 : 100.0;
        double efficiencyScore = efficiency >= 80 ? 100 : Math.max(20, 100 - (80 - efficiency) * 1.5);
        double score = (blockScore * 0.6) + (efficiencyScore * 0.4);
        return (int) Math.max(0, Math.min(100, score));
    }

    // --- JSON & HTTP Utilities ---
    private static void handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    map.put(k, v);
                } else if (idx < 0) {
                    map.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    private static Map<String, String> parseJsonSimple(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        String trimmed = json.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        // Minimal regex-based JSON parser for flat key-value pairs
        boolean inQuote = false;
        StringBuilder currentKey = new StringBuilder();
        StringBuilder currentValue = new StringBuilder();
        boolean readingKey = true;
        boolean escapeNext = false;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (escapeNext) {
                if (readingKey) currentKey.append(c);
                else currentValue.append(c);
                escapeNext = false;
                continue;
            }
            if (c == '\\') {
                escapeNext = true;
                continue;
            }
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (!inQuote) {
                if (c == ':') {
                    readingKey = false;
                    continue;
                }
                if (c == ',') {
                    String k = currentKey.toString().trim();
                    String v = currentValue.toString().trim();
                    if (!k.isEmpty()) map.put(k, v);
                    currentKey.setLength(0);
                    currentValue.setLength(0);
                    readingKey = true;
                    continue;
                }
            }
            if (readingKey) currentKey.append(c);
            else currentValue.append(c);
        }
        String k = currentKey.toString().trim();
        String v = currentValue.toString().trim();
        if (!k.isEmpty()) map.put(k, v);
        return map;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20 || ch > 0x7e) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    private static String sessionRecordToJson(SessionRecord s, int index) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"index\":").append(index).append(",");
        sb.append("\"type\":\"").append(s.getType().name()).append("\",");
        sb.append("\"subject\":\"").append(escapeJson(s.getSubject())).append("\",");
        sb.append("\"startTime\":\"").append(s.getStartTime().toString()).append("\",");
        sb.append("\"endTime\":\"").append(s.getEndTime().toString()).append("\",");
        sb.append("\"durationSeconds\":").append(s.getDurationSeconds()).append(",");
        sb.append("\"description\":\"").append(escapeJson(s.getDescription())).append("\",");
        sb.append("\"pauseCount\":").append(s.getPauseCount()).append(",");
        sb.append("\"questionsSolved\":").append(s.getQuestionsSolved());
        sb.append("}");
        return sb.toString();
    }
}
