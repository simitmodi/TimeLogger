package com.timelogger;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AppFrame extends JFrame {
    private final StorageService storageService = new StorageService();
    private final ExportService exportService = new ExportService();

    private final List<String> subjects;
    private final DefaultListModel<String> subjectsListModel = new DefaultListModel<>();

    private final DefaultTableModel sessionsTableModel = new DefaultTableModel(
        new Object[]{"Type", "Subject", "Start", "End", "Duration (sec)", "Duration"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JLabel stopwatchTimeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
    private final JLabel stopwatchSubjectLabel = new JLabel("Subject: -", SwingConstants.CENTER);
    private final JComboBox<String> stopwatchSubjectCombo = new JComboBox<>();
    private final JButton stopwatchStartButton = new JButton("Start");
    private final JButton stopwatchPauseResumeButton = new JButton("Pause");
    private final JButton stopwatchStopButton = new JButton("Stop & Log");
    private final JButton stopwatchResetButton = new JButton("Reset");

    private boolean stopwatchRunning = false;
    private boolean stopwatchStarted = false;
    private long stopwatchElapsedMillis = 0;
    private long stopwatchStartNano = 0;
    private LocalDateTime stopwatchSessionStart;
    private String stopwatchSubject;
    private final Timer stopwatchUiTimer;

    private final JLabel timerTimeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
    private final JSpinner hoursSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
    private final JSpinner minutesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
    private final JSpinner secondsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
    private final JComboBox<String> timerSubjectCombo = new JComboBox<>();
    private final JButton timerStartButton = new JButton("Start");
    private final JButton timerPauseResumeButton = new JButton("Pause");
    private final JButton timerStopButton = new JButton("Stop");
    private final JButton timerResetButton = new JButton("Reset");

    private boolean timerRunning = false;
    private boolean timerStarted = false;
    private long timerTotalSeconds = 0;
    private long timerRemainingSeconds = 0;
    private LocalDateTime timerSessionStart;
    private final Timer timerTick;

    private final JButton exportWeeklyButton = new JButton("Export Weekly XLSX");
    private final JButton exportMonthlyButton = new JButton("Export Monthly XLSX");
    private final JLabel exportInfoLabel = new JLabel();

    public AppFrame() {
        setTitle("Time Logger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);

        this.subjects = new ArrayList<>(storageService.loadSubjects());
        subjects.forEach(subjectsListModel::addElement);

        this.stopwatchUiTimer = new Timer(100, e -> updateStopwatchDisplay());
        this.timerTick = new Timer(1000, e -> onTimerTick());

        setJMenuBar(createMenuBar());
        setLayout(new BorderLayout());
        add(createTabs(), BorderLayout.CENTER);

        refreshStopwatchSubjects();
        refreshTimerSubjects();
        refreshSessionsTable();
        refreshExportAvailability();
        updateStopwatchButtons();
        updateTimerButtons();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem subjectsMenuItem = new JMenuItem("Manage Subjects");
        subjectsMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Use the Settings tab to add/remove subjects.",
            "Subjects",
            JOptionPane.INFORMATION_MESSAGE));
        settingsMenu.add(subjectsMenuItem);
        menuBar.add(settingsMenu);
        return menuBar;
    }

    private JTabbedPane createTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Stopwatch", createStopwatchPanel());
        tabs.addTab("Timer", createTimerPanel());
        tabs.addTab("Logs", createLogsPanel());
        tabs.addTab("Settings", createSettingsPanel());
        return tabs;
    }

    private JPanel createStopwatchPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        stopwatchTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 64));
        stopwatchSubjectLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        JPanel config = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        config.add(new JLabel("Subject"));
        config.add(stopwatchSubjectCombo);

        centerPanel.add(stopwatchSubjectLabel, BorderLayout.NORTH);
        centerPanel.add(stopwatchTimeLabel, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        stopwatchStartButton.addActionListener(e -> startStopwatch());
        stopwatchPauseResumeButton.addActionListener(e -> togglePauseStopwatch());
        stopwatchStopButton.addActionListener(e -> stopAndLogStopwatch());
        stopwatchResetButton.addActionListener(e -> resetStopwatch());

        controls.add(stopwatchStartButton);
        controls.add(stopwatchPauseResumeButton);
        controls.add(stopwatchStopButton);
        controls.add(stopwatchResetButton);

        panel.add(config, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTimerPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        timerTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 64));

        JPanel config = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        config.add(new JLabel("Hours"));
        config.add(hoursSpinner);
        config.add(new JLabel("Minutes"));
        config.add(minutesSpinner);
        config.add(new JLabel("Seconds"));
        config.add(secondsSpinner);
        config.add(new JLabel("Subject"));
        config.add(timerSubjectCombo);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        timerStartButton.addActionListener(e -> startTimer());
        timerPauseResumeButton.addActionListener(e -> togglePauseTimer());
        timerStopButton.addActionListener(e -> stopTimer());
        timerResetButton.addActionListener(e -> resetTimer());

        controls.add(timerStartButton);
        controls.add(timerPauseResumeButton);
        controls.add(timerStopButton);
        controls.add(timerResetButton);

        panel.add(config, BorderLayout.NORTH);
        panel.add(timerTimeLabel, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JTable table = new JTable(sessionsTableModel);
        table.setRowHeight(24);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshSessionsTable());

        exportWeeklyButton.addActionListener(e -> exportWeeklyReport());
        exportMonthlyButton.addActionListener(e -> exportMonthlyReport());

        actions.add(refreshButton);
        actions.add(exportWeeklyButton);
        actions.add(exportMonthlyButton);

        exportInfoLabel.setHorizontalAlignment(SwingConstants.LEFT);

        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(exportInfoLabel, BorderLayout.SOUTH);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JList<String> subjectsList = new JList<>(subjectsListModel);
        panel.add(new JScrollPane(subjectsList), BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Manage Subjects"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField subjectField = new JTextField(20);
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove Selected");

        formPanel.add(new JLabel("Subject"), gbc);
        gbc.gridx = 1;
        formPanel.add(subjectField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(addButton, gbc);

        gbc.gridx = 1;
        formPanel.add(removeButton, gbc);

        addButton.addActionListener(e -> {
            String value = subjectField.getText().trim();
            if (value.isEmpty()) {
                return;
            }
            if (subjects.stream().anyMatch(existing -> existing.equalsIgnoreCase(value))) {
                JOptionPane.showMessageDialog(this, "Subject already exists.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            subjects.add(value);
            subjects.sort(String::compareToIgnoreCase);
            persistSubjects();
            subjectField.setText("");
        });

        removeButton.addActionListener(e -> {
            String selected = subjectsList.getSelectedValue();
            if (selected == null) {
                return;
            }
            if (subjects.size() <= 1) {
                JOptionPane.showMessageDialog(this,
                    "At least one subject is required.",
                    "Cannot Remove",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            subjects.removeIf(s -> s.equals(selected));
            persistSubjects();
        });

        panel.add(formPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void startStopwatch() {
        if (subjects.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one subject in Settings first.", "No Subjects", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!stopwatchStarted) {
            String selectedSubject = (String) stopwatchSubjectCombo.getSelectedItem();

            if (selectedSubject == null || selectedSubject.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Select a subject from the list before starting stopwatch.",
                    "No Subject Selected",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            stopwatchSubject = selectedSubject;
            stopwatchSubjectLabel.setText("Subject: " + stopwatchSubject);
            stopwatchSessionStart = LocalDateTime.now();
            stopwatchElapsedMillis = 0;
            stopwatchStarted = true;
        }

        if (!stopwatchRunning) {
            stopwatchStartNano = System.nanoTime();
            stopwatchRunning = true;
            stopwatchUiTimer.start();
            updateStopwatchButtons();
        }
    }

    private void togglePauseStopwatch() {
        if (!stopwatchStarted) {
            return;
        }

        if (stopwatchRunning) {
            pauseStopwatch();
        } else {
            stopwatchStartNano = System.nanoTime();
            stopwatchRunning = true;
            stopwatchUiTimer.start();
            updateStopwatchButtons();
        }
    }

    private void pauseStopwatch() {
        if (!stopwatchRunning) {
            return;
        }

        stopwatchElapsedMillis += (System.nanoTime() - stopwatchStartNano) / 1_000_000;
        stopwatchRunning = false;
        stopwatchUiTimer.stop();
        updateStopwatchDisplay();
        updateStopwatchButtons();
    }

    private void stopAndLogStopwatch() {
        if (!stopwatchStarted) {
            return;
        }

        if (stopwatchRunning) {
            pauseStopwatch();
        }

        long durationSeconds = Math.max(1, stopwatchElapsedMillis / 1000);
        LocalDateTime end = LocalDateTime.now();

        storageService.appendSession(new SessionRecord(
            SessionRecord.SessionType.STOPWATCH,
            stopwatchSubject,
            stopwatchSessionStart,
            end,
            durationSeconds
        ));

        refreshSessionsTable();
        refreshExportAvailability();
        resetStopwatch();
        JOptionPane.showMessageDialog(this, "Stopwatch session logged.", "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetStopwatch() {
        stopwatchRunning = false;
        stopwatchStarted = false;
        stopwatchElapsedMillis = 0;
        stopwatchStartNano = 0;
        stopwatchSubject = null;
        stopwatchSessionStart = null;
        stopwatchUiTimer.stop();
        stopwatchSubjectLabel.setText("Subject: -");
        stopwatchTimeLabel.setText("00:00:00");
        updateStopwatchButtons();
    }

    private void updateStopwatchDisplay() {
        long elapsed = stopwatchElapsedMillis;
        if (stopwatchRunning) {
            elapsed += (System.nanoTime() - stopwatchStartNano) / 1_000_000;
        }

        long totalSeconds = elapsed / 1000;
        stopwatchTimeLabel.setText(formatDuration(totalSeconds));
    }

    private void updateStopwatchButtons() {
        stopwatchStartButton.setEnabled(!stopwatchRunning);
        stopwatchPauseResumeButton.setEnabled(stopwatchStarted);
        stopwatchPauseResumeButton.setText(stopwatchRunning ? "Pause" : "Resume");
        stopwatchStopButton.setEnabled(stopwatchStarted);
        stopwatchResetButton.setEnabled(stopwatchStarted);
    }

    private void startTimer() {
        if (!timerStarted) {
            long h = (Integer) hoursSpinner.getValue();
            long m = (Integer) minutesSpinner.getValue();
            long s = (Integer) secondsSpinner.getValue();
            timerTotalSeconds = h * 3600 + m * 60 + s;
            timerRemainingSeconds = timerTotalSeconds;

            if (timerTotalSeconds <= 0) {
                JOptionPane.showMessageDialog(this, "Set a time greater than 00:00:00.", "Invalid Time", JOptionPane.WARNING_MESSAGE);
                return;
            }

            timerSessionStart = LocalDateTime.now();
            timerStarted = true;
        }

        if (!timerRunning) {
            timerRunning = true;
            timerTick.start();
            updateTimerButtons();
        }
    }

    private void togglePauseTimer() {
        if (!timerStarted) {
            return;
        }

        timerRunning = !timerRunning;
        if (timerRunning) {
            timerTick.start();
        } else {
            timerTick.stop();
        }
        updateTimerButtons();
    }

    private void stopTimer() {
        if (!timerStarted) {
            return;
        }

        if (timerRunning) {
            timerTick.stop();
            timerRunning = false;
        }

        long elapsed = timerTotalSeconds - timerRemainingSeconds;
        if (elapsed > 0) {
            logTimerSession(elapsed);
            JOptionPane.showMessageDialog(this, "Timer session logged.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        }

        resetTimer();
    }

    private void resetTimer() {
        timerTick.stop();
        timerRunning = false;
        timerStarted = false;
        timerTotalSeconds = 0;
        timerRemainingSeconds = 0;
        timerSessionStart = null;
        timerTimeLabel.setText("00:00:00");
        updateTimerButtons();
    }

    private void onTimerTick() {
        if (!timerRunning) {
            return;
        }

        timerRemainingSeconds--;
        timerTimeLabel.setText(formatDuration(Math.max(0, timerRemainingSeconds)));

        if (timerRemainingSeconds <= 0) {
            timerTick.stop();
            timerRunning = false;
            long elapsed = timerTotalSeconds;
            logTimerSession(elapsed);
            JOptionPane.showMessageDialog(this, "Timer finished and session logged.", "Completed", JOptionPane.INFORMATION_MESSAGE);
            resetTimer();
        }
    }

    private void logTimerSession(long elapsedSeconds) {
        String subject = (String) timerSubjectCombo.getSelectedItem();
        if (subject == null || subject.isBlank()) {
            subject = "General";
        }

        storageService.appendSession(new SessionRecord(
            SessionRecord.SessionType.TIMER,
            subject,
            timerSessionStart != null ? timerSessionStart : LocalDateTime.now(),
            LocalDateTime.now(),
            elapsedSeconds
        ));

        refreshSessionsTable();
        refreshExportAvailability();
    }

    private void refreshSessionsTable() {
        sessionsTableModel.setRowCount(0);

        List<SessionRecord> sessions = storageService.loadSessions();
        for (SessionRecord session : sessions) {
            sessionsTableModel.addRow(new Object[]{
                session.getType().name(),
                session.getSubject(),
                session.getStartTime().toString().replace('T', ' '),
                session.getEndTime().toString().replace('T', ' '),
                session.getDurationSeconds(),
                formatDuration(session.getDurationSeconds())
            });
        }
    }

    private void persistSubjects() {
        storageService.saveSubjects(subjects);
        subjectsListModel.clear();
        subjects.stream().sorted(String::compareToIgnoreCase).forEach(subjectsListModel::addElement);
        refreshStopwatchSubjects();
        refreshTimerSubjects();
    }

    private void refreshStopwatchSubjects() {
        String previous = (String) stopwatchSubjectCombo.getSelectedItem();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String subject : subjects) {
            model.addElement(subject);
        }
        stopwatchSubjectCombo.setModel(model);

        if (previous != null && subjects.contains(previous)) {
            stopwatchSubjectCombo.setSelectedItem(previous);
        } else if (!subjects.isEmpty()) {
            stopwatchSubjectCombo.setSelectedIndex(0);
        }
    }

    private void refreshTimerSubjects() {
        String previous = (String) timerSubjectCombo.getSelectedItem();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String subject : subjects) {
            model.addElement(subject);
        }
        timerSubjectCombo.setModel(model);

        if (previous != null && subjects.contains(previous)) {
            timerSubjectCombo.setSelectedItem(previous);
        } else if (!subjects.isEmpty()) {
            timerSubjectCombo.setSelectedIndex(0);
        }
    }

    private void refreshExportAvailability() {
        boolean isMonday = LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY;
        boolean isFirstDayOfMonth = LocalDate.now().getDayOfMonth() == 1;
        exportWeeklyButton.setEnabled(isMonday);
        exportMonthlyButton.setEnabled(isFirstDayOfMonth);

        if (isMonday && isFirstDayOfMonth) {
            exportInfoLabel.setText("Today supports both weekly and monthly export.");
        } else if (isMonday) {
            exportInfoLabel.setText("Today is Monday. Weekly export is available.");
        } else if (isFirstDayOfMonth) {
            exportInfoLabel.setText("Today is the first day of the month. Monthly export is available.");
        } else {
            exportInfoLabel.setText("Weekly export: Monday only. Monthly export: first day of month only.");
        }
    }

    private void exportWeeklyReport() {
        if (LocalDate.now().getDayOfWeek() != DayOfWeek.MONDAY) {
            JOptionPane.showMessageDialog(this,
                "Weekly export is available on Mondays only.",
                "Not Available",
                JOptionPane.INFORMATION_MESSAGE);
            refreshExportAvailability();
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusWeeks(1);
        LocalDate weekEnd = today.minusDays(1);

        List<SessionRecord> sessions = storageService.loadSessions().stream()
            .filter(session -> {
                LocalDate sessionDate = session.getEndTime().toLocalDate();
                return !sessionDate.isBefore(weekStart) && !sessionDate.isAfter(weekEnd);
            })
            .collect(Collectors.toList());

        if (sessions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No sessions found for previous week to export.",
                "No Data",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String period = weekStart + " to " + weekEnd;
        String fileName = "timelog-weekly-" + weekStart + "_to_" + weekEnd + ".xlsx";
        Path outputFile = storageService.getAppDirectory().resolve(fileName);
        exportService.exportReport(outputFile, sessions, period);

        JOptionPane.showMessageDialog(this,
            "Exported report to:\n" + outputFile,
            "Export Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportMonthlyReport() {
        if (LocalDate.now().getDayOfMonth() != 1) {
            JOptionPane.showMessageDialog(this,
                "Monthly export is available on the first day of month only.",
                "Not Available",
                JOptionPane.INFORMATION_MESSAGE);
            refreshExportAvailability();
            return;
        }

        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate monthStart = previousMonth.atDay(1);
        LocalDate monthEnd = previousMonth.atEndOfMonth();

        List<SessionRecord> sessions = storageService.loadSessions().stream()
            .filter(session -> {
                LocalDate sessionDate = session.getEndTime().toLocalDate();
                return !sessionDate.isBefore(monthStart) && !sessionDate.isAfter(monthEnd);
            })
            .collect(Collectors.toList());

        if (sessions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No sessions found for previous month to export.",
                "No Data",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String period = previousMonth.toString();
        String fileName = "timelog-monthly-" + previousMonth + ".xlsx";
        Path outputFile = storageService.getAppDirectory().resolve(fileName);
        exportService.exportReport(outputFile, sessions, period);

        JOptionPane.showMessageDialog(this,
            "Exported report to:\n" + outputFile,
            "Export Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateTimerButtons() {
        timerStartButton.setEnabled(!timerRunning);
        timerPauseResumeButton.setEnabled(timerStarted);
        timerPauseResumeButton.setText(timerRunning ? "Pause" : "Resume");
        timerStopButton.setEnabled(timerStarted);
        timerResetButton.setEnabled(timerStarted);
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
