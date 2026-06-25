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
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.GradientPaint;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
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

    // --- System Tray and Notifications ---
    private TrayIcon trayIcon = null;
    private boolean trayIconAdded = false;
    private boolean stopwatchBreakNotificationSent = false;
    private boolean timerBreakNotificationSent = false;
    private boolean goalNotificationSentToday = false;
    private LocalDate lastGoalCheckDate = LocalDate.now();

    // --- Insights Tab UI Elements ---
    private final JLabel burnoutRiskLabel = new JLabel("0%", SwingConstants.CENTER);
    private final JLabel burnoutStatusLabel = new JLabel("Calculating...", SwingConstants.CENTER);
    private final JLabel productivityIndexLabel = new JLabel("0/100", SwingConstants.CENTER);
    private final javax.swing.JProgressBar productivityProgress = new javax.swing.JProgressBar(0, 100);
    private final JPanel warningsContainer = new JPanel();
    private final JLabel insightDetailHours = new JLabel("-");
    private final JLabel insightDetailDays = new JLabel("-");
    private final JLabel insightDetailLateNights = new JLabel("-");
    private final JLabel insightDetailBreakRatio = new JLabel("-");
    private final JLabel insightDetailMidSessionBreaks = new JLabel("-");
    private final JLabel insightDetailEfficiency = new JLabel("-");
    private javax.swing.JCheckBox enableNotificationsCheckbox;
    private javax.swing.JCheckBox enableBreakRemindersCheckbox;

    final List<String> subjects;
    private final DefaultListModel<String> subjectsListModel = new DefaultListModel<>();

    private final DefaultTableModel sessionsTableModel = new DefaultTableModel(
        new Object[]{"Date", "Type", "Subject", "Activity", "Start", "End", "Duration"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JLabel stopwatchTimeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
    private final JLabel stopwatchSubjectLabel = new JLabel("Subject: -", SwingConstants.CENTER);
    final JComboBox<String> stopwatchSubjectCombo = new JComboBox<>();
    final JComboBox<String> stopwatchActivityTypeCombo = new JComboBox<>(new String[]{"General", "Questions", "Lecture"});
    final JPanel stopwatchActivitySubPanel = new JPanel(new java.awt.CardLayout());
    final JTextField stopwatchActivityField = new JTextField(20);
    final JComboBox<String> stopwatchQuestionTypeCombo = new JComboBox<>(new String[]{
        "DPP Questions", "Practice Book Questions", "Previous Year Questions"
    });
    final JTextField stopwatchQuestionDescField = new JTextField(15);
    final JTextField stopwatchChapterField = new JTextField(5);
    final JTextField stopwatchLectureField = new JTextField(5);
    private final ModernButton stopwatchStartButton = new ModernButton("Start");
    private final ModernButton stopwatchPauseResumeButton = new ModernButton("Pause");
    private final ModernButton stopwatchStopButton = new ModernButton("Stop & Log");
    private final ModernButton stopwatchResetButton = new ModernButton("Reset");
    private final ModernButton stopwatchMiniButton = new ModernButton("Mini Mode");

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
    final JComboBox<String> timerSubjectCombo = new JComboBox<>();
    private final ModernButton timerStartButton = new ModernButton("Start");
    private final ModernButton timerPauseResumeButton = new ModernButton("Pause");
    private final ModernButton timerStopButton = new ModernButton("Stop");
    private final ModernButton timerResetButton = new ModernButton("Reset");
    private final ModernButton timerMiniButton = new ModernButton("Mini Mode");

    private boolean timerRunning = false;
    private boolean timerStarted = false;
    private long timerTotalSeconds = 0;
    private long timerRemainingSeconds = 0;
    private LocalDateTime timerSessionStart;
    private final Timer timerTick;

    private MiniWindow miniWindow = null;
    private JTabbedPane tabs;
    private SessionRecord resumedSession = null;
    private String previousAnalysisPeriod = "All Time";
    private LocalDate customAnalysisStartDate = null;
    private LocalDate customAnalysisEndDate = null;

    private final JComboBox<String> logsSubjectFilterCombo = new JComboBox<>();
    private final JComboBox<String> logsModeFilterCombo = new JComboBox<>(new String[]{"All Modes", "Stopwatch", "Timer"});
    private final JComboBox<String> logsDateFilterCombo = new JComboBox<>(new String[]{"All Dates", "Today", "Yesterday", "This Week", "This Month", "Custom Range..."});
    private String previousLogsDatePeriod = "All Dates";
    private LocalDate customLogsStartDate = null;
    private LocalDate customLogsEndDate = null;
    private final JTextField logsSearchField = new JTextField(15);
    private final List<SessionRecord> displayedSessions = new java.util.ArrayList<>();
    private JTable logsTable;
    private final java.util.Map<Integer, java.util.Set<String>> columnFilters = new java.util.HashMap<>();

    final ModernButton exportWeeklyButton = new ModernButton("Export Weekly XLSX");
    final ModernButton exportMonthlyButton = new ModernButton("Export Monthly XLSX");
    private final JLabel exportInfoLabel = new JLabel();

    final JLabel totalSessionsValueLabel = new JLabel("-");
    final JLabel totalDurationValueLabel = new JLabel("-");
    final DefaultTableModel subjectAnalysisModel = new DefaultTableModel(new Object[]{"Subject", "Duration"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    final DefaultTableModel dayOfWeekAnalysisModel = new DefaultTableModel(new Object[]{"Day of Week", "Duration"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    final DefaultTableModel activityAnalysisModel = new DefaultTableModel(new Object[]{"Activity", "Duration"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    final DefaultTableModel chapterAnalysisModel = new DefaultTableModel(new Object[]{"Subject - Chapter", "Duration"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final PieChartPanel subjectPieChart = new PieChartPanel();
    private final HeatmapPanel heatmapPanel = new HeatmapPanel();
    private final WeeklyBarChartPanel weeklyBarChartPanel = new WeeklyBarChartPanel();
    private final DailyTimelinePanel dailyTimelinePanel = new DailyTimelinePanel();

    private final JComboBox<String> analysisPeriodCombo = new JComboBox<>(new String[]{
        "All Time", "Today", "Yesterday", "This Week", "Last 7 Days", "This Month", "Last 30 Days", "Custom Range..."
    });
    private final JLabel avgSessionLabel = new JLabel("Avg Duration: -", SwingConstants.CENTER);
    private final JLabel activeDayAvgLabel = new JLabel("Active Day Avg: -", SwingConstants.CENTER);
    private final JLabel mostActiveSubjectLabel = new JLabel("Most Active: -", SwingConstants.CENTER);
    private final JLabel studyEfficiencyLabel = new JLabel("Study Efficiency: -", SwingConstants.CENTER);
    private final JLabel midSessionBreakLabel = new JLabel("Mid-Session Breaks: -", SwingConstants.CENTER);

    private final JLabel goalProgressLabel = new JLabel("Today: 0 / 0 min (0%)", SwingConstants.CENTER);
    private final javax.swing.JProgressBar goalProgressBar = new javax.swing.JProgressBar(0, 100);
    private final JLabel streakLabel = new JLabel("Streak: 0 days 🔥 (Max: 0 🏆)", SwingConstants.CENTER);

    public AppFrame() {
        setTitle("Time Logger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);

        // Set window icon
        try {
            java.io.InputStream imgStream = AppFrame.class.getResourceAsStream("/com/timelogger/icon.png");
            if (imgStream != null) {
                javax.swing.ImageIcon icon = new javax.swing.ImageIcon(javax.imageio.ImageIO.read(imgStream));
                setIconImage(icon.getImage());
            }
        } catch (Exception ignored) {
        }

        this.subjects = new ArrayList<>(storageService.loadSubjects());
        subjects.forEach(subjectsListModel::addElement);

        this.stopwatchUiTimer = new Timer(100, e -> updateStopwatchDisplay());
        this.timerTick = new Timer(1000, e -> onTimerTick());

        setJMenuBar(createMenuBar());
        setLayout(new BorderLayout());
        add(createTabs(), BorderLayout.CENTER);

        refreshStopwatchSubjects();
        refreshTimerSubjects();
        refreshLogsSubjects();
        refreshSessionsTable();
        refreshExportAvailability();
        updateStopwatchButtons();
        updateTimerButtons();

        // Apply saved theme on startup
        updateTheme(ThemeManager.loadTheme());

        initSystemTray();

        this.tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == tabs.indexOfTab("Insights")) {
                refreshInsights();
            }
        });

        // Minimize memory footprint and hide to system tray when minimized
        this.addWindowStateListener(e -> {
            if ((e.getNewState() & JFrame.ICONIFIED) == JFrame.ICONIFIED) {
                System.gc();
                if (SystemTray.isSupported() && trayIconAdded) {
                    setVisible(false);
                }
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem subjectsMenuItem = new JMenuItem("Manage Subjects");
        subjectsMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Use the Subjects tab to add/remove subjects.",
            "Subjects",
            JOptionPane.INFORMATION_MESSAGE));
        settingsMenu.add(subjectsMenuItem);
        
        JMenuItem goalMenuItem = new JMenuItem("Set Daily Goal");
        goalMenuItem.addActionListener(e -> promptSetDailyGoal());
        settingsMenu.add(goalMenuItem);

        menuBar.add(settingsMenu);

        JMenu themeMenu = new JMenu("Theme");
        JMenuItem lightItem = new JMenuItem("Light Mode");
        lightItem.addActionListener(e -> updateTheme(ThemeManager.AppTheme.LIGHT));
        JMenuItem darkItem = new JMenuItem("Dark Mode");
        darkItem.addActionListener(e -> updateTheme(ThemeManager.AppTheme.DARK));
        JMenuItem hcItem = new JMenuItem("High Contrast Mode");
        hcItem.addActionListener(e -> updateTheme(ThemeManager.AppTheme.HIGH_CONTRAST));
        themeMenu.add(lightItem);
        themeMenu.add(darkItem);
        themeMenu.add(hcItem);
        menuBar.add(themeMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem shortcutsMenuItem = new JMenuItem("Keyboard Shortcuts");
        shortcutsMenuItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        shortcutsMenuItem.addActionListener(e -> showShortcutsHelp());
        helpMenu.add(shortcutsMenuItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JTabbedPane createTabs() {
        this.tabs = new JTabbedPane();
        tabs.addTab("Stopwatch", createStopwatchPanel());
        tabs.addTab("Timer", createTimerPanel());
        tabs.addTab("Logs", createLogsPanel());
        tabs.addTab("Analysis", createAnalysisPanel());
        tabs.addTab("Insights", createInsightsPanel());
        tabs.addTab("Subjects", createSubjectsPanel());
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
        config.add(new JLabel("Activity Type"));
        config.add(stopwatchActivityTypeCombo);

        JPanel generalCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        generalCard.add(new JLabel("Desc: "));
        generalCard.add(stopwatchActivityField);

        javax.swing.JPopupMenu autocompletePopup = new javax.swing.JPopupMenu();
        autocompletePopup.setFocusable(false);
        stopwatchActivityField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateAutocomplete() {
                String typed = stopwatchActivityField.getText().trim();
                if (typed.isEmpty()) {
                    autocompletePopup.setVisible(false);
                    return;
                }
                String subject = (String) stopwatchSubjectCombo.getSelectedItem();
                if (subject == null) {
                    autocompletePopup.setVisible(false);
                    return;
                }
                List<SessionRecord> allSessions = storageService.loadSessions();
                List<String> matches = allSessions.stream()
                    .filter(s -> s.getSubject().equalsIgnoreCase(subject) && s.getDescription() != null)
                    .map(SessionRecord::getDescription)
                    .filter(d -> !d.isEmpty() && d.toLowerCase().contains(typed.toLowerCase()))
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
                if (matches.isEmpty()) {
                    autocompletePopup.setVisible(false);
                    return;
                }
                autocompletePopup.removeAll();
                for (String m : matches) {
                    javax.swing.JMenuItem item = new javax.swing.JMenuItem(m);
                    item.addActionListener(e -> {
                        stopwatchActivityField.setText(m);
                        autocompletePopup.setVisible(false);
                    });
                    autocompletePopup.add(item);
                }
                if (stopwatchActivityField.isShowing()) {
                    autocompletePopup.show(stopwatchActivityField, 0, stopwatchActivityField.getHeight());
                }
            }
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (stopwatchActivityField.hasFocus()) {
                    javax.swing.SwingUtilities.invokeLater(this::updateAutocomplete);
                }
            }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (stopwatchActivityField.hasFocus()) {
                    javax.swing.SwingUtilities.invokeLater(this::updateAutocomplete);
                }
            }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        JPanel questionsCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        questionsCard.add(new JLabel("Type: "));
        questionsCard.add(stopwatchQuestionTypeCombo);
        questionsCard.add(new JLabel(" Desc: "));
        questionsCard.add(stopwatchQuestionDescField);

        JPanel lectureCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lectureCard.add(new JLabel("Ch No: "));
        lectureCard.add(stopwatchChapterField);
        lectureCard.add(new JLabel("Lec No: "));
        lectureCard.add(stopwatchLectureField);

        stopwatchActivitySubPanel.add(generalCard, "General");
        stopwatchActivitySubPanel.add(questionsCard, "Questions");
        stopwatchActivitySubPanel.add(lectureCard, "Lecture");

        java.awt.CardLayout cardLayout = (java.awt.CardLayout) stopwatchActivitySubPanel.getLayout();
        stopwatchActivityTypeCombo.addActionListener(e -> {
            String selected = (String) stopwatchActivityTypeCombo.getSelectedItem();
            cardLayout.show(stopwatchActivitySubPanel, selected);
        });

        config.add(stopwatchActivitySubPanel);

        centerPanel.add(stopwatchSubjectLabel, BorderLayout.NORTH);
        centerPanel.add(stopwatchTimeLabel, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        stopwatchStartButton.addActionListener(e -> startStopwatch());
        stopwatchPauseResumeButton.addActionListener(e -> togglePauseStopwatch());
        stopwatchStopButton.addActionListener(e -> stopAndLogStopwatch());
        stopwatchResetButton.addActionListener(e -> resetStopwatch());
        stopwatchMiniButton.addActionListener(e -> {
            if (subjects.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Add at least one subject in the Subjects tab first.", "No Subjects", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String selectedSubject = (String) stopwatchSubjectCombo.getSelectedItem();
            if (selectedSubject == null || selectedSubject.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Select a subject from the list before entering Mini Mode.",
                    "No Subject Selected",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!stopwatchStarted) {
                stopwatchSubject = selectedSubject;
                stopwatchSubjectLabel.setText("Subject: " + stopwatchSubject);
                stopwatchSessionStart = LocalDateTime.now();
                stopwatchElapsedMillis = 0;
                stopwatchStarted = true;
                updateStopwatchButtons();
            }
            openMiniWindow(SessionRecord.SessionType.STOPWATCH);
        });

        stopwatchStartButton.setMnemonic('S');
        stopwatchPauseResumeButton.setMnemonic('P');
        stopwatchStopButton.setMnemonic('L');
        stopwatchResetButton.setMnemonic('R');
        stopwatchMiniButton.setMnemonic('M');

        controls.add(stopwatchStartButton);
        controls.add(stopwatchPauseResumeButton);
        controls.add(stopwatchStopButton);
        controls.add(stopwatchResetButton);
        controls.add(stopwatchMiniButton);

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
        timerMiniButton.addActionListener(e -> {
            if (!timerStarted) {
                long h = (Integer) hoursSpinner.getValue();
                long m = (Integer) minutesSpinner.getValue();
                long s = (Integer) secondsSpinner.getValue();
                timerTotalSeconds = h * 3600 + m * 60 + s;
                timerRemainingSeconds = timerTotalSeconds;

                if (timerTotalSeconds <= 0) {
                    JOptionPane.showMessageDialog(this, "Set a time greater than 00:00:00 before entering Mini Mode.", "Invalid Time", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                timerSessionStart = LocalDateTime.now();
                timerStarted = true;
                updateTimerButtons();
            }
            openMiniWindow(SessionRecord.SessionType.TIMER);
        });

        timerStartButton.setMnemonic('S');
        timerPauseResumeButton.setMnemonic('P');
        timerStopButton.setMnemonic('T');
        timerResetButton.setMnemonic('R');
        timerMiniButton.setMnemonic('M');

        controls.add(timerStartButton);
        controls.add(timerPauseResumeButton);
        controls.add(timerStopButton);
        controls.add(timerResetButton);
        controls.add(timerMiniButton);

        panel.add(config, BorderLayout.NORTH);
        panel.add(timerTimeLabel, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        filterBar.add(new JLabel("Subject:"));
        filterBar.add(logsSubjectFilterCombo);
        filterBar.add(new JLabel("Mode:"));
        filterBar.add(logsModeFilterCombo);
        filterBar.add(new JLabel("Date:"));
        filterBar.add(logsDateFilterCombo);
        filterBar.add(new JLabel("Search Activity:"));
        filterBar.add(logsSearchField);

        ModernButton clearFiltersBtn = new ModernButton("Clear Filters");
        clearFiltersBtn.addActionListener(e -> {
            logsSubjectFilterCombo.setSelectedIndex(0);
            logsModeFilterCombo.setSelectedIndex(0);
            if (logsDateFilterCombo != null) {
                // Remove custom date range items if any
                for (int i = 0; i < logsDateFilterCombo.getItemCount(); i++) {
                    String item = logsDateFilterCombo.getItemAt(i);
                    if (item != null && item.startsWith("Custom: ")) {
                        logsDateFilterCombo.removeItemAt(i);
                        break;
                    }
                }
                logsDateFilterCombo.setSelectedItem("All Dates");
            }
            logsSearchField.setText("");
            columnFilters.clear();
            if (logsTable.getRowSorter() != null) {
                logsTable.getRowSorter().setSortKeys(null);
            }
            refreshSessionsTable();
        });
        filterBar.add(clearFiltersBtn);

        // Add action listeners to trigger table refresh
        logsSubjectFilterCombo.addActionListener(e -> refreshSessionsTable());
        logsModeFilterCombo.addActionListener(e -> refreshSessionsTable());
        logsDateFilterCombo.addActionListener(e -> refreshSessionsTable());
        
        logsSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                refreshSessionsTable();
            }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                refreshSessionsTable();
            }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                refreshSessionsTable();
            }
        });

        panel.add(filterBar, BorderLayout.NORTH);

        logsTable = new JTable(sessionsTableModel);
        logsTable.setRowHeight(24);

        // TableRowSorter for Excel sorting
        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(sessionsTableModel);
        logsTable.setRowSorter(sorter);

        // Custom Header Renderer
        logsTable.getTableHeader().setDefaultRenderer(new FilterHeaderRenderer(logsTable.getTableHeader().getDefaultRenderer()));

        // MouseListener on table header to trigger popup or sort
        logsTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int colIndex = logsTable.getTableHeader().columnAtPoint(e.getPoint());
                if (colIndex == -1) return;

                java.awt.Rectangle rect = logsTable.getTableHeader().getHeaderRect(colIndex);
                // Click on the rightmost 25 pixels
                if (e.getX() >= rect.x + rect.width - 25) {
                    showFilterPopup(colIndex, e.getComponent(), e.getX(), e.getY());
                    e.consume();
                }
            }
        });

        panel.add(new JScrollPane(logsTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ModernButton refreshButton = new ModernButton("Refresh");
        refreshButton.addActionListener(e -> refreshSessionsTable());

        ModernButton deleteSelectedButton = new ModernButton("Delete Selected");
        deleteSelectedButton.addActionListener(e -> {
            int selectedRow = logsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this,
                    "Please select a log entry to delete.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            int modelRow = logsTable.convertRowIndexToModel(selectedRow);
            if (modelRow >= 0 && modelRow < displayedSessions.size()) {
                SessionRecord selectedSession = displayedSessions.get(modelRow);
                List<SessionRecord> allSessions = storageService.loadSessions();
                
                boolean removed = allSessions.removeIf(s -> 
                    s.getStartTime().equals(selectedSession.getStartTime()) &&
                    s.getSubject().equalsIgnoreCase(selectedSession.getSubject()) &&
                    s.getType() == selectedSession.getType()
                );
                
                if (removed) {
                    storageService.saveSessions(allSessions);
                    refreshSessionsTable();
                    refreshExportAvailability();
                    JOptionPane.showMessageDialog(this,
                        "Selected session log deleted.",
                        "Deleted",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        ModernButton clearAllButton = new ModernButton("Clear All Logs");
        clearAllButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all tracking history?\nThis action cannot be undone.",
                "Confirm Clear All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                storageService.saveSessions(new ArrayList<>());
                refreshSessionsTable();
                refreshExportAvailability();
                JOptionPane.showMessageDialog(this,
                    "All session logs cleared.",
                    "Cleared",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        ModernButton resumeSelectedButton = new ModernButton("Resume Session");
        resumeSelectedButton.addActionListener(e -> {
            if (stopwatchStarted || timerStarted) {
                JOptionPane.showMessageDialog(this,
                    "A tracking session is currently active. Please stop and save or reset your current session before resuming a past log.",
                    "Session Active",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            int selectedRow = logsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this,
                    "Please select a log entry to resume.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            int modelRow = logsTable.convertRowIndexToModel(selectedRow);
            if (modelRow >= 0 && modelRow < displayedSessions.size()) {
                SessionRecord selectedSession = displayedSessions.get(modelRow);
                this.resumedSession = selectedSession;
                String subject = selectedSession.getSubject();
                
                // Add subject dynamically if it doesn't exist in current subjects
                if (subject != null && !subject.isBlank() && !subjects.contains(subject)) {
                    subjects.add(subject);
                    subjectsListModel.addElement(subject);
                    storageService.saveSubjects(subjects);
                    refreshStopwatchSubjects();
                    refreshTimerSubjects();
                    refreshLogsSubjects();
                }

                if (selectedSession.getType() == SessionRecord.SessionType.STOPWATCH) {
                    // Populate stopwatch fields
                    stopwatchSubjectCombo.setSelectedItem(subject);
                    
                    String desc = selectedSession.getDescription() != null ? selectedSession.getDescription() : "";
                    if (desc.startsWith("Questions: ")) {
                        stopwatchActivityTypeCombo.setSelectedItem("Questions");
                        String content = desc.substring("Questions: ".length());
                        int commaIndex = content.indexOf(",");
                        String qType = "";
                        String qDesc = "";
                        if (commaIndex != -1) {
                            qType = content.substring(0, commaIndex).trim();
                            qDesc = content.substring(commaIndex + 1).trim();
                        } else {
                            qType = content.trim();
                        }
                        stopwatchQuestionTypeCombo.setSelectedItem(qType);
                        stopwatchQuestionDescField.setText(qDesc);
                    } else if (desc.startsWith("Lecture: ")) {
                        stopwatchActivityTypeCombo.setSelectedItem("Lecture");
                        String content = desc.substring("Lecture: ".length());
                        int commaIndex = content.indexOf(",");
                        String ch = "";
                        String lec = "";
                        if (commaIndex != -1) {
                            String chPart = content.substring(0, commaIndex).trim();
                            String lecPart = content.substring(commaIndex + 1).trim();
                            if (chPart.startsWith("Ch ")) {
                                ch = chPart.substring(3).trim();
                            }
                            if (lecPart.startsWith("Lec ")) {
                                lec = lecPart.substring(4).trim();
                            }
                        }
                        stopwatchChapterField.setText(ch);
                        stopwatchLectureField.setText(lec);
                    } else {
                        stopwatchActivityTypeCombo.setSelectedItem("General");
                        stopwatchActivityField.setText(desc);
                    }
                    
                    // Setup stopwatch state for resumption but don't start running yet
                    stopwatchSubject = subject;
                    stopwatchSubjectLabel.setText("Subject: " + stopwatchSubject);
                    stopwatchSessionStart = selectedSession.getStartTime();
                    stopwatchElapsedMillis = selectedSession.getDurationSeconds() * 1000;
                    stopwatchStarted = true;
                    stopwatchRunning = false;
                    updateStopwatchDisplay();
                    updateStopwatchButtons();
                    
                    // Switch to Stopwatch tab
                    tabs.setSelectedIndex(0);
                } else if (selectedSession.getType() == SessionRecord.SessionType.TIMER) {
                    // Populate timer fields
                    timerSubjectCombo.setSelectedItem(subject);
                    
                    long totalSeconds = selectedSession.getDurationSeconds();
                    int hours = (int) (totalSeconds / 3600);
                    int minutes = (int) ((totalSeconds % 3600) / 60);
                    int seconds = (int) (totalSeconds % 60);
                    
                    hoursSpinner.setValue(hours);
                    minutesSpinner.setValue(minutes);
                    secondsSpinner.setValue(seconds);
                    
                    // Setup timer state for resumption but don't start running yet
                    timerTotalSeconds = totalSeconds;
                    timerRemainingSeconds = totalSeconds;
                    timerSessionStart = selectedSession.getStartTime();
                    timerStarted = true;
                    timerRunning = false;
                    timerTimeLabel.setText(formatDuration(totalSeconds));
                    updateTimerButtons();
                    
                    // Switch to Timer tab
                    tabs.setSelectedIndex(1);
                }
            }
        });

        exportWeeklyButton.addActionListener(e -> exportWeeklyReport());
        exportMonthlyButton.addActionListener(e -> exportMonthlyReport());

        ModernButton exportCustomButton = new ModernButton("Export Custom Range...");
        exportCustomButton.addActionListener(e -> exportCustomRangeReport());

        actions.add(refreshButton);
        actions.add(resumeSelectedButton);
        actions.add(deleteSelectedButton);
        actions.add(clearAllButton);
        actions.add(exportWeeklyButton);
        actions.add(exportMonthlyButton);
        actions.add(exportCustomButton);

        exportInfoLabel.setHorizontalAlignment(SwingConstants.LEFT);

        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(exportInfoLabel, BorderLayout.SOUTH);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSubjectsPanel() {
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
        ModernButton addButton = new ModernButton("Add");
        ModernButton removeButton = new ModernButton("Remove Selected");

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

    private JPanel createAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        JLabel filterLabel = new JLabel("Time Period:");
        filterLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        filterBar.add(filterLabel);
        filterBar.add(analysisPeriodCombo);
        
        analysisPeriodCombo.addActionListener(e -> refreshAnalysis());

        // Top Summary Panel
        JPanel goalCard = new JPanel(new java.awt.GridLayout(3, 1, 4, 4));
        goalCard.setBorder(BorderFactory.createTitledBorder("Daily Goal & Streaks"));

        goalProgressLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        streakLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        goalProgressBar.setStringPainted(true);

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        progressPanel.add(goalProgressBar, BorderLayout.CENTER);

        goalCard.add(goalProgressLabel);
        goalCard.add(progressPanel);
        goalCard.add(streakLabel);
        
        JPanel sessionsCard = new JPanel(new java.awt.GridLayout(3, 1, 4, 4));
        sessionsCard.setBorder(BorderFactory.createTitledBorder("Total Sessions"));
        totalSessionsValueLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        totalSessionsValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sessionsCard.add(totalSessionsValueLabel);
        avgSessionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        avgSessionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sessionsCard.add(avgSessionLabel);
        studyEfficiencyLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        studyEfficiencyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sessionsCard.add(studyEfficiencyLabel);
        
        JPanel durationCard = new JPanel(new java.awt.GridLayout(4, 1, 2, 2));
        durationCard.setBorder(BorderFactory.createTitledBorder("Total Duration"));
        totalDurationValueLabel.setFont(new Font("Monospaced", Font.BOLD, 28));
        totalDurationValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        durationCard.add(totalDurationValueLabel);
        activeDayAvgLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        activeDayAvgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        durationCard.add(activeDayAvgLabel);
        mostActiveSubjectLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        mostActiveSubjectLabel.setHorizontalAlignment(SwingConstants.CENTER);
        durationCard.add(mostActiveSubjectLabel);
        midSessionBreakLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        midSessionBreakLabel.setHorizontalAlignment(SwingConstants.CENTER);
        durationCard.add(midSessionBreakLabel);

        JPanel chartCard = new JPanel(new BorderLayout(8, 8));
        chartCard.setBorder(BorderFactory.createTitledBorder("Subject Distribution"));
        chartCard.add(subjectPieChart, BorderLayout.CENTER);

        JTable subjectTable = new JTable(subjectAnalysisModel);
        subjectTable.setRowHeight(24);
        JPanel subjectCard = new JPanel(new BorderLayout(8, 8));
        subjectCard.setBorder(BorderFactory.createTitledBorder("By Subject (All Time)"));
        JScrollPane subjectScroll = new JScrollPane(subjectTable);
        subjectScroll.setPreferredSize(new Dimension(200, 300));
        subjectCard.add(subjectScroll, BorderLayout.CENTER);

        JTable chapterTable = new JTable(chapterAnalysisModel);
        chapterTable.setRowHeight(24);
        JPanel chapterCard = new JPanel(new BorderLayout(8, 8));
        chapterCard.setBorder(BorderFactory.createTitledBorder("By Chapter in Subject (All Time)"));
        JScrollPane chapterScroll = new JScrollPane(chapterTable);
        chapterScroll.setPreferredSize(new Dimension(200, 300));
        chapterCard.add(chapterScroll, BorderLayout.CENTER);

        JTable activityTable = new JTable(activityAnalysisModel);
        activityTable.setRowHeight(24);
        JPanel activityCard = new JPanel(new BorderLayout(8, 8));
        activityCard.setBorder(BorderFactory.createTitledBorder("By Activity"));
        JScrollPane activityScroll = new JScrollPane(activityTable);
        activityScroll.setPreferredSize(new Dimension(200, 140));
        activityCard.add(activityScroll, BorderLayout.CENTER);

        JPanel dayOfWeekCard = new JPanel(new BorderLayout(8, 8));
        dayOfWeekCard.setBorder(BorderFactory.createTitledBorder("By Day of Week (Last 7 Days)"));
        dayOfWeekCard.add(weeklyBarChartPanel, BorderLayout.CENTER);

        JPanel timelineCard = new JPanel(new BorderLayout(8, 8));
        timelineCard.setBorder(BorderFactory.createTitledBorder("Today's Session Timeline"));
        timelineCard.add(dailyTimelinePanel, BorderLayout.CENTER);

        JPanel heatmapCard = new JPanel(new BorderLayout(8, 8));
        heatmapCard.setBorder(BorderFactory.createTitledBorder("Activity Heatmap"));
        heatmapCard.add(heatmapPanel, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ModernButton exportAnalysisBtn = new ModernButton("Export Current View");
        exportAnalysisBtn.addActionListener(e -> exportCurrentAnalysisView());
        bottomPanel.add(exportAnalysisBtn);
        ModernButton refreshBtn = new ModernButton("Refresh Analysis");
        refreshBtn.addActionListener(e -> refreshAnalysis());
        bottomPanel.add(refreshBtn);

        // Bento Box Layout Setup using GridBagLayout
        JPanel mainContent = new JPanel(new GridBagLayout());
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new java.awt.Insets(8, 8, 8, 8);

        // Row 0: Goal, Sessions, Duration (all 1x1)
        gbc.gridy = 0;
        gbc.weighty = 0.0;

        gbc.gridx = 0; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weightx = 1.0;
        mainContent.add(goalCard, gbc);

        gbc.gridx = 1; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weightx = 1.0;
        mainContent.add(sessionsCard, gbc);

        gbc.gridx = 2; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weightx = 1.0;
        mainContent.add(durationCard, gbc);

        // Row 1: Pie Chart (starts Col 0, spans 1 Col and 2 Rows)
        gbc.gridy = 1;
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.gridheight = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
        mainContent.add(chartCard, gbc);

        // Row 1: By Subject (starts Col 1, spans 1 Col and 2 Rows)
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.gridheight = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
        mainContent.add(subjectCard, gbc);

        // Row 1: By Chapter (starts Col 2, spans 1 Col and 2 Rows)
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.gridheight = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
        mainContent.add(chapterCard, gbc);

        // Row 3: By Activity (Wide card: Col 0 & 1, spans 2 Cols and 1 Row)
        gbc.gridy = 3; gbc.gridheight = 1; gbc.weighty = 0.5;
        
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 2.0;
        mainContent.add(activityCard, gbc);

        // Row 3: By Day of Week (Col 2, spans 1 Col and 1 Row)
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 1.0;
        mainContent.add(dayOfWeekCard, gbc);

        // Row 4: Daily Session Timeline (Spans all 3 columns, 1 row)
        gbc.gridy = 4;
        gbc.gridx = 0; gbc.gridwidth = 3; gbc.gridheight = 1; gbc.weightx = 1.0; gbc.weighty = 0.0;
        mainContent.add(timelineCard, gbc);

        // Row 5: Activity Heatmap (Spans all 3 columns, 1 row)
        gbc.gridy = 5;
        gbc.gridx = 0; gbc.gridwidth = 3; gbc.gridheight = 1; gbc.weightx = 1.0; gbc.weighty = 0.0;
        mainContent.add(heatmapCard, gbc);

        // Wrap the bento grid in a scroll pane to support smaller screens
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(filterBar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshAnalysis() {
        List<SessionRecord> rawSessions = storageService.loadSessions();
        
        // Filter based on selected time period
        String period = (String) analysisPeriodCombo.getSelectedItem();
        if (period == null) period = "All Time";
        
        if ("Custom Range...".equals(period)) {
            DateRange dr = promptDateRange("Select Analysis Date Range", LocalDate.now().minusDays(7), LocalDate.now());
            if (dr != null) {
                customAnalysisStartDate = dr.startDate;
                customAnalysisEndDate = dr.endDate;
                String customLabel = "Custom: " + dr.startDate + " to " + dr.endDate;
                
                // Temporarily disable action listener of combobox to avoid infinite loops
                java.awt.event.ActionListener[] listeners = analysisPeriodCombo.getActionListeners();
                for (java.awt.event.ActionListener l : listeners) {
                    analysisPeriodCombo.removeActionListener(l);
                }
                
                // Remove existing custom items starting with "Custom: "
                for (int i = 0; i < analysisPeriodCombo.getItemCount(); i++) {
                    String item = analysisPeriodCombo.getItemAt(i);
                    if (item != null && item.startsWith("Custom: ")) {
                        analysisPeriodCombo.removeItemAt(i);
                        break;
                    }
                }
                
                // Insert new custom item before "Custom Range..."
                int insertIndex = analysisPeriodCombo.getItemCount() - 1; // before "Custom Range..."
                analysisPeriodCombo.insertItemAt(customLabel, insertIndex);
                analysisPeriodCombo.setSelectedItem(customLabel);
                previousAnalysisPeriod = customLabel;
                
                // Re-enable action listeners
                for (java.awt.event.ActionListener l : listeners) {
                    analysisPeriodCombo.addActionListener(l);
                }
                
                period = customLabel;
            } else {
                // User cancelled, revert to previous selection
                java.awt.event.ActionListener[] listeners = analysisPeriodCombo.getActionListeners();
                for (java.awt.event.ActionListener l : listeners) {
                    analysisPeriodCombo.removeActionListener(l);
                }
                analysisPeriodCombo.setSelectedItem(previousAnalysisPeriod);
                for (java.awt.event.ActionListener l : listeners) {
                    analysisPeriodCombo.addActionListener(l);
                }
                return;
            }
        } else if (!period.startsWith("Custom: ")) {
            previousAnalysisPeriod = period;
            customAnalysisStartDate = null;
            customAnalysisEndDate = null;
        } else {
            // It is the custom label, e.g., "Custom: 2026-06-01 to 2026-06-10"
            // Extract dates
            try {
                String datesPart = period.substring("Custom: ".length());
                String[] parts = datesPart.split(" to ");
                customAnalysisStartDate = LocalDate.parse(parts[0]);
                customAnalysisEndDate = LocalDate.parse(parts[1]);
            } catch (Exception e) {
                customAnalysisStartDate = null;
                customAnalysisEndDate = null;
            }
        }
        
        final String finalPeriod = period;
        LocalDate today = LocalDate.now();
        List<SessionRecord> sessions = rawSessions.stream()
            .filter(s -> {
                LocalDate sDate = s.getStartTime().toLocalDate();
                switch (finalPeriod) {
                    case "Today":
                        return sDate.equals(today);
                    case "Yesterday":
                        return sDate.equals(today.minusDays(1));
                    case "This Week":
                        LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                        return !sDate.isBefore(startOfWeek) && !sDate.isAfter(today);
                    case "Last 7 Days":
                        return !sDate.isBefore(today.minusDays(6)) && !sDate.isAfter(today);
                    case "This Month":
                        return sDate.getYear() == today.getYear() && sDate.getMonth() == today.getMonth();
                    case "Last 30 Days":
                        return !sDate.isBefore(today.minusDays(29)) && !sDate.isAfter(today);
                    default:
                        if (finalPeriod.startsWith("Custom: ") && customAnalysisStartDate != null && customAnalysisEndDate != null) {
                            return !sDate.isBefore(customAnalysisStartDate) && !sDate.isAfter(customAnalysisEndDate);
                        }
                        return true; // All Time
                }
            })
            .collect(Collectors.toList());
        
        int totalSessions = sessions.size();
        long totalSeconds = sessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
        
        totalSessionsValueLabel.setText(String.valueOf(totalSessions));
        totalDurationValueLabel.setText(formatDuration(totalSeconds));

        // Average session duration
        if (totalSessions > 0) {
            avgSessionLabel.setText("Avg Duration: " + formatDuration(totalSeconds / totalSessions));
        } else {
            avgSessionLabel.setText("Avg Duration: 00:00:00");
        }

        // Active day average
        long uniqueDays = sessions.stream()
            .map(s -> s.getStartTime().toLocalDate())
            .distinct()
            .count();
        if (uniqueDays > 0) {
            activeDayAvgLabel.setText("Active Day Avg: " + formatDuration(totalSeconds / uniqueDays));
        } else {
            activeDayAvgLabel.setText("Active Day Avg: 00:00:00");
        }

        // Mid-session breaks & study efficiency
        long totalSpanSeconds = 0;
        long totalActiveSeconds = 0;
        for (SessionRecord s : sessions) {
            long span = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
            totalSpanSeconds += span;
            totalActiveSeconds += s.getDurationSeconds();
        }
        long totalMidSessionBreaks = Math.max(0, totalSpanSeconds - totalActiveSeconds);
        double efficiency = totalSpanSeconds > 0 ? ((double) totalActiveSeconds / totalSpanSeconds) * 100.0 : 100.0;

        if (totalSessions > 0) {
            midSessionBreakLabel.setText(String.format("Mid-Session Breaks: %s", formatDuration(totalMidSessionBreaks)));
            studyEfficiencyLabel.setText(String.format("Study Efficiency: %.1f%%", efficiency));
        } else {
            midSessionBreakLabel.setText("Mid-Session Breaks: 00:00:00");
            studyEfficiencyLabel.setText("Study Efficiency: 100.0%");
        }

        // Goal & Streak update
        GoalStreakStats stats = calculateGoalStreakStats();
        goalProgressLabel.setText(String.format("Today: %d / %d mins (%d%%)", 
            stats.todayMinutes, stats.dailyGoalMinutes, 
            stats.dailyGoalMinutes > 0 ? (stats.todayMinutes * 100 / stats.dailyGoalMinutes) : 0));
        
        int percent = stats.dailyGoalMinutes > 0 ? Math.min(100, stats.todayMinutes * 100 / stats.dailyGoalMinutes) : 0;
        goalProgressBar.setValue(percent);
        if (stats.todayGoalMet) {
            goalProgressBar.setForeground(new java.awt.Color(46, 139, 87));
        } else {
            goalProgressBar.setForeground(new java.awt.Color(70, 130, 180));
        }
        
        streakLabel.setText(String.format("Streak: %d days %s (Max: %d 🏆)", 
            stats.currentStreak, stats.currentStreak > 0 ? "🔥" : "💤", stats.maxStreak));
        
        // Subject breakdown
        subjectAnalysisModel.setRowCount(0);
        java.util.Map<String, Long> bySubjectAllTime = rawSessions.stream()
            .collect(Collectors.groupingBy(SessionRecord::getSubject, Collectors.summingLong(SessionRecord::getDurationSeconds)));
        
        bySubjectAllTime.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(entry -> {
                subjectAnalysisModel.addRow(new Object[]{
                    entry.getKey(),
                    formatDuration(entry.getValue())
                });
            });

        // Subject distribution chart and most active subject metrics (Filtered)
        java.util.Map<String, Long> bySubjectFiltered = sessions.stream()
            .collect(Collectors.groupingBy(SessionRecord::getSubject, Collectors.summingLong(SessionRecord::getDurationSeconds)));
        subjectPieChart.setData(bySubjectFiltered);

        String mostActive = bySubjectFiltered.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(entry -> entry.getKey() + " (" + formatDuration(entry.getValue()) + ")")
            .orElse("None");
        
        if (mostActive.length() > 25) {
            mostActive = mostActive.substring(0, 22) + "...";
        }
        mostActiveSubjectLabel.setText("Most Active: " + mostActive);

        // Chapter breakdown
        chapterAnalysisModel.setRowCount(0);
        java.util.Map<String, Long> byChapter = new java.util.TreeMap<>();
        for (SessionRecord session : rawSessions) {
            String desc = session.getDescription();
            if (desc.startsWith("Lecture: ")) {
                String chName = "Unknown Chapter";
                int chStart = desc.indexOf("Ch ");
                if (chStart != -1) {
                    int commaIndex = desc.indexOf(",", chStart);
                    if (commaIndex != -1) {
                        chName = desc.substring(chStart, commaIndex).trim();
                    } else {
                        chName = desc.substring(chStart).trim();
                    }
                }
                String key = session.getSubject() + " - " + chName;
                byChapter.put(key, byChapter.getOrDefault(key, 0L) + session.getDurationSeconds());
            }
        }
        for (java.util.Map.Entry<String, Long> entry : byChapter.entrySet()) {
            chapterAnalysisModel.addRow(new Object[]{
                entry.getKey(),
                formatDuration(entry.getValue())
            });
        }

        // Activity breakdown
        long dppSec = 0;
        long practiceSec = 0;
        long pyqSec = 0;
        long generalSec = 0;

        for (SessionRecord session : sessions) {
            String desc = session.getDescription();
            long sec = session.getDurationSeconds();
            if (desc.startsWith("Questions: ")) {
                String qType = desc.substring("Questions: ".length());
                if (qType.startsWith("DPP Questions")) {
                    dppSec += sec;
                } else if (qType.startsWith("Practice Book Questions")) {
                    practiceSec += sec;
                } else if (qType.startsWith("Previous Year Questions")) {
                    pyqSec += sec;
                } else {
                    generalSec += sec;
                }
            } else if (!desc.startsWith("Lecture: ")) {
                generalSec += sec;
            }
        }

        activityAnalysisModel.setRowCount(0);
        activityAnalysisModel.addRow(new Object[]{"DPP Questions", formatDuration(dppSec)});
        activityAnalysisModel.addRow(new Object[]{"Practice Book Questions", formatDuration(practiceSec)});
        activityAnalysisModel.addRow(new Object[]{"Previous Year Questions", formatDuration(pyqSec)});
        activityAnalysisModel.addRow(new Object[]{"General / Other", formatDuration(generalSec)});

        // Day of Week breakdown (Always Last 7 Days)
        List<SessionRecord> last7DaysSessions = rawSessions.stream()
            .filter(s -> {
                LocalDate sDate = s.getStartTime().toLocalDate();
                return !sDate.isBefore(today.minusDays(6)) && !sDate.isAfter(today);
            })
            .collect(Collectors.toList());
        java.util.Map<java.time.DayOfWeek, Long> byDay = last7DaysSessions.stream()
            .collect(Collectors.groupingBy(
                s -> s.getStartTime().getDayOfWeek(),
                Collectors.summingLong(SessionRecord::getDurationSeconds)
            ));
        weeklyBarChartPanel.setData(byDay);

        // Update daily timeline with today's sessions
        dailyTimelinePanel.setData(rawSessions);

        // Heatmap update (always last 365 days)
        LocalDate heatmapStart = today.minusDays(365);
        java.util.Map<LocalDate, Long> dailyDurations = rawSessions.stream()
            .filter(s -> !s.getStartTime().toLocalDate().isBefore(heatmapStart))
            .collect(Collectors.groupingBy(
                s -> s.getStartTime().toLocalDate(),
                Collectors.summingLong(SessionRecord::getDurationSeconds)
            ));
        heatmapPanel.setData(dailyDurations);
    }

    public void startStopwatch() {
        if (subjects.isEmpty()) {
            JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this, "Add at least one subject in the Subjects tab first.", "No Subjects", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!stopwatchStarted) {
            String selectedSubject = (String) stopwatchSubjectCombo.getSelectedItem();

            if (selectedSubject == null || selectedSubject.trim().isEmpty()) {
                JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this,
                    "Select a subject from the list before starting stopwatch.",
                    "No Subject Selected",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            stopwatchSubject = selectedSubject;
            stopwatchSubjectLabel.setText("Subject: " + stopwatchSubject);
            if (resumedSession != null) {
                if (resumedSession.getType() == SessionRecord.SessionType.STOPWATCH) {
                    stopwatchSessionStart = resumedSession.getStartTime();
                    stopwatchElapsedMillis = resumedSession.getDurationSeconds() * 1000;
                } else {
                    resumedSession = null;
                    stopwatchSessionStart = LocalDateTime.now();
                    stopwatchElapsedMillis = 0;
                }
            } else {
                stopwatchSessionStart = LocalDateTime.now();
                stopwatchElapsedMillis = 0;
            }
            stopwatchStarted = true;
            stopwatchBreakNotificationSent = false;
        }

        if (!stopwatchRunning) {
            stopwatchStartNano = System.nanoTime();
            stopwatchRunning = true;
            stopwatchUiTimer.start();
            updateStopwatchButtons();
        }
    }

    public void togglePauseStopwatch() {
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

    public void stopAndLogStopwatch() {
        if (!stopwatchStarted) {
            return;
        }

        if (stopwatchRunning) {
            pauseStopwatch();
        }

        long durationSeconds = Math.max(1, stopwatchElapsedMillis / 1000);
        LocalDateTime end = LocalDateTime.now();

        String activityType = (String) stopwatchActivityTypeCombo.getSelectedItem();
        String activityDetail = "";
        if ("General".equals(activityType)) {
            activityDetail = stopwatchActivityField.getText().trim();
        } else if ("Questions".equals(activityType)) {
            String qType = (String) stopwatchQuestionTypeCombo.getSelectedItem();
            String qDesc = stopwatchQuestionDescField.getText().trim();
            activityDetail = "Questions: " + qType + (qDesc.isEmpty() ? "" : ", " + qDesc);
        } else if ("Lecture".equals(activityType)) {
            String ch = stopwatchChapterField.getText().trim();
            String lec = stopwatchLectureField.getText().trim();
            activityDetail = "Lecture: Ch " + ch + ", Lec " + lec;
        }

        if (resumedSession != null && resumedSession.getType() == SessionRecord.SessionType.STOPWATCH) {
            List<SessionRecord> allSessions = storageService.loadSessions();
            boolean updated = false;
            for (int i = 0; i < allSessions.size(); i++) {
                SessionRecord s = allSessions.get(i);
                if (s.getStartTime().equals(resumedSession.getStartTime()) &&
                    s.getSubject().equalsIgnoreCase(resumedSession.getSubject()) &&
                    s.getType() == resumedSession.getType()) {
                    allSessions.set(i, new SessionRecord(
                        SessionRecord.SessionType.STOPWATCH,
                        stopwatchSubject,
                        stopwatchSessionStart,
                        end,
                        durationSeconds,
                        activityDetail
                    ));
                    updated = true;
                    break;
                }
            }
            if (updated) {
                storageService.saveSessions(allSessions);
            } else {
                storageService.appendSession(new SessionRecord(
                    SessionRecord.SessionType.STOPWATCH,
                    stopwatchSubject,
                    stopwatchSessionStart,
                    end,
                    durationSeconds,
                    activityDetail
                ));
            }
            resumedSession = null;
        } else {
            storageService.appendSession(new SessionRecord(
                SessionRecord.SessionType.STOPWATCH,
                stopwatchSubject,
                stopwatchSessionStart,
                end,
                durationSeconds,
                activityDetail
            ));
        }

        refreshSessionsTable();
        refreshExportAvailability();
        notifySessionLogged(durationSeconds);
        resetStopwatch();
        JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this, "Stopwatch session logged.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        if (miniWindow != null) {
            closeMiniWindow();
        }
    }

    public void resetStopwatch() {
        stopwatchRunning = false;
        stopwatchStarted = false;
        stopwatchElapsedMillis = 0;
        stopwatchStartNano = 0;
        stopwatchSubject = null;
        stopwatchSessionStart = null;
        resumedSession = null;
        stopwatchBreakNotificationSent = false;
        stopwatchUiTimer.stop();
        stopwatchSubjectLabel.setText("Subject: -");
        stopwatchTimeLabel.setText("00:00:00");
        stopwatchActivityField.setText("");
        stopwatchChapterField.setText("");
        stopwatchLectureField.setText("");
        stopwatchQuestionDescField.setText("");
        stopwatchActivityTypeCombo.setSelectedIndex(0);
        stopwatchQuestionTypeCombo.setSelectedIndex(0);
        updateStopwatchButtons();
    }

    private void updateStopwatchDisplay() {
        long elapsed = stopwatchElapsedMillis;
        if (stopwatchRunning) {
            elapsed += (System.nanoTime() - stopwatchStartNano) / 1_000_000;
        }

        long totalSeconds = elapsed / 1000;
        
        // 50-min break reminder (3000 seconds)
        if (totalSeconds >= 3000 && !stopwatchBreakNotificationSent) {
            if (storageService.loadBreakRemindersEnabled()) {
                sendNotification("Take a Break!", "You've been tracking for 50 minutes. Time to stretch and rest your eyes!", TrayIcon.MessageType.INFO);
            }
            stopwatchBreakNotificationSent = true;
        }

        checkGoalAchievement(totalSeconds);

        String formatted = formatDuration(totalSeconds);
        stopwatchTimeLabel.setText(formatted);
        if (miniWindow != null) {
            miniWindow.updateTime(formatted);
        }
    }

    private void updateStopwatchButtons() {
        stopwatchStartButton.setEnabled(!stopwatchRunning);
        stopwatchPauseResumeButton.setEnabled(stopwatchStarted);
        stopwatchPauseResumeButton.setText(stopwatchRunning ? "Pause" : "Resume");
        stopwatchStopButton.setEnabled(stopwatchStarted);
        stopwatchResetButton.setEnabled(stopwatchStarted);
        stopwatchSubjectCombo.setEnabled(!stopwatchStarted);
        stopwatchActivityTypeCombo.setEnabled(!stopwatchStarted);
        stopwatchActivityField.setEnabled(!stopwatchStarted);
        stopwatchQuestionTypeCombo.setEnabled(!stopwatchStarted);
        stopwatchQuestionDescField.setEnabled(!stopwatchStarted);
        stopwatchChapterField.setEnabled(!stopwatchStarted);
        stopwatchLectureField.setEnabled(!stopwatchStarted);
        if (miniWindow != null) {
            miniWindow.updateState();
        }
    }

    public void startTimer() {
        if (!timerStarted) {
            long h = (Integer) hoursSpinner.getValue();
            long m = (Integer) minutesSpinner.getValue();
            long s = (Integer) secondsSpinner.getValue();
            timerTotalSeconds = h * 3600 + m * 60 + s;
            timerRemainingSeconds = timerTotalSeconds;

            if (timerTotalSeconds <= 0) {
                JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this, "Set a time greater than 00:00:00.", "Invalid Time", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (resumedSession != null) {
                if (resumedSession.getType() == SessionRecord.SessionType.TIMER) {
                    timerSessionStart = resumedSession.getStartTime();
                } else {
                    resumedSession = null;
                    timerSessionStart = LocalDateTime.now();
                }
            } else {
                timerSessionStart = LocalDateTime.now();
            }
            timerStarted = true;
            timerBreakNotificationSent = false;
        }

        if (!timerRunning) {
            timerRunning = true;
            timerTick.start();
            updateTimerButtons();
        }
    }

    public void togglePauseTimer() {
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

    public void stopTimer() {
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
            notifySessionLogged(elapsed);
            JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this, "Timer session logged.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        }

        resetTimer();
        if (miniWindow != null) {
            closeMiniWindow();
        }
    }

    public void resetTimer() {
        timerTick.stop();
        timerRunning = false;
        timerStarted = false;
        timerTotalSeconds = 0;
        timerRemainingSeconds = 0;
        timerSessionStart = null;
        resumedSession = null;
        timerBreakNotificationSent = false;
        timerTimeLabel.setText("00:00:00");
        updateTimerButtons();
    }

    private void onTimerTick() {
        if (!timerRunning) {
            return;
        }

        timerRemainingSeconds--;
        
        long elapsedSec = timerTotalSeconds - timerRemainingSeconds;
        if (elapsedSec >= 3000 && !timerBreakNotificationSent) {
            if (storageService.loadBreakRemindersEnabled()) {
                sendNotification("Take a Break!", "You've been tracking for 50 minutes. Time to stretch and rest your eyes!", TrayIcon.MessageType.INFO);
            }
            timerBreakNotificationSent = true;
        }

        checkGoalAchievement(elapsedSec);

        String formatted = formatDuration(Math.max(0, timerRemainingSeconds));
        timerTimeLabel.setText(formatted);
        if (miniWindow != null) {
            miniWindow.updateTime(formatted);
        }

        if (timerRemainingSeconds <= 0) {
            timerTick.stop();
            timerRunning = false;
            long elapsed = timerTotalSeconds;
            logTimerSession(elapsed);
            notifySessionLogged(elapsed);
            JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this, "Timer finished and session logged.", "Completed", JOptionPane.INFORMATION_MESSAGE);
            resetTimer();
            if (miniWindow != null) {
                closeMiniWindow();
            }
        }
    }

    private void logTimerSession(long elapsedSeconds) {
        String subject = (String) timerSubjectCombo.getSelectedItem();
        if (subject == null || subject.isBlank()) {
            subject = "General";
        }

        if (resumedSession != null && resumedSession.getType() == SessionRecord.SessionType.TIMER) {
            List<SessionRecord> allSessions = storageService.loadSessions();
            boolean updated = false;
            for (int i = 0; i < allSessions.size(); i++) {
                SessionRecord s = allSessions.get(i);
                if (s.getStartTime().equals(resumedSession.getStartTime()) &&
                    s.getSubject().equalsIgnoreCase(resumedSession.getSubject()) &&
                    s.getType() == resumedSession.getType()) {
                    long newDuration = resumedSession.getDurationSeconds() + elapsedSeconds;
                    allSessions.set(i, new SessionRecord(
                        SessionRecord.SessionType.TIMER,
                        subject,
                        timerSessionStart,
                        LocalDateTime.now(),
                        newDuration,
                        resumedSession.getDescription()
                    ));
                    updated = true;
                    break;
                }
            }
            if (updated) {
                storageService.saveSessions(allSessions);
            } else {
                storageService.appendSession(new SessionRecord(
                    SessionRecord.SessionType.TIMER,
                    subject,
                    timerSessionStart != null ? timerSessionStart : LocalDateTime.now(),
                    LocalDateTime.now(),
                    elapsedSeconds,
                    resumedSession.getDescription()
                ));
            }
            resumedSession = null;
        } else {
            storageService.appendSession(new SessionRecord(
                SessionRecord.SessionType.TIMER,
                subject,
                timerSessionStart != null ? timerSessionStart : LocalDateTime.now(),
                LocalDateTime.now(),
                elapsedSeconds
            ));
        }

        refreshSessionsTable();
        refreshExportAvailability();
    }

    private void refreshSessionsTable() {
        sessionsTableModel.setRowCount(0);

        List<SessionRecord> rawSessions = storageService.loadSessions();
        
        String selectedSubject = logsSubjectFilterCombo != null ? (String) logsSubjectFilterCombo.getSelectedItem() : "All Subjects";
        String selectedMode = logsModeFilterCombo != null ? (String) logsModeFilterCombo.getSelectedItem() : "All Modes";
        String datePeriod = logsDateFilterCombo != null ? (String) logsDateFilterCombo.getSelectedItem() : "All Dates";
        if (datePeriod == null) datePeriod = "All Dates";

        if ("Custom Range...".equals(datePeriod)) {
            DateRange dr = promptDateRange("Select Log Date Range", LocalDate.now().minusDays(7), LocalDate.now());
            if (dr != null) {
                customLogsStartDate = dr.startDate;
                customLogsEndDate = dr.endDate;
                String customLabel = "Custom: " + dr.startDate + " to " + dr.endDate;
                
                // Temporarily disable action listener of combobox to avoid infinite loops
                java.awt.event.ActionListener[] listeners = logsDateFilterCombo.getActionListeners();
                for (java.awt.event.ActionListener l : listeners) {
                    logsDateFilterCombo.removeActionListener(l);
                }
                
                // Remove existing custom items starting with "Custom: "
                for (int i = 0; i < logsDateFilterCombo.getItemCount(); i++) {
                    String item = logsDateFilterCombo.getItemAt(i);
                    if (item != null && item.startsWith("Custom: ")) {
                        logsDateFilterCombo.removeItemAt(i);
                        break;
                    }
                }
                
                // Insert new custom item before "Custom Range..."
                int insertIndex = logsDateFilterCombo.getItemCount() - 1; // before "Custom Range..."
                logsDateFilterCombo.insertItemAt(customLabel, insertIndex);
                logsDateFilterCombo.setSelectedItem(customLabel);
                previousLogsDatePeriod = customLabel;
                
                // Re-enable action listeners
                for (java.awt.event.ActionListener l : listeners) {
                    logsDateFilterCombo.addActionListener(l);
                }
                
                datePeriod = customLabel;
            } else {
                // User cancelled, revert to previous selection
                java.awt.event.ActionListener[] listeners = logsDateFilterCombo.getActionListeners();
                for (java.awt.event.ActionListener l : listeners) {
                    logsDateFilterCombo.removeActionListener(l);
                }
                logsDateFilterCombo.setSelectedItem(previousLogsDatePeriod);
                for (java.awt.event.ActionListener l : listeners) {
                    logsDateFilterCombo.addActionListener(l);
                }
                return;
            }
        } else if (!datePeriod.startsWith("Custom: ")) {
            previousLogsDatePeriod = datePeriod;
            customLogsStartDate = null;
            customLogsEndDate = null;
        } else {
            // It is the custom label, extract dates
            try {
                String datesPart = datePeriod.substring("Custom: ".length());
                String[] parts = datesPart.split(" to ");
                customLogsStartDate = LocalDate.parse(parts[0]);
                customLogsEndDate = LocalDate.parse(parts[1]);
            } catch (Exception e) {
                customLogsStartDate = null;
                customLogsEndDate = null;
            }
        }

        String searchKeyword = logsSearchField != null ? logsSearchField.getText().trim().toLowerCase() : "";

        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM);
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.MEDIUM);

        final String finalDatePeriod = datePeriod;

        List<SessionRecord> sessions = rawSessions.stream()
            .filter(session -> {
                // Filter by Subject
                if (selectedSubject != null && !selectedSubject.equals("All Subjects")) {
                    if (!session.getSubject().equalsIgnoreCase(selectedSubject)) {
                        return false;
                    }
                }
                // Filter by Mode
                if (selectedMode != null && !selectedMode.equals("All Modes")) {
                    if (selectedMode.equalsIgnoreCase("Stopwatch") && session.getType() != SessionRecord.SessionType.STOPWATCH) {
                        return false;
                    }
                    if (selectedMode.equalsIgnoreCase("Timer") && session.getType() != SessionRecord.SessionType.TIMER) {
                        return false;
                    }
                }
                // Filter by Date Period
                if (finalDatePeriod != null && !finalDatePeriod.equals("All Dates")) {
                    LocalDate sDate = session.getStartTime().toLocalDate();
                    LocalDate today = LocalDate.now();
                    switch (finalDatePeriod) {
                        case "Today":
                            if (!sDate.equals(today)) return false;
                            break;
                        case "Yesterday":
                            if (!sDate.equals(today.minusDays(1))) return false;
                            break;
                        case "This Week":
                            LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                            if (sDate.isBefore(startOfWeek) || sDate.isAfter(today)) return false;
                            break;
                        case "This Month":
                            if (sDate.getYear() != today.getYear() || sDate.getMonth() != today.getMonth()) return false;
                            break;
                        default:
                            if (finalDatePeriod.startsWith("Custom: ") && customLogsStartDate != null && customLogsEndDate != null) {
                                if (sDate.isBefore(customLogsStartDate) || sDate.isAfter(customLogsEndDate)) return false;
                            }
                            break;
                    }
                }
                // Filter by search query on description
                if (!searchKeyword.isEmpty()) {
                    String desc = session.getDescription().toLowerCase();
                    if (!desc.contains(searchKeyword)) {
                        return false;
                    }
                }
                // Filter by column checklists
                for (java.util.Map.Entry<Integer, java.util.Set<String>> entry : columnFilters.entrySet()) {
                    int col = entry.getKey();
                    java.util.Set<String> allowedValues = entry.getValue();
                    if (allowedValues != null) {
                        String value = getSessionValueForColumn(session, col, dateFormatter, timeFormatter);
                        if (!allowedValues.contains(value)) {
                            return false;
                        }
                    }
                }
                return true;
            })
            .collect(Collectors.toList());

        // Reverse to show newest (last logged) first by default
        java.util.Collections.reverse(sessions);

        // Update the displayed sessions reference
        displayedSessions.clear();
        displayedSessions.addAll(sessions);

        for (SessionRecord session : sessions) {
            String dateStr = getSessionValueForColumn(session, 0, dateFormatter, timeFormatter);
            String startTimeStr = session.getStartTime().toLocalTime().format(timeFormatter);
            String endTimeStr = session.getEndTime().toLocalTime().format(timeFormatter);
            sessionsTableModel.addRow(new Object[]{
                dateStr,
                session.getType().name(),
                session.getSubject(),
                session.getDescription(),
                startTimeStr,
                endTimeStr,
                formatDuration(session.getDurationSeconds())
            });
        }
        refreshAnalysis();
        refreshInsights();
        checkGoalAchievement(0);
        refreshStopwatchSubjects();
        refreshTimerSubjects();
    }

    private void persistSubjects() {
        storageService.saveSubjects(subjects);
        subjectsListModel.clear();
        subjects.stream().sorted(String::compareToIgnoreCase).forEach(subjectsListModel::addElement);
        refreshStopwatchSubjects();
        refreshTimerSubjects();
        refreshLogsSubjects();
    }

    public List<String> getSubjectsSortedByUsage() {
        List<SessionRecord> sessions = storageService.loadSessions();
        java.util.Map<String, Long> subjectUsage = new java.util.HashMap<>();
        for (SessionRecord session : sessions) {
            String sub = session.getSubject();
            subjectUsage.put(sub, subjectUsage.getOrDefault(sub, 0L) + session.getDurationSeconds());
        }

        List<String> sortedSubjects = new ArrayList<>(subjects);
        sortedSubjects.sort((a, b) -> {
            long usageA = subjectUsage.getOrDefault(a, 0L);
            long usageB = subjectUsage.getOrDefault(b, 0L);
            if (usageA != usageB) {
                return Long.compare(usageB, usageA); // Descending
            }
            return a.compareToIgnoreCase(b); // Alphabetical fallback
        });
        return sortedSubjects;
    }

    private void refreshStopwatchSubjects() {
        String previous = (String) stopwatchSubjectCombo.getSelectedItem();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        List<String> sorted = getSubjectsSortedByUsage();
        for (String subject : sorted) {
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
        List<String> sorted = getSubjectsSortedByUsage();
        for (String subject : sorted) {
            model.addElement(subject);
        }
        timerSubjectCombo.setModel(model);

        if (previous != null && subjects.contains(previous)) {
            timerSubjectCombo.setSelectedItem(previous);
        } else if (!subjects.isEmpty()) {
            timerSubjectCombo.setSelectedIndex(0);
        }
    }

    private void refreshLogsSubjects() {
        String previous = (String) logsSubjectFilterCombo.getSelectedItem();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("All Subjects");
        List<String> sorted = getSubjectsSortedByUsage();
        for (String subject : sorted) {
            model.addElement(subject);
        }
        logsSubjectFilterCombo.setModel(model);

        if (previous != null) {
            logsSubjectFilterCombo.setSelectedItem(previous);
        } else {
            logsSubjectFilterCombo.setSelectedIndex(0);
        }
    }

    private void refreshExportAvailability() {
        exportWeeklyButton.setEnabled(true);
        exportMonthlyButton.setEnabled(true);
        exportInfoLabel.setText("Export weekly reports (previous calendar week) or monthly reports (previous calendar month) on demand.");
    }

    private void exportWeeklyReport() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate weekStart = lastMonday.minusWeeks(1);
        LocalDate weekEnd = lastMonday.minusDays(1);

        List<SessionRecord> sessions = storageService.loadSessions().stream()
            .filter(session -> {
                LocalDate sessionDate = session.getEndTime().toLocalDate();
                return !sessionDate.isBefore(weekStart) && !sessionDate.isAfter(weekEnd);
            })
            .collect(Collectors.toList());

        if (sessions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No sessions found for the previous calendar week (" + weekStart + " to " + weekEnd + ") to export.",
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
                "No sessions found for previous month (" + previousMonth + ") to export.",
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

    private void exportCustomRangeReport() {
        DateRange dr = promptDateRange("Select Export Date Range", LocalDate.now().minusDays(7), LocalDate.now());
        if (dr == null) {
            return;
        }

        List<SessionRecord> sessions = storageService.loadSessions().stream()
            .filter(session -> {
                LocalDate sessionDate = session.getEndTime().toLocalDate();
                return !sessionDate.isBefore(dr.startDate) && !sessionDate.isAfter(dr.endDate);
            })
            .collect(Collectors.toList());

        if (sessions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No sessions found for the selected date range (" + dr.startDate + " to " + dr.endDate + ") to export.",
                "No Data",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String period = dr.startDate + " to " + dr.endDate;
        String fileName = "timelog-custom-" + dr.startDate + "_to_" + dr.endDate + ".xlsx";
        Path outputFile = storageService.getAppDirectory().resolve(fileName);
        exportService.exportReport(outputFile, sessions, period);

        JOptionPane.showMessageDialog(this,
            "Exported report to:\n" + outputFile,
            "Export Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportCurrentAnalysisView() {
        List<SessionRecord> rawSessions = storageService.loadSessions();
        
        String period = (String) analysisPeriodCombo.getSelectedItem();
        if (period == null) period = "All Time";
        
        final String finalPeriod = period;
        LocalDate today = LocalDate.now();
        List<SessionRecord> sessions = rawSessions.stream()
            .filter(s -> {
                LocalDate sDate = s.getStartTime().toLocalDate();
                switch (finalPeriod) {
                    case "Today":
                        return sDate.equals(today);
                    case "Yesterday":
                        return sDate.equals(today.minusDays(1));
                    case "This Week":
                        LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                        return !sDate.isBefore(startOfWeek) && !sDate.isAfter(today);
                    case "Last 7 Days":
                        return !sDate.isBefore(today.minusDays(6)) && !sDate.isAfter(today);
                    case "This Month":
                        return sDate.getYear() == today.getYear() && sDate.getMonth() == today.getMonth();
                    case "Last 30 Days":
                        return !sDate.isBefore(today.minusDays(29)) && !sDate.isAfter(today);
                    default:
                        if (finalPeriod.startsWith("Custom: ") && customAnalysisStartDate != null && customAnalysisEndDate != null) {
                            return !sDate.isBefore(customAnalysisStartDate) && !sDate.isAfter(customAnalysisEndDate);
                        }
                        return true; // All Time
                }
            })
            .collect(Collectors.toList());

        if (sessions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No sessions found for the current period (" + period + ") to export.",
                "No Data",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String safePeriod = period.replace(":", "").replace(" ", "_");
        String fileName = "timelog-analysis-" + safePeriod + ".xlsx";
        Path outputFile = storageService.getAppDirectory().resolve(fileName);
        exportService.exportReport(outputFile, sessions, period);

        JOptionPane.showMessageDialog(this,
            "Exported report to:\n" + outputFile,
            "Export Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private static class DateRange {
        LocalDate startDate;
        LocalDate endDate;
    }

    private DateRange promptDateRange(String title, LocalDate defaultStart, LocalDate defaultEnd) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel startPanel = new JPanel(new BorderLayout(6, 6));
        startPanel.setOpaque(false);
        JLabel startLabel = new JLabel("Start: " + defaultStart.toString(), SwingConstants.CENTER);
        startLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        startPanel.add(startLabel, BorderLayout.NORTH);
        
        final LocalDate[] selectedStart = {defaultStart};
        final LocalDate[] selectedEnd = {defaultEnd};

        CalendarPanel startCalendar = new CalendarPanel(defaultStart, date -> {
            selectedStart[0] = date;
            startLabel.setText("Start: " + date.toString());
        });
        startCalendar.setPreferredSize(new Dimension(280, 240));
        startPanel.add(startCalendar, BorderLayout.CENTER);

        JPanel endPanel = new JPanel(new BorderLayout(6, 6));
        endPanel.setOpaque(false);
        JLabel endLabel = new JLabel("End: " + defaultEnd.toString(), SwingConstants.CENTER);
        endLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        endPanel.add(endLabel, BorderLayout.NORTH);

        CalendarPanel endCalendar = new CalendarPanel(defaultEnd, date -> {
            selectedEnd[0] = date;
            endLabel.setText("End: " + date.toString());
        });
        endCalendar.setPreferredSize(new Dimension(280, 240));
        endPanel.add(endCalendar, BorderLayout.CENTER);

        panel.add(startPanel);
        panel.add(endPanel);

        ThemeManager.applyTheme(panel, ThemeManager.getColors(ThemeManager.loadTheme()));

        int result = JOptionPane.showConfirmDialog(miniWindow != null ? miniWindow : this,
            panel,
            title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            LocalDate startLocal = selectedStart[0];
            LocalDate endLocal = selectedEnd[0];

            if (startLocal.isAfter(endLocal)) {
                JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this,
                    "Start date cannot be after end date.",
                    "Invalid Range",
                    JOptionPane.WARNING_MESSAGE);
                return null;
            }

            DateRange dr = new DateRange();
            dr.startDate = startLocal;
            dr.endDate = endLocal;
            return dr;
        }
        return null;
    }

    private static class CalendarPanel extends JPanel {
        private LocalDate selectedDate;
        private YearMonth displayedMonth;
        
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel daysGrid = new JPanel(new java.awt.GridLayout(0, 7, 2, 2));
        private final java.util.function.Consumer<LocalDate> onDateSelected;

        public CalendarPanel(LocalDate initialDate, java.util.function.Consumer<LocalDate> onDateSelected) {
            this.selectedDate = initialDate;
            this.displayedMonth = YearMonth.from(initialDate);
            this.onDateSelected = onDateSelected;

            setLayout(new BorderLayout(4, 4));
            
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            
            ModernButton prevBtn = new ModernButton("<");
            prevBtn.setPreferredSize(new Dimension(32, 24));
            prevBtn.setMargin(new Insets(0, 0, 0, 0));
            prevBtn.addActionListener(e -> {
                displayedMonth = displayedMonth.minusMonths(1);
                updateCalendar();
            });

            ModernButton nextBtn = new ModernButton(">");
            nextBtn.setPreferredSize(new Dimension(32, 24));
            nextBtn.setMargin(new Insets(0, 0, 0, 0));
            nextBtn.addActionListener(e -> {
                displayedMonth = displayedMonth.plusMonths(1);
                updateCalendar();
            });

            monthLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            header.add(prevBtn, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(nextBtn, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            daysGrid.setOpaque(false);
            add(daysGrid, BorderLayout.CENTER);
            
            updateCalendar();
        }

        private void updateCalendar() {
            daysGrid.removeAll();
            
            String monthName = displayedMonth.getMonth().name().charAt(0) + displayedMonth.getMonth().name().substring(1).toLowerCase();
            monthLabel.setText(monthName + " " + displayedMonth.getYear());

            String[] headers = {"M", "T", "W", "T", "F", "S", "S"};
            for (String h : headers) {
                JLabel lbl = new JLabel(h, SwingConstants.CENTER);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                daysGrid.add(lbl);
            }

            LocalDate firstOfMonth = displayedMonth.atDay(1);
            int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
            
            for (int i = 1; i < dayOfWeek; i++) {
                daysGrid.add(new JLabel(""));
            }

            int daysInMonth = displayedMonth.lengthOfMonth();
            ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
            
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = displayedMonth.atDay(day);
                
                ModernButton dayBtn = new ModernButton(String.valueOf(day));
                dayBtn.setPreferredSize(new Dimension(32, 26));
                dayBtn.setMargin(new Insets(0, 0, 0, 0));
                dayBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
                dayBtn.putClientProperty("themeOverride", true);
                
                if (date.equals(selectedDate)) {
                    dayBtn.setBackground(colors.accent);
                    dayBtn.setForeground(java.awt.Color.WHITE);
                    dayBtn.setBorderColor(colors.accent);
                } else {
                    dayBtn.setBackground(colors.cardBg);
                    dayBtn.setForeground(colors.text);
                    dayBtn.setBorderColor(colors.border);
                }

                dayBtn.addActionListener(e -> {
                    selectedDate = date;
                    updateCalendar();
                    if (onDateSelected != null) {
                        onDateSelected.accept(date);
                    }
                });
                
                daysGrid.add(dayBtn);
            }

            daysGrid.revalidate();
            daysGrid.repaint();
        }
    }

    private void updateTimerButtons() {
        timerStartButton.setEnabled(!timerRunning);
        timerPauseResumeButton.setEnabled(timerStarted);
        timerPauseResumeButton.setText(timerRunning ? "Pause" : "Resume");
        timerStopButton.setEnabled(timerStarted);
        timerResetButton.setEnabled(timerStarted);
        if (miniWindow != null) {
            miniWindow.updateState();
        }
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void openMiniWindow(SessionRecord.SessionType mode) {
        if (miniWindow != null) {
            miniWindow.dispose();
        }
        miniWindow = new MiniWindow(this, mode);
        miniWindow.setVisible(true);
        this.setVisible(false);
        System.gc();
    }

    public void closeMiniWindow() {
        if (miniWindow != null) {
            miniWindow.dispose();
            miniWindow = null;
        }
        this.setVisible(true);
        refreshSessionsTable();
        System.gc();
    }

    public String getStopwatchSubject() {
        return stopwatchSubject != null ? stopwatchSubject : (String) stopwatchSubjectCombo.getSelectedItem();
    }

    public boolean isStopwatchStarted() {
        return stopwatchStarted;
    }

    public boolean isStopwatchRunning() {
        return stopwatchRunning;
    }

    public String getTimerSubject() {
        String subject = (String) timerSubjectCombo.getSelectedItem();
        return (subject != null && !subject.isBlank()) ? subject : "General";
    }

    public boolean isTimerStarted() {
        return timerStarted;
    }

    public boolean isTimerRunning() {
        return timerRunning;
    }

    public void showShortcutsHelp() {
        String msg = "Time Logger Keyboard Shortcuts\n\n" +
            "=== Main Window Shortcuts (Alt + Key) ===\n" +
            "Stopwatch:\n" +
            "  Alt + S : Start Stopwatch\n" +
            "  Alt + P : Pause / Resume Stopwatch\n" +
            "  Alt + L : Stop & Log Stopwatch Session\n" +
            "  Alt + R : Reset Stopwatch\n" +
            "  Alt + M : Enter Mini Mode\n\n" +
            "Timer:\n" +
            "  Alt + S : Start Timer\n" +
            "  Alt + P : Pause / Resume Timer\n" +
            "  Alt + T : Stop Timer\n" +
            "  Alt + R : Reset Timer\n" +
            "  Alt + M : Enter Mini Mode\n\n" +
            "=== Mini Mode Window Shortcuts ===\n" +
            "  Space   : Start / Pause / Resume\n" +
            "  Enter   : Stop & Log (Stopwatch) or Stop (Timer)\n" +
            "  Escape  : Maximize (Restore Full Mode)\n" +
            "  Alt + S : Start\n" +
            "  Alt + P : Pause / Resume\n" +
            "  Alt + L / Alt + T : Stop / Log\n" +
            "  Alt + R : Reset\n" +
            "  Alt + M : Maximize";
        
        JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this,
            msg,
            "Keyboard Shortcuts Help",
            JOptionPane.INFORMATION_MESSAGE);
    }

    public static class GoalStreakStats {
        public int todayMinutes;
        public int dailyGoalMinutes;
        public int currentStreak;
        public int maxStreak;
        public boolean todayGoalMet;
    }

    public GoalStreakStats calculateGoalStreakStats() {
        GoalStreakStats stats = new GoalStreakStats();
        stats.dailyGoalMinutes = storageService.loadDailyGoalMinutes();
        
        List<SessionRecord> sessions = storageService.loadSessions();
        LocalDate today = LocalDate.now();
        
        // Group seconds by date
        java.util.Map<LocalDate, Long> secondsPerDay = new java.util.HashMap<>();
        for (SessionRecord session : sessions) {
            LocalDate date = session.getStartTime().toLocalDate();
            long sec = session.getDurationSeconds();
            secondsPerDay.put(date, secondsPerDay.getOrDefault(date, 0L) + sec);
        }
        
        stats.todayMinutes = (int) (secondsPerDay.getOrDefault(today, 0L) / 60);
        stats.todayGoalMet = stats.todayMinutes >= stats.dailyGoalMinutes;
        
        // Helper to check if goal was met on a date
        java.util.function.Predicate<LocalDate> metGoal = date -> {
            long sec = secondsPerDay.getOrDefault(date, 0L);
            return (sec / 60) >= stats.dailyGoalMinutes;
        };
        
        // 1. Calculate Current Streak
        int currentStreak = 0;
        if (metGoal.test(today)) {
            LocalDate d = today;
            while (metGoal.test(d)) {
                currentStreak++;
                d = d.minusDays(1);
            }
        } else if (metGoal.test(today.minusDays(1))) {
            LocalDate d = today.minusDays(1);
            while (metGoal.test(d)) {
                currentStreak++;
                d = d.minusDays(1);
            }
        }
        stats.currentStreak = currentStreak;
        
        // 2. Calculate Max Streak
        if (sessions.isEmpty()) {
            stats.maxStreak = 0;
            return stats;
        }
        
        LocalDate earliest = sessions.stream()
            .map(s -> s.getStartTime().toLocalDate())
            .min(LocalDate::compareTo)
            .orElse(today);
            
        int maxStreak = 0;
        int running = 0;
        LocalDate d = earliest;
        while (!d.isAfter(today)) {
            if (metGoal.test(d)) {
                running++;
                maxStreak = Math.max(maxStreak, running);
            } else {
                running = 0;
            }
            d = d.plusDays(1);
        }
        stats.maxStreak = maxStreak;
        
        return stats;
    }

    private void promptSetDailyGoal() {
        int currentGoal = storageService.loadDailyGoalMinutes();
        int currentHours = currentGoal / 60;
        int currentMins = currentGoal % 60;
        
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        JSpinner hrSpinner = new JSpinner(new SpinnerNumberModel(currentHours, 0, 24, 1));
        JSpinner minSpinner = new JSpinner(new SpinnerNumberModel(currentMins, 0, 59, 1));
        
        panel.add(new JLabel("Hours:"));
        panel.add(hrSpinner);
        panel.add(new JLabel("Minutes:"));
        panel.add(minSpinner);
        
        int result = JOptionPane.showConfirmDialog(miniWindow != null ? miniWindow : this,
            panel,
            "Set Daily Goal",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
            
        if (result == JOptionPane.OK_OPTION) {
            int hrs = (Integer) hrSpinner.getValue();
            int mins = (Integer) minSpinner.getValue();
            int totalMins = hrs * 60 + mins;
            if (totalMins <= 0) {
                JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this,
                    "Goal must be greater than 0 minutes.",
                    "Invalid Goal",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            storageService.saveDailyGoalMinutes(totalMins);
            refreshAnalysis();
            JOptionPane.showMessageDialog(miniWindow != null ? miniWindow : this,
                "Daily goal saved successfully!",
                "Goal Saved",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void updateTheme(ThemeManager.AppTheme theme) {
        ThemeManager.saveTheme(theme);
        ThemeManager.ThemeColors colors = ThemeManager.getColors(theme);
        ThemeManager.applyTheme(this, colors);
        if (miniWindow != null) {
            ThemeManager.applyTheme(miniWindow, colors);
        }
        this.repaint();
    }

    // --- Excel-like Filtering and Sorting helpers ---

    private String getSessionValueForColumn(SessionRecord session, int col, java.time.format.DateTimeFormatter dateFormatter, java.time.format.DateTimeFormatter timeFormatter) {
        switch (col) {
            case 0: {
                String dayOfWeekName = session.getStartTime().getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault());
                return session.getStartTime().toLocalDate().format(dateFormatter) + " (" + dayOfWeekName + ")";
            }
            case 1: return session.getType().name();
            case 2: return session.getSubject();
            case 3: return session.getDescription();
            case 4: return session.getStartTime().toLocalTime().format(timeFormatter);
            case 5: return session.getEndTime().toLocalTime().format(timeFormatter);
            case 6: return formatDuration(session.getDurationSeconds());
            default: return "";
        }
    }

    private java.util.List<String> getUniqueValuesForColumn(int colIndex) {
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM);
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.MEDIUM);
        
        List<SessionRecord> rawSessions = storageService.loadSessions();
        return rawSessions.stream()
            .map(s -> getSessionValueForColumn(s, colIndex, dateFormatter, timeFormatter))
            .distinct()
            .sorted(String::compareToIgnoreCase)
            .collect(Collectors.toList());
    }

    private void sortTableColumn(int colIndex, boolean ascending) {
        javax.swing.table.TableRowSorter<?> sorter = (javax.swing.table.TableRowSorter<?>) logsTable.getRowSorter();
        if (sorter != null) {
            java.util.List<javax.swing.RowSorter.SortKey> sortKeys = new java.util.ArrayList<>();
            sortKeys.add(new javax.swing.RowSorter.SortKey(colIndex, ascending ? javax.swing.SortOrder.ASCENDING : javax.swing.SortOrder.DESCENDING));
            sorter.setSortKeys(sortKeys);
            sorter.sort();
        }
    }

    private void showFilterPopup(int colIndex, java.awt.Component invoker, int x, int y) {
        ColumnFilterPopup popup = new ColumnFilterPopup(colIndex);
        popup.show(invoker, x, y);
    }

    private class FilterHeaderRenderer implements javax.swing.table.TableCellRenderer {
        private final javax.swing.table.TableCellRenderer defaultRenderer;

        public FilterHeaderRenderer(javax.swing.table.TableCellRenderer defaultRenderer) {
            this.defaultRenderer = defaultRenderer;
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component c = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                boolean hasActiveFilter = columnFilters.containsKey(column);
                String text = value.toString();
                if (hasActiveFilter) {
                    label.setText(text + " ⛛");
                } else {
                    label.setText(text + " ▾");
                }
            }
            return c;
        }
    }

    private class ColumnFilterPopup extends javax.swing.JPopupMenu {
        private final int colIndex;
        private final JTextField searchField = new JTextField(12);
        private final JPanel listPanel = new JPanel();
        private final JScrollPane scrollPane = new JScrollPane(listPanel);
        private final javax.swing.JCheckBox selectAllBox = new javax.swing.JCheckBox("(Select All)", true);
        private final java.util.List<javax.swing.JCheckBox> valueBoxes = new java.util.ArrayList<>();
        private final java.util.List<String> uniqueValues;
        
        public ColumnFilterPopup(int colIndex) {
            this.colIndex = colIndex;
            this.uniqueValues = getUniqueValuesForColumn(colIndex);
            
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            
            // Sort options
            JPanel sortPanel = new JPanel(new java.awt.GridLayout(2, 1, 2, 2));
            sortPanel.setOpaque(false);
            
            JMenuItem sortAsc = new JMenuItem("Sort Ascending (A to Z)");
            sortAsc.addActionListener(e -> {
                sortTableColumn(colIndex, true);
                ColumnFilterPopup.this.setVisible(false);
            });
            JMenuItem sortDesc = new JMenuItem("Sort Descending (Z to A)");
            sortDesc.addActionListener(e -> {
                sortTableColumn(colIndex, false);
                ColumnFilterPopup.this.setVisible(false);
            });
            sortPanel.add(sortAsc);
            sortPanel.add(sortDesc);
            add(sortPanel, BorderLayout.NORTH);
            
            // Search and list container
            JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
            centerPanel.setOpaque(false);
            
            JPanel searchPane = new JPanel(new BorderLayout(4, 4));
            searchPane.setOpaque(false);
            searchPane.add(new JLabel("Search:"), BorderLayout.WEST);
            searchPane.add(searchField, BorderLayout.CENTER);
            centerPanel.add(searchPane, BorderLayout.NORTH);
            
            // Scrollable list
            listPanel.setLayout(new javax.swing.BoxLayout(listPanel, javax.swing.BoxLayout.Y_AXIS));
            listPanel.setOpaque(false);
            
            // Populate valueBoxes
            java.util.Set<String> activeFilter = columnFilters.get(colIndex);
            
            selectAllBox.setOpaque(false);
            selectAllBox.addActionListener(e -> {
                boolean sel = selectAllBox.isSelected();
                for (javax.swing.JCheckBox cb : valueBoxes) {
                    if (cb.isVisible()) {
                        cb.setSelected(sel);
                    }
                }
            });
            listPanel.add(selectAllBox);
            
            for (String val : uniqueValues) {
                boolean isChecked = (activeFilter == null || activeFilter.contains(val));
                javax.swing.JCheckBox cb = new javax.swing.JCheckBox(val.isEmpty() ? "(Blanks)" : val, isChecked);
                cb.setOpaque(false);
                cb.putClientProperty("value", val);
                cb.addActionListener(e -> {
                    boolean allChecked = true;
                    for (javax.swing.JCheckBox b : valueBoxes) {
                        if (!b.isSelected()) {
                            allChecked = false;
                            break;
                        }
                    }
                    selectAllBox.setSelected(allChecked);
                });
                valueBoxes.add(cb);
                listPanel.add(cb);
            }
            
            scrollPane.setPreferredSize(new Dimension(220, 150));
            centerPanel.add(scrollPane, BorderLayout.CENTER);
            add(centerPanel, BorderLayout.CENTER);
            
            // Search field filtering
            searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void filterCheckboxes() {
                    String query = searchField.getText().trim().toLowerCase();
                    for (javax.swing.JCheckBox cb : valueBoxes) {
                        String text = cb.getText().toLowerCase();
                        cb.setVisible(text.contains(query));
                    }
                    listPanel.revalidate();
                    listPanel.repaint();
                }
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) { filterCheckboxes(); }
                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) { filterCheckboxes(); }
                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) { filterCheckboxes(); }
            });
            
            // OK / Cancel Buttons
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            btnPanel.setOpaque(false);
            
            ModernButton okBtn = new ModernButton("OK");
            okBtn.addActionListener(e -> {
                java.util.Set<String> selected = new java.util.HashSet<>();
                boolean allSelected = true;
                for (javax.swing.JCheckBox cb : valueBoxes) {
                    if (cb.isSelected()) {
                        selected.add((String) cb.getClientProperty("value"));
                    } else {
                        allSelected = false;
                    }
                }
                
                if (allSelected) {
                    columnFilters.remove(colIndex);
                } else {
                    columnFilters.put(colIndex, selected);
                }
                
                refreshSessionsTable();
                ColumnFilterPopup.this.setVisible(false);
            });
            
            ModernButton cancelBtn = new ModernButton("Cancel");
            cancelBtn.addActionListener(e -> ColumnFilterPopup.this.setVisible(false));
            
            btnPanel.add(okBtn);
            btnPanel.add(cancelBtn);
            add(btnPanel, BorderLayout.SOUTH);
            
            // Apply theme
            ThemeManager.applyTheme(this, ThemeManager.getColors(ThemeManager.loadTheme()));
        }
    }

    // --- System Tray and Notifications ---

    private void initSystemTray() {
        if (!SystemTray.isSupported()) return;
        SystemTray tray = SystemTray.getSystemTray();
        Image image = getIconImage();
        if (image == null) {
            try {
                java.io.InputStream imgStream = AppFrame.class.getResourceAsStream("/com/timelogger/icon.png");
                if (imgStream != null) {
                    image = javax.imageio.ImageIO.read(imgStream);
                }
            } catch (Exception ignored) {}
        }
        if (image != null) {
            trayIcon = new TrayIcon(image, "Time Logger");
            trayIcon.setImageAutoSize(true);
            
            // Double-click to restore
            trayIcon.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
                toFront();
            });

            // Build Context Menu
            java.awt.PopupMenu popup = new java.awt.PopupMenu();
            
            java.awt.MenuItem restoreItem = new java.awt.MenuItem("Show Time Logger");
            restoreItem.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
                toFront();
            });
            popup.add(restoreItem);
            
            java.awt.MenuItem pauseItem = new java.awt.MenuItem("Pause / Resume Stopwatch");
            pauseItem.addActionListener(e -> {
                if (stopwatchStarted) {
                    togglePauseStopwatch();
                }
            });
            popup.add(pauseItem);

            java.awt.MenuItem stopItem = new java.awt.MenuItem("Stop & Log Stopwatch");
            stopItem.addActionListener(e -> {
                if (stopwatchStarted) {
                    stopAndLogStopwatch();
                }
            });
            popup.add(stopItem);
            
            popup.addSeparator();
            
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
            exitItem.addActionListener(e -> {
                System.exit(0);
            });
            popup.add(exitItem);
            
            trayIcon.setPopupMenu(popup);
            
            try {
                tray.add(trayIcon);
                trayIconAdded = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendNotification(String title, String message, TrayIcon.MessageType type) {
        if (trayIcon != null && storageService.loadNotificationEnabled()) {
            trayIcon.displayMessage(title, message, type);
        }
    }

    private void notifySessionLogged(long durationSeconds) {
        if (durationSeconds <= 0 || !storageService.loadNotificationEnabled()) return;

        long mins = durationSeconds / 60;
        String durationStr = mins + " minutes";
        if (mins >= 60) {
            durationStr = (mins / 60) + " hr " + (mins % 60) + " mins";
        }

        String advice = "Take a 5-minute break to refresh your mind!";
        if (durationSeconds >= 3600) {
            advice = "Take a 15-minute break. Walk around and hydrate!";
        } else if (durationSeconds >= 1800) {
            advice = "Take a 10-minute break. Rest your eyes!";
        }

        sendNotification("Session Logged!", "Tracked " + durationStr + ". " + advice, TrayIcon.MessageType.INFO);
    }

    // --- Insights UI Panel ---

    private javax.swing.JComponent createInsightsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top Row: Summary Cards (Burnout Risk & Productivity Index)
        JPanel topRow = new JPanel(new GridLayout(1, 2, 12, 12));
        
        JPanel riskCard = new JPanel(new BorderLayout(8, 8));
        riskCard.setName("summaryPanel");
        riskCard.setBorder(BorderFactory.createTitledBorder("Burnout Risk Meter"));
        burnoutRiskLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        burnoutStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        riskCard.add(burnoutRiskLabel, BorderLayout.CENTER);
        riskCard.add(burnoutStatusLabel, BorderLayout.SOUTH);
        topRow.add(riskCard);

        JPanel prodCard = new JPanel(new BorderLayout(8, 8));
        prodCard.setName("summaryPanel");
        prodCard.setBorder(BorderFactory.createTitledBorder("Productivity Index"));
        productivityIndexLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        productivityProgress.setStringPainted(true);
        prodCard.add(productivityIndexLabel, BorderLayout.CENTER);
        prodCard.add(productivityProgress, BorderLayout.SOUTH);
        topRow.add(prodCard);

        mainPanel.add(topRow, BorderLayout.NORTH);

        // Middle Panel: Bento Split (Warnings & Details)
        JPanel middleRow = new JPanel(new GridLayout(1, 2, 12, 12));

        JPanel warningCard = new JPanel(new BorderLayout(8, 8));
        warningCard.setName("summaryPanel");
        warningCard.setBorder(BorderFactory.createTitledBorder("Warning Signs & Advice"));
        warningsContainer.setLayout(new javax.swing.BoxLayout(warningsContainer, javax.swing.BoxLayout.Y_AXIS));
        warningsContainer.setOpaque(false);
        warningCard.add(new JScrollPane(warningsContainer), BorderLayout.CENTER);
        middleRow.add(warningCard);

        JPanel detailsCard = new JPanel(new GridBagLayout());
        detailsCard.setName("summaryPanel");
        detailsCard.setBorder(BorderFactory.createTitledBorder("7-Day Analytics Details"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addDetailRow(detailsCard, "Total Hours Tracked:", insightDetailHours, gbc, 0);
        addDetailRow(detailsCard, "Consecutive Active Days:", insightDetailDays, gbc, 1);
        addDetailRow(detailsCard, "Late-Night Sessions:", insightDetailLateNights, gbc, 2);
        addDetailRow(detailsCard, "Average Break Ratio:", insightDetailBreakRatio, gbc, 3);
        addDetailRow(detailsCard, "Total Mid-Session Breaks:", insightDetailMidSessionBreaks, gbc, 4);
        addDetailRow(detailsCard, "Study Session Efficiency:", insightDetailEfficiency, gbc, 5);
        
        middleRow.add(detailsCard);

        mainPanel.add(middleRow, BorderLayout.CENTER);

        // Bottom Panel: Preferences
        JPanel prefsCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        prefsCard.setName("summaryPanel");
        prefsCard.setBorder(BorderFactory.createTitledBorder("Windows Notification Settings"));
        
        enableNotificationsCheckbox = new javax.swing.JCheckBox("Enable Windows Notifications");
        enableNotificationsCheckbox.setSelected(storageService.loadNotificationEnabled());
        enableNotificationsCheckbox.setOpaque(false);
        enableNotificationsCheckbox.addActionListener(e -> storageService.saveNotificationEnabled(enableNotificationsCheckbox.isSelected()));

        enableBreakRemindersCheckbox = new javax.swing.JCheckBox("Enable 50-Min Break Reminders");
        enableBreakRemindersCheckbox.setSelected(storageService.loadBreakRemindersEnabled());
        enableBreakRemindersCheckbox.setOpaque(false);
        enableBreakRemindersCheckbox.addActionListener(e -> storageService.saveBreakRemindersEnabled(enableBreakRemindersCheckbox.isSelected()));

        prefsCard.add(enableNotificationsCheckbox);
        prefsCard.add(enableBreakRemindersCheckbox);

        mainPanel.add(prefsCard, BorderLayout.SOUTH);

        // Wrap in a viewport scrollpane
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private void addDetailRow(JPanel panel, String labelText, JLabel valueLabel, GridBagConstraints gbc, int row) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.6;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        panel.add(valueLabel, gbc);
    }

    private void refreshInsights() {
        List<SessionRecord> allSessions = storageService.loadSessions();
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);

        // Filter sessions in the last 7 days
        List<SessionRecord> recentSessions = allSessions.stream()
            .filter(s -> !s.getStartTime().toLocalDate().isBefore(sevenDaysAgo) && !s.getStartTime().toLocalDate().isAfter(today))
            .collect(Collectors.toList());

        // 1. Total Hours Tracked
        double totalHours = recentSessions.stream()
            .mapToLong(SessionRecord::getDurationSeconds)
            .sum() / 3600.0;

        // 2. Active Days Count
        java.util.Set<LocalDate> activeDates = recentSessions.stream()
            .map(s -> s.getStartTime().toLocalDate())
            .collect(Collectors.toSet());
        int activeDaysCount = activeDates.size();

        // 3. Consecutive Active Days
        int consecutiveActiveDays = 0;
        LocalDate checkDate = today;
        // If today has no sessions, check starting from yesterday
        if (!activeDates.contains(today)) {
            checkDate = today.minusDays(1);
        }
        while (activeDates.contains(checkDate)) {
            consecutiveActiveDays++;
            checkDate = checkDate.minusDays(1);
        }

        // 4. Late-Night Sessions
        int lateNightSessions = 0;
        for (SessionRecord s : recentSessions) {
            int startHour = s.getStartTime().getHour();
            int endHour = s.getEndTime().getHour();
            if (startHour >= 23 || startHour < 5 || endHour >= 23 || endHour < 5) {
                lateNightSessions++;
            }
        }

        // 5. Average Break Ratio
        // We group sessions by date. For each active date, we sort by start time.
        // Active time = sum of session durations.
        // Break time = sum of idle gaps between sessions.
        double totalBreakRatioSum = 0;
        int activeDaysWithSessions = 0;
        
        java.util.Map<LocalDate, List<SessionRecord>> sessionsByDate = recentSessions.stream()
            .collect(Collectors.groupingBy(s -> s.getStartTime().toLocalDate()));

        for (java.util.Map.Entry<LocalDate, List<SessionRecord>> entry : sessionsByDate.entrySet()) {
            List<SessionRecord> dailySessions = new ArrayList<>(entry.getValue());
            dailySessions.sort(java.util.Comparator.comparing(SessionRecord::getStartTime));
            
            long activeSeconds = dailySessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
            long breakSeconds = 0;
            for (int i = 0; i < dailySessions.size() - 1; i++) {
                LocalDateTime endPrev = dailySessions.get(i).getEndTime();
                LocalDateTime startNext = dailySessions.get(i+1).getStartTime();
                long gap = java.time.Duration.between(endPrev, startNext).toSeconds();
                if (gap > 0) {
                    breakSeconds += gap;
                }
            }
            if (activeSeconds > 0) {
                totalBreakRatioSum += (double) breakSeconds / activeSeconds;
                activeDaysWithSessions++;
            }
        }

        double avgBreakRatio = activeDaysWithSessions > 0 ? (totalBreakRatioSum / activeDaysWithSessions) : 1.0;

        // Calculate Burnout Risk Score
        double burnoutScore = 0;
        burnoutScore += totalHours * 1.5; // 66 hours = 100%
        if (activeDaysCount > 0 && avgBreakRatio < 0.15) {
            burnoutScore += 20.0;
        }
        int riskPercent = (int) Math.max(0, Math.min(100, burnoutScore));

        // Update Burnout UI
        burnoutRiskLabel.setText(riskPercent + "%");
        ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
        if (riskPercent >= 70) {
            burnoutRiskLabel.setForeground(java.awt.Color.RED);
            burnoutStatusLabel.setText("High Burnout Risk! Rest is required.");
            burnoutStatusLabel.setForeground(java.awt.Color.RED);
        } else if (riskPercent >= 30) {
            burnoutRiskLabel.setForeground(java.awt.Color.ORANGE);
            burnoutStatusLabel.setText("Moderate Burnout Risk. Schedule breaks.");
            burnoutStatusLabel.setForeground(java.awt.Color.ORANGE);
        } else {
            burnoutRiskLabel.setForeground(colors.accent);
            burnoutStatusLabel.setText("Low Burnout Risk. Doing great!");
            burnoutStatusLabel.setForeground(colors.text);
        }

        // Calculate Productivity Index
        double dailyGoalHrs = storageService.loadDailyGoalMinutes() / 60.0;
        double targetHours = dailyGoalHrs * 7.0;
        double goalAchievementRate = targetHours > 0 ? Math.min(1.0, totalHours / targetHours) : 1.0;
        double pacingFactor = 1.0 - (riskPercent / 200.0);
        int prodIndex = (int) (goalAchievementRate * pacingFactor * 100.0);

        // Update Productivity UI
        productivityIndexLabel.setText(prodIndex + "/100");
        productivityProgress.setValue(prodIndex);

        // Calculate mid-session breaks & efficiency for the last 7 days
        long totalSpanSeconds = 0;
        long totalActiveSeconds = 0;
        for (SessionRecord s : recentSessions) {
            long span = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
            totalSpanSeconds += span;
            totalActiveSeconds += s.getDurationSeconds();
        }
        long totalMidSessionBreaks = Math.max(0, totalSpanSeconds - totalActiveSeconds);
        double efficiencyVal = totalSpanSeconds > 0 ? ((double) totalActiveSeconds / totalSpanSeconds) * 100.0 : 100.0;

        // Update Metrics Labels
        insightDetailHours.setText(String.format("%.1f hrs", totalHours));
        insightDetailDays.setText(consecutiveActiveDays + " days");
        insightDetailLateNights.setText(lateNightSessions + " sessions");
        insightDetailBreakRatio.setText(String.format("%.1f%%", avgBreakRatio * 100));
        insightDetailMidSessionBreaks.setText(formatDuration(totalMidSessionBreaks));
        insightDetailEfficiency.setText(String.format("%.1f%%", efficiencyVal));

        // Update Warnings & Recommendations
        warningsContainer.removeAll();
        boolean hasWarnings = false;

        if (totalHours > 56.0) {
            addWarningLabel("⚠️ Extreme Load: Tracked over 56 hours. High fatigue risk.");
            hasWarnings = true;
        }
        if (activeDaysCount > 0 && avgBreakRatio < 0.15) {
            addWarningLabel("⚠️ Insufficient Breaks: Break ratio is under 15%. Rest more.");
            hasWarnings = true;
        }
        if (!hasWarnings) {
            JLabel okayLabel = new JLabel("✓ Your tracking habits look balanced and healthy!");
            okayLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            okayLabel.setForeground(colors.accent);
            warningsContainer.add(okayLabel);
        }

        warningsContainer.revalidate();
        warningsContainer.repaint();
    }

    private void addWarningLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(java.awt.Color.RED);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        warningsContainer.add(lbl);
    }

    private void checkGoalAchievement(long activeSeconds) {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastGoalCheckDate)) {
            goalNotificationSentToday = false;
            lastGoalCheckDate = today;
        }

        if (goalNotificationSentToday) return;

        GoalStreakStats stats = calculateGoalStreakStats();
        if (stats.dailyGoalMinutes <= 0) return;

        int totalMinutesToday = stats.todayMinutes + (int)(activeSeconds / 60);
        if (totalMinutesToday >= stats.dailyGoalMinutes) {
            sendNotification("Goal Achieved! 🎉", 
                "You have met your daily goal of " + stats.dailyGoalMinutes + " minutes today!", 
                TrayIcon.MessageType.INFO);
            goalNotificationSentToday = true;
        }
    }

    private static class WeeklyBarChartPanel extends JPanel {
        private java.util.Map<java.time.DayOfWeek, Long> data = new java.util.HashMap<>();

        public WeeklyBarChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(200, 140));
        }

        public void setData(java.util.Map<java.time.DayOfWeek, Long> data) {
            this.data = data != null ? data : new java.util.HashMap<>();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
            int w = getWidth();
            int h = getHeight();

            int paddingLeft = 16;
            int paddingRight = 16;
            int paddingTop = 20;
            int paddingBottom = 24;

            int chartW = w - paddingLeft - paddingRight;
            int chartH = h - paddingTop - paddingBottom;

            long maxVal = 0;
            for (long val : data.values()) {
                if (val > maxVal) maxVal = val;
            }
            if (maxVal == 0) maxVal = 3600;

            DayOfWeek[] days = DayOfWeek.values();
            int barGap = 6;
            int numBars = days.length;
            int barW = (chartW - (barGap * (numBars - 1))) / numBars;

            g2.setColor(colors.border);
            g2.drawLine(paddingLeft, h - paddingBottom, w - paddingRight, h - paddingBottom);

            String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

            for (int i = 0; i < numBars; i++) {
                DayOfWeek day = days[i];
                long seconds = data.getOrDefault(day, 0L);
                double ratio = (double) seconds / maxVal;
                int barH = (int) (ratio * chartH);

                int x = paddingLeft + i * (barW + barGap);
                int y = h - paddingBottom - barH;

                if (seconds > 0) {
                    GradientPaint gp = new GradientPaint(x, y, colors.accent, x, y + barH, new Color(colors.accent.getRed(), colors.accent.getGreen(), colors.accent.getBlue(), 50));
                    g2.setPaint(gp);
                    g2.fillRoundRect(x, y, barW, barH, 4, 4);
                }

                g2.setColor(colors.text);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                FontMetrics fm = g2.getFontMetrics();
                String lbl = dayLabels[i];
                int lblX = x + (barW - fm.stringWidth(lbl)) / 2;
                int lblY = h - paddingBottom + 14;
                g2.drawString(lbl, lblX, lblY);

                if (seconds > 0) {
                    double hrs = seconds / 3600.0;
                    String hrsStr = String.format("%.1fh", hrs);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                    FontMetrics fmHrs = g2.getFontMetrics();
                    int hrsX = x + (barW - fmHrs.stringWidth(hrsStr)) / 2;
                    int hrsY = y - 4;
                    g2.drawString(hrsStr, hrsX, hrsY);
                }
            }
            g2.dispose();
        }
    }

    private static class DailyTimelinePanel extends JPanel {
        private List<SessionRecord> todaySessions = new ArrayList<>();

        public DailyTimelinePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(600, 70));
            setToolTipText("");
        }

        public void setData(List<SessionRecord> allSessions) {
            LocalDate today = LocalDate.now();
            this.todaySessions = allSessions.stream()
                .filter(s -> s.getStartTime().toLocalDate().equals(today))
                .sorted(java.util.Comparator.comparing(SessionRecord::getStartTime))
                .collect(Collectors.toList());
            repaint();
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            if (todaySessions.isEmpty()) return "No sessions tracked today.";

            int w = getWidth();
            int paddingLeft = 20;
            int paddingRight = 20;
            int chartW = w - paddingLeft - paddingRight;

            LocalDateTime firstStart = todaySessions.get(0).getStartTime();
            LocalDateTime lastEnd = todaySessions.get(todaySessions.size() - 1).getEndTime();
            
            LocalDateTime timelineStart = firstStart.minusMinutes(30);
            LocalDateTime timelineEnd = lastEnd.plusMinutes(30);

            long totalTimelineSec = java.time.Duration.between(timelineStart, timelineEnd).toSeconds();
            if (totalTimelineSec <= 0) return null;

            int mouseX = e.getX();
            if (mouseX < paddingLeft || mouseX > w - paddingRight) return null;

            double ratio = (double) (mouseX - paddingLeft) / chartW;
            long offsetSeconds = (long) (ratio * totalTimelineSec);
            LocalDateTime hoverTime = timelineStart.plusSeconds(offsetSeconds);

            for (SessionRecord s : todaySessions) {
                if (!hoverTime.isBefore(s.getStartTime()) && !hoverTime.isAfter(s.getEndTime())) {
                    long activeSec = s.getDurationSeconds();
                    long spanSec = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
                    long breakSec = Math.max(0, spanSec - activeSec);
                    double efficiency = spanSec > 0 ? ((double) activeSec / spanSec) * 100.0 : 100.0;

                    String activeStr = String.format("%d min", activeSec / 60);
                    if (activeSec >= 3600) {
                        activeStr = (activeSec / 3600) + "h " + ((activeSec % 3600) / 60) + "m";
                    }

                    String breakStr = "";
                    if (breakSec > 0) {
                        breakStr = String.format("<br>Mid-Session Breaks: %d min (Efficiency: %.1f%%)", breakSec / 60, efficiency);
                    }

                    return "<html><b>Subject:</b> " + s.getSubject() +
                           "<br><b>Activity:</b> " + s.getDescription() +
                           "<br><b>Time:</b> " + s.getStartTime().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + 
                           " - " + s.getEndTime().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) +
                           "<br><b>Active Duration:</b> " + activeStr +
                           breakStr + "</html>";
                }
            }

            return "Idle/Break between sessions";
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
            int w = getWidth();
            int h = getHeight();

            int paddingLeft = 20;
            int paddingRight = 20;
            int paddingTop = 10;
            int paddingBottom = 30;

            int chartW = w - paddingLeft - paddingRight;
            int barY = paddingTop;
            int barH = h - paddingTop - paddingBottom;

            g2.setColor(colors.border);
            g2.fillRoundRect(paddingLeft, barY, chartW, barH, 6, 6);

            if (todaySessions.isEmpty()) {
                g2.setColor(colors.text);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                FontMetrics fm = g2.getFontMetrics();
                String msg = "No study sessions logged today yet.";
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, barY + barH / 2 + 5);
                g2.dispose();
                return;
            }

            LocalDateTime firstStart = todaySessions.get(0).getStartTime();
            LocalDateTime lastEnd = todaySessions.get(todaySessions.size() - 1).getEndTime();
            
            LocalDateTime timelineStart = firstStart.minusMinutes(30);
            LocalDateTime timelineEnd = lastEnd.plusMinutes(30);

            long totalTimelineSec = java.time.Duration.between(timelineStart, timelineEnd).toSeconds();
            if (totalTimelineSec <= 0) totalTimelineSec = 1;

            for (SessionRecord s : todaySessions) {
                long offsetStartSec = java.time.Duration.between(timelineStart, s.getStartTime()).toSeconds();
                long sessionSpanSec = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();

                int x = paddingLeft + (int) (((double) offsetStartSec / totalTimelineSec) * chartW);
                int sessionW = (int) (((double) sessionSpanSec / totalTimelineSec) * chartW);
                if (sessionW < 4) sessionW = 4;

                g2.setColor(colors.accent);
                g2.fillRoundRect(x, barY, sessionW, barH, 4, 4);

                long activeSec = s.getDurationSeconds();
                long midSessionBreakSec = Math.max(0, sessionSpanSec - activeSec);
                if (midSessionBreakSec > 0 && sessionW > 10) {
                    double ratio = (double) midSessionBreakSec / sessionSpanSec;
                    int breakW = (int) (sessionW * ratio);
                    int breakX = x + sessionW - breakW;
                    g2.setColor(new Color(128, 128, 128, 180));
                    g2.fillRoundRect(breakX, barY + 2, breakW, barH - 4, 3, 3);
                }
            }

            g2.setColor(colors.text);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();

            long hoursSpan = totalTimelineSec / 3600;
            int tickIntervalHours = hoursSpan > 12 ? 3 : (hoursSpan > 6 ? 2 : 1);

            LocalDateTime tickTime = timelineStart.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);
            while (tickTime.isBefore(timelineEnd)) {
                long offsetSec = java.time.Duration.between(timelineStart, tickTime).toSeconds();
                int tickX = paddingLeft + (int) (((double) offsetSec / totalTimelineSec) * chartW);

                if (tickX >= paddingLeft && tickX <= w - paddingRight) {
                    g2.setColor(colors.border);
                    g2.drawLine(tickX, barY + barH, tickX, barY + barH + 4);

                    g2.setColor(colors.text);
                    String timeLabel = tickTime.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    int lblX = tickX - fm.stringWidth(timeLabel) / 2;
                    int lblY = barY + barH + 16;
                    g2.drawString(timeLabel, lblX, lblY);
                }
                tickTime = tickTime.plusHours(tickIntervalHours);
            }
            g2.dispose();
        }
    }
}

