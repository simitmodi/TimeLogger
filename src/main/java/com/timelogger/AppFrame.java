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
import javax.swing.JTextPane;
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
    private int currentSessionPauseCount = 0;
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
    private final JLabel insightDetailAttentionSpan = new JLabel("-");
    private final JLabel insightDetailFocusScore = new JLabel("-");
    private final JLabel insightDetailTotalXp = new JLabel("-");
    private final JLabel insightDetailAvgXp = new JLabel("-");
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
    final JComboBox<String> stopwatchActivityTypeCombo = new JComboBox<>(new String[]{"General", "Questions", "Lecture", "Revision"});
    final JPanel stopwatchActivitySubPanel = new JPanel(new java.awt.CardLayout()) {
        @Override
        public Dimension getPreferredSize() {
            for (java.awt.Component child : getComponents()) {
                if (child.isVisible()) {
                    return child.getPreferredSize();
                }
            }
            return super.getPreferredSize();
        }
    };
    final JTextField stopwatchActivityField = new JTextField(20);
    final JComboBox<String> stopwatchQuestionTypeCombo = new JComboBox<>(new String[]{
        "DPP Questions", "Practice Book Questions", "Previous Year Questions"
    });
    final JComboBox<String> stopwatchQuestionDescCombo = new JComboBox<>();
    private JButton stopwatchCalcBtn;
    final JTextField stopwatchChapterField = new JTextField(5);
    final JTextField stopwatchLectureField = new JTextField(5);
    final JTextField stopwatchRevisionTopicField = new JTextField(20);
    private final ModernButton stopwatchStartButton = new ModernButton("Start");
    private final ModernButton stopwatchPauseResumeButton = new ModernButton("Pause");
    private final ModernButton stopwatchStopButton = new ModernButton("Stop & Log");
    private final ModernButton stopwatchResetButton = new ModernButton("Reset");

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

    private boolean timerRunning = false;
    private boolean timerStarted = false;
    private long timerTotalSeconds = 0;
    private long timerRemainingSeconds = 0;
    private LocalDateTime timerSessionStart;
    private final Timer timerTick;

    // MiniWindow reference removed (mini mode discarded)
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
    private JComboBox<String> sessionsFilterCombo;
    final JLabel totalDurationValueLabel = new JLabel("-");
    final JLabel xpValueLabel = new JLabel("-");
    final JLabel xpBreakdownLabel = new JLabel("-");
    final JLabel xpAnalyticsLabel = new JLabel("-");
    final JLabel xpRateLabel = new JLabel("-");
    final DefaultTableModel dailyXpAnalysisModel = new DefaultTableModel(
        new Object[]{"Date", "Study Duration", "Break Duration", "XP Gained", "XP Deducted", "Net XP"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
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
    final DefaultTableModel activityAnalysisModel = new DefaultTableModel(new Object[]{"Activity", "Duration", "Questions Solved", "Avg Time/Q"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    final DefaultTableModel revisionAnalysisModel = new DefaultTableModel(new Object[]{"Topic", "Duration"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    final DefaultTableModel questionsByTopicAnalysisModel = new DefaultTableModel(new Object[]{"Topic", "Duration", "Questions Solved", "Avg Time/Q"}, 0) {
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
    private JComboBox<String> questionsTopicFilterCombo;
    private ScientificCalculator scientificCalculator;
    private final JLabel avgSessionLabel = new JLabel("Avg Duration: -", SwingConstants.CENTER);
    private final JLabel activeDayAvgLabel = new JLabel("Active Day Avg: -", SwingConstants.CENTER);
    private final JLabel mostActiveSubjectLabel = new JLabel("Most Active: -", SwingConstants.CENTER);
    private final JLabel studyEfficiencyLabel = new JLabel("Study Efficiency: -", SwingConstants.CENTER);
    private final JLabel midSessionBreakLabel = new JLabel("Mid-Session Breaks: -", SwingConstants.CENTER);

    // AI Assistant components
    private final JPanel aiCardPanel = new JPanel(new java.awt.CardLayout());
    private final JTextPane aiChatLogPane = new JTextPane();
    private final JTextField aiInputTextArea = new JTextField();
    private final ModernButton aiSendBtn = new ModernButton("Send");
    private final JLabel aiStatusLabel = new JLabel(" ", SwingConstants.LEFT);
    private final javax.swing.JPasswordField openRouterKeyField = new javax.swing.JPasswordField(30);
    private final StringBuilder aiChatHtmlBuilder = new StringBuilder();
    
    private static class ChatTurn {
        String role;
        String text;
        ChatTurn(String role, String text) {
            this.role = role;
            this.text = text;
        }
    }
    private final List<ChatTurn> aiChatHistoryList = new ArrayList<>();

    private final JLabel goalProgressLabel = new JLabel("Today: 0 / 0 min (0%)", SwingConstants.CENTER);
    private final javax.swing.JProgressBar goalProgressBar = new javax.swing.JProgressBar(0, 100);
    private final JLabel streakLabel = new JLabel("Streak: 0 days 🔥 (Max: 0 🏆)", SwingConstants.CENTER);
    private javax.swing.border.TitledBorder timelineBorder;
    private JPanel stopwatchConfigPanel;
    private JPanel timerConfigPanel;

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
        refreshQuestionTopicsDropdown();
        String initialQType = (String) stopwatchQuestionTypeCombo.getSelectedItem();
        if (initialQType != null) {
            String subject = (String) stopwatchSubjectCombo.getSelectedItem();
            if (subject == null) subject = "";
            String lastDesc = storageService.loadLastQuestionDesc(initialQType, subject);
            selectOrAddQuestionDesc(lastDesc);
        }
        refreshTimerSubjects();
        refreshLogsSubjects();
        refreshSessionsTable();
        refreshExportAvailability();
        updateStopwatchButtons();
        updateTimerButtons();

        // Apply saved theme on startup
        updateTheme(ThemeManager.loadTheme());

        initSystemTray();

        // Register shutdown hook to cleanly remove tray icon on VM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupSystemTray));

        this.tabs.addChangeListener(e -> {
            int selectedIdx = tabs.getSelectedIndex();
            if (selectedIdx == tabs.indexOfTab("Insights")) {
                refreshInsights();
            } else if (selectedIdx == tabs.indexOfTab("AI Assistant")) {
                showChatInterface();
            }
            
            if (selectedIdx != tabs.indexOfTab("Stopwatch")) {
                if (scientificCalculator != null && scientificCalculator.isVisible()) {
                    scientificCalculator.setVisible(false);
                }
            } else {
                updateCalculatorVisibility();
            }
        });

        // Minimize memory footprint when minimized (normal taskbar behavior, no hide)
        this.addWindowStateListener(e -> {
            if ((e.getNewState() & JFrame.ICONIFIED) == JFrame.ICONIFIED) {
                System.gc();
            }
        });

        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateTimeLabelsFontSize();
            }
        });

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveChatHistory();
                cleanupSystemTray();
                if (scientificCalculator != null) {
                    scientificCalculator.dispose();
                }
            }
            @Override
            public void windowIconified(java.awt.event.WindowEvent e) {
                if (scientificCalculator != null && scientificCalculator.isVisible()) {
                    scientificCalculator.setVisible(false);
                }
            }
            @Override
            public void windowDeiconified(java.awt.event.WindowEvent e) {
                updateCalculatorVisibility();
            }
        });
        
        updateCalculatorVisibility();
    }

    private void applyFontToContainer(java.awt.Container container, Font labelFont, Font controlFont) {
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof JLabel) {
                if (child != stopwatchSubjectLabel && child != stopwatchTimeLabel && child != timerTimeLabel) {
                    child.setFont(labelFont);
                }
            } else if (child instanceof JComboBox || child instanceof JTextField || child instanceof JSpinner) {
                child.setFont(controlFont);
                int width = 150;
                if (child == stopwatchActivityField || child == stopwatchRevisionTopicField) {
                    width = Math.max(150, getWidth() / 4);
                } else if (child == stopwatchQuestionTypeCombo || child == stopwatchQuestionDescCombo) {
                    width = Math.max(200, child.getPreferredSize().width + 40);
                } else if (child == stopwatchChapterField || child == stopwatchLectureField) {
                    width = Math.max(40, getWidth() / 20);
                } else if (child == stopwatchSubjectCombo || child == stopwatchActivityTypeCombo || child == timerSubjectCombo) {
                    width = child.getPreferredSize().width + 40;
                }
                
                Dimension d = child.getPreferredSize();
                child.setPreferredSize(new Dimension(width, d.height));
            } else if (child instanceof java.awt.Container) {
                applyFontToContainer((java.awt.Container) child, labelFont, controlFont);
            }
        }
    }

    private void updateTimeLabelsFontSize() {
        int w = getWidth();
        int h = getHeight();
        int size = Math.max(64, Math.min(w / 10, h / 5));
        Font f = new Font("Monospaced", Font.BOLD, size);
        stopwatchTimeLabel.setFont(f);
        timerTimeLabel.setFont(f);

        int subjectLabelSize = Math.max(18, Math.min(w / 50, h / 30));
        stopwatchSubjectLabel.setFont(new Font("SansSerif", Font.BOLD, subjectLabelSize));

        // Scale config font size
        int configSize = Math.max(12, Math.min(w / 70, h / 45));
        Font labelFont = new Font("SansSerif", Font.BOLD, configSize);
        Font controlFont = new Font("SansSerif", Font.PLAIN, configSize);

        if (stopwatchConfigPanel != null) {
            applyFontToContainer(stopwatchConfigPanel, labelFont, controlFont);
            stopwatchConfigPanel.revalidate();
            stopwatchConfigPanel.repaint();
        }
        if (timerConfigPanel != null) {
            applyFontToContainer(timerConfigPanel, labelFont, controlFont);
            timerConfigPanel.revalidate();
            timerConfigPanel.repaint();
        }
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
        tabs.addTab("AI Assistant", createAiAssistantPanel());
        return tabs;
    }

    private JPanel createStopwatchPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        stopwatchTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 64));
        stopwatchSubjectLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));

        stopwatchActivityField.setPreferredSize(new Dimension(300, 28));
        stopwatchQuestionDescCombo.setPreferredSize(new Dimension(300, 28));
        stopwatchQuestionTypeCombo.setPreferredSize(new Dimension(200, 28));
        stopwatchChapterField.setPreferredSize(new Dimension(50, 28));
        stopwatchLectureField.setPreferredSize(new Dimension(50, 28));
        stopwatchRevisionTopicField.setPreferredSize(new Dimension(300, 28));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        stopwatchConfigPanel = new JPanel(new GridBagLayout());
        JPanel config = stopwatchConfigPanel;
        GridBagConstraints gbcConfig = new GridBagConstraints();
        gbcConfig.gridx = 0;
        gbcConfig.gridy = 0;
        gbcConfig.insets = new Insets(4, 12, 4, 12);
        gbcConfig.fill = GridBagConstraints.NONE;
        gbcConfig.anchor = GridBagConstraints.CENTER;

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row0.setOpaque(false);
        row0.add(new JLabel("Subject"));
        row0.add(stopwatchSubjectCombo);
        row0.add(new JLabel("Activity Type"));
        row0.add(stopwatchActivityTypeCombo);
        config.add(row0, gbcConfig);

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
        questionsCard.add(stopwatchQuestionDescCombo);
        
        stopwatchCalcBtn = new JButton("🧮 Calculator");
        stopwatchCalcBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        stopwatchCalcBtn.addActionListener(e -> {
            if (scientificCalculator == null) {
                scientificCalculator = new ScientificCalculator(this);
            }
            scientificCalculator.setVisible(true);
        });
        questionsCard.add(stopwatchCalcBtn);

        JPanel lectureCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lectureCard.add(new JLabel("Ch No: "));
        lectureCard.add(stopwatchChapterField);
        lectureCard.add(new JLabel("Lec No: "));
        lectureCard.add(stopwatchLectureField);

        JPanel revisionCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        revisionCard.add(new JLabel("Topic: "));
        revisionCard.add(stopwatchRevisionTopicField);

        stopwatchActivitySubPanel.add(generalCard, "General");
        stopwatchActivitySubPanel.add(questionsCard, "Questions");
        stopwatchActivitySubPanel.add(lectureCard, "Lecture");
        stopwatchActivitySubPanel.add(revisionCard, "Revision");

        java.awt.CardLayout cardLayout = (java.awt.CardLayout) stopwatchActivitySubPanel.getLayout();
        stopwatchActivityTypeCombo.addActionListener(e -> {
            String selected = (String) stopwatchActivityTypeCombo.getSelectedItem();
            cardLayout.show(stopwatchActivitySubPanel, selected);
            stopwatchActivitySubPanel.revalidate();
            stopwatchActivitySubPanel.repaint();
            config.revalidate();
            config.repaint();
            updateCalculatorVisibility();
        });

        final String[] lastSelectedDesc = {null};
        stopwatchQuestionDescCombo.addActionListener(e -> {
            String selected = (String) stopwatchQuestionDescCombo.getSelectedItem();
            if ("[Add Custom...]".equals(selected)) {
                String input = JOptionPane.showInputDialog(this, "Enter custom topic name:", "New Topic", JOptionPane.PLAIN_MESSAGE);
                if (input != null) {
                    input = input.trim();
                    if (!input.isEmpty() && !"[Add Custom...]".equals(input)) {
                        boolean exists = false;
                        for (int i = 0; i < stopwatchQuestionDescCombo.getItemCount(); i++) {
                            if (input.equalsIgnoreCase(stopwatchQuestionDescCombo.getItemAt(i))) {
                                stopwatchQuestionDescCombo.setSelectedIndex(i);
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            int count = stopwatchQuestionDescCombo.getItemCount();
                            stopwatchQuestionDescCombo.insertItemAt(input, count - 1);
                            stopwatchQuestionDescCombo.setSelectedItem(input);
                            saveCurrentQuestionTopics();
                        }
                    } else {
                        revertQuestionDescSelection(lastSelectedDesc[0]);
                    }
                } else {
                    revertQuestionDescSelection(lastSelectedDesc[0]);
                }
            } else {
                lastSelectedDesc[0] = selected;
            }
        });

        stopwatchQuestionTypeCombo.addActionListener(e -> {
            String qType = (String) stopwatchQuestionTypeCombo.getSelectedItem();
            if (qType != null) {
                refreshQuestionTopicsDropdown();
                String subject = (String) stopwatchSubjectCombo.getSelectedItem();
                if (subject == null) subject = "";
                String lastDesc = storageService.loadLastQuestionDesc(qType, subject);
                selectOrAddQuestionDesc(lastDesc);
                updateCalculatorVisibility();
            }
        });

        stopwatchSubjectCombo.addActionListener(e -> {
            String qType = (String) stopwatchQuestionTypeCombo.getSelectedItem();
            if (qType != null) {
                refreshQuestionTopicsDropdown();
                String subject = (String) stopwatchSubjectCombo.getSelectedItem();
                if (subject == null) subject = "";
                String lastDesc = storageService.loadLastQuestionDesc(qType, subject);
                selectOrAddQuestionDesc(lastDesc);
            }
        });

        GridBagConstraints gbcSub = new GridBagConstraints();
        gbcSub.gridx = 0;
        gbcSub.gridy = 1;
        gbcSub.insets = new Insets(4, 12, 4, 12);
        gbcSub.fill = GridBagConstraints.NONE;
        gbcSub.anchor = GridBagConstraints.CENTER;
        config.add(stopwatchActivitySubPanel, gbcSub);

        centerPanel.add(stopwatchSubjectLabel, BorderLayout.NORTH);
        centerPanel.add(stopwatchTimeLabel, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        stopwatchStartButton.addActionListener(e -> startStopwatch());
        stopwatchPauseResumeButton.addActionListener(e -> togglePauseStopwatch());
        stopwatchStopButton.addActionListener(e -> stopAndLogStopwatch());
        stopwatchResetButton.addActionListener(e -> resetStopwatch());

        stopwatchStartButton.setMnemonic('S');
        stopwatchPauseResumeButton.setMnemonic('P');
        stopwatchStopButton.setMnemonic('L');
        stopwatchResetButton.setMnemonic('R');

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

        timerConfigPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        JPanel config = timerConfigPanel;
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

        timerStartButton.setMnemonic('S');
        timerPauseResumeButton.setMnemonic('P');
        timerStopButton.setMnemonic('T');
        timerResetButton.setMnemonic('R');

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
                            int solvedIdx = qDesc.indexOf(" (Solved:");
                            if (solvedIdx != -1) {
                                qDesc = qDesc.substring(0, solvedIdx).trim();
                            }
                        } else {
                            qType = content.trim();
                        }
                        stopwatchQuestionTypeCombo.setSelectedItem(qType);
                        selectOrAddQuestionDesc(qDesc);
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
                    } else if (desc.startsWith("Revision: ")) {
                        stopwatchActivityTypeCombo.setSelectedItem("Revision");
                        String topic = desc.substring("Revision: ".length()).trim();
                        stopwatchRevisionTopicField.setText(topic);
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
        analysisPeriodCombo.setSelectedItem("Today");
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
        
        JPanel sessionsCard = new JPanel(new BorderLayout(4, 4));
        sessionsCard.setBorder(BorderFactory.createTitledBorder("Total Sessions"));
        
        sessionsFilterCombo = new JComboBox<>(new String[]{"All", "General", "Revision", "Questions", "Lecture"});
        sessionsFilterCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sessionsFilterCombo.addActionListener(e -> refreshAnalysis());
        
        JPanel filterComboPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        filterComboPanel.setOpaque(false);
        filterComboPanel.add(sessionsFilterCombo);
        sessionsCard.add(filterComboPanel, BorderLayout.NORTH);
        
        JPanel sessionsStatsPanel = new JPanel(new java.awt.GridLayout(3, 1, 2, 2));
        sessionsStatsPanel.setOpaque(false);
        
        totalSessionsValueLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        totalSessionsValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sessionsStatsPanel.add(totalSessionsValueLabel);
        
        avgSessionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        avgSessionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sessionsStatsPanel.add(avgSessionLabel);
        
        studyEfficiencyLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        studyEfficiencyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sessionsStatsPanel.add(studyEfficiencyLabel);
        
        sessionsCard.add(sessionsStatsPanel, BorderLayout.CENTER);
        
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

        JTable revisionTable = new JTable(revisionAnalysisModel);
        revisionTable.setRowHeight(24);
        JPanel revisionCard = new JPanel(new BorderLayout(8, 8));
        revisionCard.setBorder(BorderFactory.createTitledBorder("Revision by Topic"));
        JScrollPane revisionScroll = new JScrollPane(revisionTable);
        revisionScroll.setPreferredSize(new Dimension(200, 140));
        revisionCard.add(revisionScroll, BorderLayout.CENTER);

        JTable questionsTopicTable = new JTable(questionsByTopicAnalysisModel);
        questionsTopicTable.setRowHeight(24);
        JPanel questionsTopicCard = new JPanel(new BorderLayout(8, 8));
        questionsTopicCard.setBorder(BorderFactory.createTitledBorder("Questions Solved by Topic"));
        
        JPanel topicFilterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        topicFilterPanel.setOpaque(false);
        JLabel filterLbl = new JLabel("Filter:");
        filterLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        questionsTopicFilterCombo = new JComboBox<>(new String[]{
            "All (Practice & PYQ)",
            "Practice Book",
            "Previous Year"
        });
        questionsTopicFilterCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        questionsTopicFilterCombo.addActionListener(e -> refreshAnalysis());
        
        topicFilterPanel.add(filterLbl);
        topicFilterPanel.add(questionsTopicFilterCombo);
        questionsTopicCard.add(topicFilterPanel, BorderLayout.NORTH);
        
        JScrollPane questionsTopicScroll = new JScrollPane(questionsTopicTable);
        questionsTopicScroll.setPreferredSize(new Dimension(200, 140));
        questionsTopicCard.add(questionsTopicScroll, BorderLayout.CENTER);

        JPanel dayOfWeekCard = new JPanel(new BorderLayout(8, 8));
        dayOfWeekCard.setBorder(BorderFactory.createTitledBorder("By Day of Week (Last 7 Days)"));
        dayOfWeekCard.add(weeklyBarChartPanel, BorderLayout.CENTER);

        JPanel timelineCard = new JPanel(new BorderLayout(8, 8));
        timelineBorder = BorderFactory.createTitledBorder("Today's Session Timeline");
        timelineCard.setBorder(timelineBorder);
        timelineCard.add(dailyTimelinePanel, BorderLayout.CENTER);

        JPanel heatmapCard = new JPanel(new BorderLayout(8, 8));
        heatmapCard.setBorder(BorderFactory.createTitledBorder("Activity Heatmap"));
        heatmapCard.add(heatmapPanel, BorderLayout.CENTER);

        JPanel xpCard = new JPanel(new java.awt.GridLayout(4, 1, 2, 2));
        xpCard.setBorder(BorderFactory.createTitledBorder("Cognitive XP"));
        xpValueLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        xpValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        xpCard.add(xpValueLabel);
        xpBreakdownLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        xpBreakdownLabel.setHorizontalAlignment(SwingConstants.CENTER);
        xpCard.add(xpBreakdownLabel);
        xpAnalyticsLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        xpAnalyticsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        xpCard.add(xpAnalyticsLabel);
        xpRateLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        xpRateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        xpRateLabel.setText("1 XP/min study | -0.25 XP/min break");
        xpCard.add(xpRateLabel);

        JTable dailyXpTable = new JTable(dailyXpAnalysisModel);
        dailyXpTable.setRowHeight(24);
        JPanel dailyXpCard = new JPanel(new BorderLayout(8, 8));
        dailyXpCard.setBorder(BorderFactory.createTitledBorder("Daily XP Ledger & Comparison"));
        JScrollPane dailyXpScroll = new JScrollPane(dailyXpTable);
        dailyXpScroll.setPreferredSize(new Dimension(200, 180));
        dailyXpCard.add(dailyXpScroll, BorderLayout.CENTER);

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

        // Row 0: Goal, Sessions, Duration, XP (all 1x1)
        gbc.gridy = 0;
        gbc.weighty = 0.0;

        gbc.gridx = 0; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weightx = 1.0;
        mainContent.add(goalCard, gbc);

        gbc.gridx = 1; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weightx = 1.0;
        mainContent.add(sessionsCard, gbc);

        gbc.gridx = 2; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weightx = 1.0;
        mainContent.add(durationCard, gbc);

        gbc.gridx = 3; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weightx = 1.0;
        mainContent.add(xpCard, gbc);

        // Row 1: Pie Chart (starts Col 0, spans 2 Cols and 2 Rows)
        gbc.gridy = 1;
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.gridheight = 2; gbc.weightx = 2.0; gbc.weighty = 1.0;
        mainContent.add(chartCard, gbc);

        // Row 1: By Subject (starts Col 2, spans 1 Col and 2 Rows)
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.gridheight = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
        mainContent.add(subjectCard, gbc);

        // Row 1: By Chapter (starts Col 3, spans 1 Col and 2 Rows)
        gbc.gridx = 3; gbc.gridwidth = 1; gbc.gridheight = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
        mainContent.add(chapterCard, gbc);

        // Row 3: By Activity, Questions Solved by Topic, Revision by Topic (various gridwidths)
        gbc.gridy = 3; gbc.gridheight = 1; gbc.weighty = 0.5;

        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 1.0;
        mainContent.add(activityCard, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 2.0;
        mainContent.add(questionsTopicCard, gbc);

        gbc.gridx = 3; gbc.gridwidth = 1; gbc.weightx = 1.0;
        mainContent.add(revisionCard, gbc);

        // Row 4: By Day of Week (Spans all 4 columns, 1 row)
        gbc.gridy = 4;
        gbc.gridx = 0; gbc.gridwidth = 4; gbc.gridheight = 1; gbc.weightx = 1.0; gbc.weighty = 0.0;
        mainContent.add(dayOfWeekCard, gbc);

        // Row 5: Daily Session Timeline (Spans all 4 columns, 1 row)
        gbc.gridy = 5;
        gbc.gridx = 0; gbc.gridwidth = 4; gbc.gridheight = 1; gbc.weightx = 1.0; gbc.weighty = 0.0;
        mainContent.add(timelineCard, gbc);

        // Row 6: Left: Activity Heatmap (starts Col 0, spans 1 Col), Right: Daily XP Ledger (starts Col 1, spans 3 Cols)
        gbc.gridy = 6;
        gbc.gridheight = 1; gbc.weighty = 0.5;
        
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 1.0;
        mainContent.add(heatmapCard, gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 3.0;
        mainContent.add(dailyXpCard, gbc);

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
        
        java.time.format.DateTimeFormatter customLabelFormatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM);

        if ("Custom Range...".equals(period)) {
            DateRange dr = promptDateRange("Select Analysis Date Range", LocalDate.now().minusDays(7), LocalDate.now());
            if (dr != null) {
                customAnalysisStartDate = dr.startDate;
                customAnalysisEndDate = dr.endDate;
                String customLabel = "Custom: " + dr.startDate.format(customLabelFormatter) + " to " + dr.endDate.format(customLabelFormatter);
                
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
                customAnalysisStartDate = LocalDate.parse(parts[0], customLabelFormatter);
                customAnalysisEndDate = LocalDate.parse(parts[1], customLabelFormatter);
            } catch (Exception e) {
                customAnalysisStartDate = null;
                customAnalysisEndDate = null;
            }
        }
        
        final String finalPeriod = period;
        LocalDate today = LocalDate.now();
        boolean isSingleDay = false;
        LocalDate singleDayTargetDate = today;
        LocalDate endDate = today;

        switch (finalPeriod) {
            case "Today":
                isSingleDay = true;
                singleDayTargetDate = today;
                endDate = today;
                break;
            case "Yesterday":
                isSingleDay = true;
                singleDayTargetDate = today.minusDays(1);
                endDate = today.minusDays(1);
                break;
            default:
                if (finalPeriod.startsWith("Custom: ") && customAnalysisStartDate != null && customAnalysisEndDate != null) {
                    if (customAnalysisStartDate.equals(customAnalysisEndDate)) {
                        isSingleDay = true;
                        singleDayTargetDate = customAnalysisStartDate;
                    }
                    endDate = customAnalysisEndDate;
                } else {
                    endDate = today;
                }
                break;
        }

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
        
        totalDurationValueLabel.setText(formatDuration(totalSeconds));

        // Total Sessions card calculations (filtered by sessionsFilterCombo)
        String sessionTypeFilter = "All";
        if (sessionsFilterCombo != null) {
            sessionTypeFilter = (String) sessionsFilterCombo.getSelectedItem();
            if (sessionTypeFilter == null) sessionTypeFilter = "All";
        }
        
        final String finalTypeFilter = sessionTypeFilter;
        List<SessionRecord> cardSessions = sessions.stream()
            .filter(s -> {
                String desc = s.getDescription() != null ? s.getDescription() : "";
                if ("All".equals(finalTypeFilter)) return true;
                if ("Questions".equals(finalTypeFilter)) return desc.startsWith("Questions: ");
                if ("Revision".equals(finalTypeFilter)) return desc.startsWith("Revision: ");
                if ("Lecture".equals(finalTypeFilter)) return desc.startsWith("Lecture: ");
                if ("General".equals(finalTypeFilter)) {
                    return !desc.startsWith("Questions: ") && !desc.startsWith("Revision: ") && !desc.startsWith("Lecture: ");
                }
                return true;
            })
            .collect(Collectors.toList());

        int totalCardSessions = cardSessions.size();
        long totalCardSeconds = cardSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
        long totalCardSpanSeconds = 0;
        for (SessionRecord s : cardSessions) {
            long span = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
            totalCardSpanSeconds += span;
        }
        double cardEfficiency = totalCardSpanSeconds > 0 ? ((double) totalCardSeconds / totalCardSpanSeconds) * 100.0 : 100.0;

        totalSessionsValueLabel.setText(String.valueOf(totalCardSessions));
        if (totalCardSessions > 0) {
            avgSessionLabel.setText("Avg Duration: " + formatDuration(totalCardSeconds / totalCardSessions));
            studyEfficiencyLabel.setText(String.format("Study Efficiency: %.1f%%", cardEfficiency));
        } else {
            avgSessionLabel.setText("Avg Duration: 00:00:00");
            studyEfficiencyLabel.setText("Study Efficiency: 100.0%");
        }

        // Active day average (overall)
        long uniqueDays = sessions.stream()
            .map(s -> s.getStartTime().toLocalDate())
            .distinct()
            .count();
        if (uniqueDays > 0) {
            activeDayAvgLabel.setText("Active Day Avg: " + formatDuration(totalSeconds / uniqueDays));
        } else {
            activeDayAvgLabel.setText("Active Day Avg: 00:00:00");
        }

        // Mid-session breaks (overall)
        long totalSpanSeconds = 0;
        long totalActiveSeconds = 0;
        for (SessionRecord s : sessions) {
            long span = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
            totalSpanSeconds += span;
            totalActiveSeconds += s.getDurationSeconds();
        }
        long totalMidSessionBreaks = Math.max(0, totalSpanSeconds - totalActiveSeconds);

        if (totalSessions > 0) {
            midSessionBreakLabel.setText(String.format("Mid-Session Breaks: %s", formatDuration(totalMidSessionBreaks)));
        } else {
            midSessionBreakLabel.setText("Mid-Session Breaks: 00:00:00");
        }

        // XP Calculation
        int totalXpGained = 0;
        int totalXpDeducted = 0;
        
        java.util.Map<LocalDate, Integer> dailyNetXp = new java.util.HashMap<>();
        java.util.Map<LocalDate, Integer> dailyGainedXp = new java.util.HashMap<>();
        java.util.Map<LocalDate, Integer> dailyDeductedXp = new java.util.HashMap<>();
        java.util.Map<LocalDate, Long> dailyActiveSec = new java.util.HashMap<>();
        java.util.Map<LocalDate, Long> dailyBreakSec = new java.util.HashMap<>();

        for (SessionRecord s : sessions) {
            LocalDate date = s.getStartTime().toLocalDate();
            long active = s.getDurationSeconds();
            long span = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
            long breakS = Math.max(0, span - active);

            dailyActiveSec.put(date, dailyActiveSec.getOrDefault(date, 0L) + active);
            dailyBreakSec.put(date, dailyBreakSec.getOrDefault(date, 0L) + breakS);
        }

        for (LocalDate date : dailyActiveSec.keySet()) {
            long activeSec = dailyActiveSec.get(date);
            long breakSec = dailyBreakSec.get(date);

            int gained = (int) Math.round(activeSec / 60.0);
            int deducted = (int) Math.round(breakSec / 60.0 * 0.25);
            int net = gained - deducted;

            dailyGainedXp.put(date, gained);
            dailyDeductedXp.put(date, deducted);
            dailyNetXp.put(date, net);

            totalXpGained += gained;
            totalXpDeducted += deducted;
        }

        int totalNetXp = totalXpGained - totalXpDeducted;
        xpValueLabel.setText(String.format("%s%d XP", totalNetXp >= 0 ? "+" : "", totalNetXp));
        xpBreakdownLabel.setText(String.format("Gained: +%d | Breaks: -%d", totalXpGained, totalXpDeducted));

        // Calculate peak and average
        int peakXp = 0;
        LocalDate peakDate = null;
        for (java.util.Map.Entry<LocalDate, Integer> entry : dailyNetXp.entrySet()) {
            if (peakDate == null || entry.getValue() > peakXp) {
                peakXp = entry.getValue();
                peakDate = entry.getKey();
            }
        }

        long activeDaysCount = dailyNetXp.size();
        double avgXpPerDay = activeDaysCount > 0 ? (double) totalNetXp / activeDaysCount : 0.0;

        java.time.format.DateTimeFormatter xpJdFormatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM);

        if (peakDate != null) {
            xpAnalyticsLabel.setText(String.format("Avg: %.1f/day | Peak: %d (%s)", avgXpPerDay, peakXp, peakDate.format(xpJdFormatter)));
        } else {
            xpAnalyticsLabel.setText(String.format("Avg: %.1f/day | Peak: -", avgXpPerDay));
        }

        // Populate Daily XP table
        dailyXpAnalysisModel.setRowCount(0);
        dailyNetXp.keySet().stream()
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(date -> {
                long active = dailyActiveSec.get(date);
                long breakS = dailyBreakSec.get(date);
                int gained = dailyGainedXp.get(date);
                int deducted = dailyDeductedXp.get(date);
                int net = dailyNetXp.get(date);

                String dayOfWeekName = date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault());
                String dateStr = date.format(xpJdFormatter) + " (" + dayOfWeekName + ")";

                dailyXpAnalysisModel.addRow(new Object[]{
                    dateStr,
                    formatDuration(active),
                    formatDuration(breakS),
                    String.format("+%d", gained),
                    String.format("-%d", deducted),
                    String.format("%s%d", net >= 0 ? "+" : "", net)
                });
            });

        // Goal & Streak update


        int dailyGoalMinutes = storageService.loadDailyGoalMinutes();

        if (isSingleDay) {
            GoalStreakStats stats = calculateGoalStreakStats(singleDayTargetDate);
            String labelPrefix = "Today";
            if (finalPeriod.equals("Yesterday")) {
                labelPrefix = "Yesterday";
            } else if (finalPeriod.startsWith("Custom: ")) {
                labelPrefix = singleDayTargetDate.format(xpJdFormatter);
            }

            goalProgressLabel.setText(String.format("%s: %d / %d mins (%d%%)", 
                labelPrefix, stats.todayMinutes, stats.dailyGoalMinutes, 
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
        } else {
            long daysInPeriod = 1;
            switch (finalPeriod) {
                case "This Week":
                    LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                    daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, today) + 1;
                    break;
                case "Last 7 Days":
                    daysInPeriod = 7;
                    break;
                case "This Month":
                    LocalDate startOfMonth = today.withDayOfMonth(1);
                    daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(startOfMonth, today) + 1;
                    break;
                case "Last 30 Days":
                    daysInPeriod = 30;
                    break;
                default:
                    if (finalPeriod.startsWith("Custom: ") && customAnalysisStartDate != null && customAnalysisEndDate != null) {
                        daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(customAnalysisStartDate, customAnalysisEndDate) + 1;
                    } else {
                        // All Time
                        if (rawSessions.isEmpty()) {
                            daysInPeriod = 1;
                        } else {
                            LocalDate earliest = rawSessions.stream()
                                .map(s -> s.getStartTime().toLocalDate())
                                .min(LocalDate::compareTo)
                                .orElse(today);
                            daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(earliest, today) + 1;
                        }
                    }
                    break;
            }
            if (daysInPeriod <= 0) {
                daysInPeriod = 1;
            }

            double avgMinutes = (double) totalSeconds / (daysInPeriod * 60.0);
            int percent = dailyGoalMinutes > 0 ? (int) Math.round((avgMinutes * 100.0) / dailyGoalMinutes) : 0;

            goalProgressLabel.setText(String.format("Average: %.1f / %d mins (%d%%)", 
                avgMinutes, dailyGoalMinutes, percent));

            goalProgressBar.setValue(Math.min(100, percent));
            if (avgMinutes >= dailyGoalMinutes) {
                goalProgressBar.setForeground(new java.awt.Color(46, 139, 87));
            } else {
                goalProgressBar.setForeground(new java.awt.Color(70, 130, 180));
            }

            GoalStreakStats stats = calculateGoalStreakStats(endDate);
            streakLabel.setText(String.format("Streak: %d days %s (Max: %d 🏆)", 
                stats.currentStreak, stats.currentStreak > 0 ? "🔥" : "💤", stats.maxStreak));
        }
        
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
        long revisionSec = 0;
        long generalSec = 0;
        int dppQ = 0;
        int practiceQ = 0;
        int pyqQ = 0;
        java.util.Map<String, Long> byRevisionTopic = new java.util.HashMap<>();
        java.util.Map<String, Long> byQuestionTopicDuration = new java.util.HashMap<>();
        java.util.Map<String, Integer> byQuestionTopicSolved = new java.util.HashMap<>();
 
        String topicFilter = "All (Practice & PYQ)";
        if (questionsTopicFilterCombo != null) {
            topicFilter = (String) questionsTopicFilterCombo.getSelectedItem();
            if (topicFilter == null) topicFilter = "All (Practice & PYQ)";
        }

        for (SessionRecord session : sessions) {
            String desc = session.getDescription() != null ? session.getDescription() : "";
            long sec = session.getDurationSeconds();
            if (desc.startsWith("Questions: ")) {
                String qType = desc.substring("Questions: ".length());
                if (qType.startsWith("DPP Questions")) {
                    dppSec += sec;
                    dppQ += session.getQuestionsSolved();
                } else if (qType.startsWith("Practice Book Questions")) {
                    practiceSec += sec;
                    practiceQ += session.getQuestionsSolved();
                } else if (qType.startsWith("Previous Year Questions")) {
                    pyqSec += sec;
                    pyqQ += session.getQuestionsSolved();
                } else {
                    generalSec += sec;
                }

                boolean includeTopic = false;
                if ("All (Practice & PYQ)".equals(topicFilter)) {
                    includeTopic = qType.startsWith("Practice Book Questions") || qType.startsWith("Previous Year Questions");
                } else if ("Practice Book".equals(topicFilter)) {
                    includeTopic = qType.startsWith("Practice Book Questions");
                } else if ("Previous Year".equals(topicFilter)) {
                    includeTopic = qType.startsWith("Previous Year Questions");
                }

                if (includeTopic) {
                    // Parse the topic (qDesc) from the description
                    String content = desc.substring("Questions: ".length());
                    int solvedIdx = content.indexOf(" (Solved:");
                    if (solvedIdx != -1) {
                        content = content.substring(0, solvedIdx);
                    }
                    String qDesc = "";
                    int commaIdx = content.indexOf(',');
                    if (commaIdx != -1) {
                        qDesc = content.substring(commaIdx + 1).trim();
                    }
                    if (qDesc.isEmpty()) {
                        qDesc = "General / Unnamed";
                    }
                    byQuestionTopicDuration.put(qDesc, byQuestionTopicDuration.getOrDefault(qDesc, 0L) + sec);
                    byQuestionTopicSolved.put(qDesc, byQuestionTopicSolved.getOrDefault(qDesc, 0) + session.getQuestionsSolved());
                }
            } else if (desc.startsWith("Revision: ")) {
                String topic = desc.substring("Revision: ".length()).trim();
                if (topic.isEmpty()) topic = "General/Unnamed";
                byRevisionTopic.put(topic, byRevisionTopic.getOrDefault(topic, 0L) + sec);
                revisionSec += sec;
            } else if (!desc.startsWith("Lecture: ")) {
                generalSec += sec;
            }
        }
 
        String dppAvg = "-";
        if (dppQ > 0) {
            dppAvg = formatAvgQTime((double) dppSec / dppQ);
        }
        String practiceAvg = "-";
        if (practiceQ > 0) {
            practiceAvg = formatAvgQTime((double) practiceSec / practiceQ);
        }
        String pyqAvg = "-";
        if (pyqQ > 0) {
            pyqAvg = formatAvgQTime((double) pyqSec / pyqQ);
        }

        activityAnalysisModel.setRowCount(0);
        activityAnalysisModel.addRow(new Object[]{"DPP Questions", formatDuration(dppSec), dppQ > 0 ? String.valueOf(dppQ) : "-", dppAvg});
        activityAnalysisModel.addRow(new Object[]{"Practice Book Questions", formatDuration(practiceSec), practiceQ > 0 ? String.valueOf(practiceQ) : "-", practiceAvg});
        activityAnalysisModel.addRow(new Object[]{"Previous Year Questions", formatDuration(pyqSec), pyqQ > 0 ? String.valueOf(pyqQ) : "-", pyqAvg});
        activityAnalysisModel.addRow(new Object[]{"Revision (Total)", formatDuration(revisionSec), "-", "-"});
        activityAnalysisModel.addRow(new Object[]{"General / Other", formatDuration(generalSec), "-", "-"});
 
        revisionAnalysisModel.setRowCount(0);
        java.util.List<java.util.Map.Entry<String, Long>> sortedRevisions = new java.util.ArrayList<>(byRevisionTopic.entrySet());
        sortedRevisions.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));
        for (java.util.Map.Entry<String, Long> entry : sortedRevisions) {
            revisionAnalysisModel.addRow(new Object[]{entry.getKey(), formatDuration(entry.getValue())});
        }

        questionsByTopicAnalysisModel.setRowCount(0);
        java.util.List<java.util.Map.Entry<String, Long>> sortedQuestionTopics = new java.util.ArrayList<>(byQuestionTopicDuration.entrySet());
        sortedQuestionTopics.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));
        for (java.util.Map.Entry<String, Long> entry : sortedQuestionTopics) {
            String topic = entry.getKey();
            long sec = entry.getValue();
            int solved = byQuestionTopicSolved.getOrDefault(topic, 0);
            String avgTimeStr = "-";
            if (solved > 0) {
                avgTimeStr = formatAvgQTime((double) sec / solved);
            }
            questionsByTopicAnalysisModel.addRow(new Object[]{
                topic,
                formatDuration(sec),
                solved > 0 ? String.valueOf(solved) : "-",
                avgTimeStr
            });
        }

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

        // Update session timeline (either single-day detailed timeline or multi-day average timeline)
        if (isSingleDay) {
            dailyTimelinePanel.setSingleDayData(rawSessions, singleDayTargetDate);
        } else {
            // Find days in period
            long daysInPeriod = 1;
            switch (finalPeriod) {
                case "This Week":
                    LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                    daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, today) + 1;
                    break;
                case "Last 7 Days":
                    daysInPeriod = 7;
                    break;
                case "This Month":
                    LocalDate startOfMonth = today.withDayOfMonth(1);
                    daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(startOfMonth, today) + 1;
                    break;
                case "Last 30 Days":
                    daysInPeriod = 30;
                    break;
                default:
                    if (finalPeriod.startsWith("Custom: ") && customAnalysisStartDate != null && customAnalysisEndDate != null) {
                        daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(customAnalysisStartDate, customAnalysisEndDate) + 1;
                    } else {
                        // All Time
                        if (rawSessions.isEmpty()) {
                            daysInPeriod = 1;
                        } else {
                            LocalDate earliest = rawSessions.stream()
                                .map(s -> s.getStartTime().toLocalDate())
                                .min(LocalDate::compareTo)
                                .orElse(today);
                            daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(earliest, today) + 1;
                        }
                    }
                    break;
            }
            if (daysInPeriod <= 0) {
                daysInPeriod = 1;
            }
            dailyTimelinePanel.setAverageData(sessions, daysInPeriod);
        }

        if (timelineBorder != null) {
            if (isSingleDay) {
                String titleDateStr = "Today";
                if (finalPeriod.equals("Yesterday")) {
                    titleDateStr = "Yesterday";
                } else if (finalPeriod.startsWith("Custom: ")) {
                    titleDateStr = singleDayTargetDate.format(xpJdFormatter);
                }

                if (titleDateStr.equals("Today")) {
                    timelineBorder.setTitle("Today's Session Timeline");
                } else if (titleDateStr.equals("Yesterday")) {
                    timelineBorder.setTitle("Yesterday's Session Timeline");
                } else {
                    timelineBorder.setTitle("Session Timeline (" + titleDateStr + ")");
                }
            } else {
                String rangeLabel = finalPeriod;
                if (finalPeriod.startsWith("Custom: ")) {
                    rangeLabel = finalPeriod.substring("Custom: ".length());
                }
                timelineBorder.setTitle("Average Session Timeline (" + rangeLabel + ")");
            }
            if (dailyTimelinePanel.getParent() != null) {
                dailyTimelinePanel.getParent().repaint();
            }
        }

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
            JOptionPane.showMessageDialog(this, "Add at least one subject in the Subjects tab first.", "No Subjects", JOptionPane.WARNING_MESSAGE);
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
            currentSessionPauseCount = 0;
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
        currentSessionPauseCount++;
        updateStopwatchDisplay();
        updateStopwatchButtons();
    }

    public void stopAndLogStopwatch() {
        if (!stopwatchStarted) {
            return;
        }

        if (stopwatchRunning) {
            stopwatchElapsedMillis += (System.nanoTime() - stopwatchStartNano) / 1_000_000;
            stopwatchRunning = false;
            stopwatchUiTimer.stop();
        }

        long durationSeconds = Math.max(1, stopwatchElapsedMillis / 1000);
        LocalDateTime end = LocalDateTime.now();

        String activityType = (String) stopwatchActivityTypeCombo.getSelectedItem();
        String activityDetail = "";
        int questionsSolved = 0;

        if ("General".equals(activityType)) {
            activityDetail = stopwatchActivityField.getText().trim();
        } else if ("Questions".equals(activityType)) {
            String qType = (String) stopwatchQuestionTypeCombo.getSelectedItem();
            Object selectedObj = stopwatchQuestionDescCombo.getSelectedItem();
            String qDesc = (selectedObj == null || "[Add Custom...]".equals(selectedObj)) ? "" : ((String) selectedObj).trim();
            if (!qDesc.isEmpty() && qType != null) {
                String subject = (String) stopwatchSubjectCombo.getSelectedItem();
                if (subject == null) subject = "";
                storageService.saveLastQuestionDesc(qType, subject, qDesc);
            }
            if ("Practice Book Questions".equals(qType) || "Previous Year Questions".equals(qType)) {
                while (true) {
                    String input = JOptionPane.showInputDialog(
                        this,
                        "Enter number of questions solved during this session:",
                        "Questions Solved",
                        JOptionPane.QUESTION_MESSAGE
                    );
                    if (input == null) {
                        break; // Cancelled
                    }
                    String trimmed = input.trim();
                    if (trimmed.isEmpty()) {
                        break; // Empty
                    }
                    try {
                        questionsSolved = Integer.parseInt(trimmed);
                        if (questionsSolved >= 0) {
                            break;
                        } else {
                            JOptionPane.showMessageDialog(this, "Please enter a non-negative integer.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this, "Please enter a valid integer.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                    }
                }
                if (questionsSolved > 0) {
                    double avgTimeSec = (double) durationSeconds / questionsSolved;
                    String avgTimeStr = formatAvgQTime(avgTimeSec);
                    activityDetail = "Questions: " + qType + (qDesc.isEmpty() ? "" : ", " + qDesc)
                                     + " (Solved: " + questionsSolved + ", Avg Time: " + avgTimeStr + ")";
                } else {
                    activityDetail = "Questions: " + qType + (qDesc.isEmpty() ? "" : ", " + qDesc);
                }
            } else {
                activityDetail = "Questions: " + qType + (qDesc.isEmpty() ? "" : ", " + qDesc);
            }
        } else if ("Lecture".equals(activityType)) {
            String ch = stopwatchChapterField.getText().trim();
            String lec = stopwatchLectureField.getText().trim();
            activityDetail = "Lecture: Ch " + ch + ", Lec " + lec;
        } else if ("Revision".equals(activityType)) {
            String topic = stopwatchRevisionTopicField.getText().trim();
            activityDetail = "Revision: " + topic;
        }

        if (resumedSession != null && resumedSession.getType() == SessionRecord.SessionType.STOPWATCH) {
            List<SessionRecord> allSessions = storageService.loadSessions();
            boolean updated = false;
            for (int i = 0; i < allSessions.size(); i++) {
                SessionRecord s = allSessions.get(i);
                if (s.getStartTime().equals(resumedSession.getStartTime()) &&
                    s.getSubject().equalsIgnoreCase(resumedSession.getSubject()) &&
                    s.getType() == resumedSession.getType()) {
                    int totalPauses = resumedSession.getPauseCount() + currentSessionPauseCount;
                    int totalQuestions = resumedSession.getQuestionsSolved() + questionsSolved;
                    
                    if (totalQuestions > 0) {
                        double avgTimeSec = (double) durationSeconds / totalQuestions;
                        String avgTimeStr = formatAvgQTime(avgTimeSec);
                        String cleanDetail = activityDetail;
                        int idx = cleanDetail.indexOf(" (Solved:");
                        if (idx != -1) {
                            cleanDetail = cleanDetail.substring(0, idx);
                        }
                        activityDetail = cleanDetail + " (Solved: " + totalQuestions + ", Avg Time: " + avgTimeStr + ")";
                    }
                    
                    allSessions.set(i, new SessionRecord(
                        SessionRecord.SessionType.STOPWATCH,
                        stopwatchSubject,
                        stopwatchSessionStart,
                        end,
                        durationSeconds,
                        activityDetail,
                        totalPauses,
                        totalQuestions
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
                    activityDetail,
                    currentSessionPauseCount,
                    questionsSolved
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
                activityDetail,
                currentSessionPauseCount,
                questionsSolved
            ));
        }

        refreshSessionsTable();
        refreshExportAvailability();
        notifySessionLogged(durationSeconds);
        resetStopwatch();
        JOptionPane.showMessageDialog(this, "Stopwatch session logged.", "Saved", JOptionPane.INFORMATION_MESSAGE);
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
        stopwatchQuestionDescCombo.setSelectedIndex(-1);
        stopwatchRevisionTopicField.setText("");
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
        stopwatchQuestionDescCombo.setEnabled(!stopwatchStarted);
        stopwatchChapterField.setEnabled(!stopwatchStarted);
        stopwatchLectureField.setEnabled(!stopwatchStarted);
        stopwatchRevisionTopicField.setEnabled(!stopwatchStarted);
    }

    public void startTimer() {
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
            currentSessionPauseCount = 0;
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
            currentSessionPauseCount++;
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
            JOptionPane.showMessageDialog(this, "Timer session logged.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        }

        resetTimer();
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

        if (timerRemainingSeconds <= 0) {
            timerTick.stop();
            timerRunning = false;
            long elapsed = timerTotalSeconds;
            logTimerSession(elapsed);
            notifySessionLogged(elapsed);
            JOptionPane.showMessageDialog(this, "Timer finished and session logged.", "Completed", JOptionPane.INFORMATION_MESSAGE);
            resetTimer();
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
                    int totalPauses = resumedSession.getPauseCount() + currentSessionPauseCount;
                    allSessions.set(i, new SessionRecord(
                        SessionRecord.SessionType.TIMER,
                        subject,
                        timerSessionStart,
                        LocalDateTime.now(),
                        newDuration,
                        resumedSession.getDescription(),
                        totalPauses
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
                    resumedSession.getDescription(),
                    currentSessionPauseCount
                ));
            }
            resumedSession = null;
        } else {
            storageService.appendSession(new SessionRecord(
                SessionRecord.SessionType.TIMER,
                subject,
                timerSessionStart != null ? timerSessionStart : LocalDateTime.now(),
                LocalDateTime.now(),
                elapsedSeconds,
                "",
                currentSessionPauseCount
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

        java.time.format.DateTimeFormatter customLabelFormatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM);

        if ("Custom Range...".equals(datePeriod)) {
            DateRange dr = promptDateRange("Select Log Date Range", LocalDate.now().minusDays(7), LocalDate.now());
            if (dr != null) {
                customLogsStartDate = dr.startDate;
                customLogsEndDate = dr.endDate;
                String customLabel = "Custom: " + dr.startDate.format(customLabelFormatter) + " to " + dr.endDate.format(customLabelFormatter);
                
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
                customLogsStartDate = LocalDate.parse(parts[0], customLabelFormatter);
                customLogsEndDate = LocalDate.parse(parts[1], customLabelFormatter);
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

        java.time.format.DateTimeFormatter dlgFormatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM);

        JPanel startPanel = new JPanel(new BorderLayout(6, 6));
        startPanel.setOpaque(false);
        JLabel startLabel = new JLabel("Start: " + defaultStart.format(dlgFormatter), SwingConstants.CENTER);
        startLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        startPanel.add(startLabel, BorderLayout.NORTH);
        
        final LocalDate[] selectedStart = {defaultStart};
        final LocalDate[] selectedEnd = {defaultEnd};

        CalendarPanel startCalendar = new CalendarPanel(defaultStart, date -> {
            selectedStart[0] = date;
            startLabel.setText("Start: " + date.format(dlgFormatter));
        });
        startCalendar.setPreferredSize(new Dimension(280, 240));
        startPanel.add(startCalendar, BorderLayout.CENTER);

        JPanel endPanel = new JPanel(new BorderLayout(6, 6));
        endPanel.setOpaque(false);
        JLabel endLabel = new JLabel("End: " + defaultEnd.format(dlgFormatter), SwingConstants.CENTER);
        endLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        endPanel.add(endLabel, BorderLayout.NORTH);

        CalendarPanel endCalendar = new CalendarPanel(defaultEnd, date -> {
            selectedEnd[0] = date;
            endLabel.setText("End: " + date.format(dlgFormatter));
        });
        endCalendar.setPreferredSize(new Dimension(280, 240));
        endPanel.add(endCalendar, BorderLayout.CENTER);

        panel.add(startPanel);
        panel.add(endPanel);

        ThemeManager.applyTheme(panel, ThemeManager.getColors(ThemeManager.loadTheme()));

        int result = JOptionPane.showConfirmDialog(this,
            panel,
            title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            LocalDate startLocal = selectedStart[0];
            LocalDate endLocal = selectedEnd[0];

            if (startLocal.isAfter(endLocal)) {
                JOptionPane.showMessageDialog(this,
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
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatAvgQTime(double avgSec) {
        if (avgSec >= 60) {
            long mins = (long) avgSec / 60;
            long secs = (long) Math.round(avgSec % 60);
            if (secs == 60) {
                mins++;
                secs = 0;
            }
            return mins + "m:" + secs + "s/q";
        } else if (avgSec >= 1.0) {
            return Math.round(avgSec) + "s/q";
        } else {
            return String.format("%.1fs/q", avgSec);
        }
    }

    // MiniWindow launch methods removed (mini mode discarded)

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
            "  Alt + R : Reset Stopwatch\n\n" +
            "Timer:\n" +
            "  Alt + S : Start Timer\n" +
            "  Alt + P : Pause / Resume Timer\n" +
            "  Alt + T : Stop Timer\n" +
            "  Alt + R : Reset Timer";
        
        JOptionPane.showMessageDialog(this,
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
        return calculateGoalStreakStats(LocalDate.now());
    }

    public GoalStreakStats calculateGoalStreakStats(LocalDate upToDate) {
        GoalStreakStats stats = new GoalStreakStats();
        stats.dailyGoalMinutes = storageService.loadDailyGoalMinutes();
        
        List<SessionRecord> sessions = storageService.loadSessions();
        
        // Group seconds by date
        java.util.Map<LocalDate, Long> secondsPerDay = new java.util.HashMap<>();
        for (SessionRecord session : sessions) {
            LocalDate date = session.getStartTime().toLocalDate();
            long sec = session.getDurationSeconds();
            secondsPerDay.put(date, secondsPerDay.getOrDefault(date, 0L) + sec);
        }
        
        stats.todayMinutes = (int) (secondsPerDay.getOrDefault(upToDate, 0L) / 60);
        stats.todayGoalMet = stats.todayMinutes >= stats.dailyGoalMinutes;
        
        // Helper to check if goal was met on a date
        java.util.function.Predicate<LocalDate> metGoal = date -> {
            long sec = secondsPerDay.getOrDefault(date, 0L);
            return (sec / 60) >= stats.dailyGoalMinutes;
        };
        
        // 1. Calculate Current Streak
        int currentStreak = 0;
        if (metGoal.test(upToDate)) {
            LocalDate d = upToDate;
            while (metGoal.test(d)) {
                currentStreak++;
                d = d.minusDays(1);
            }
        } else if (metGoal.test(upToDate.minusDays(1))) {
            LocalDate d = upToDate.minusDays(1);
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
            .orElse(upToDate);
            
        int maxStreak = 0;
        int running = 0;
        LocalDate d = earliest;
        while (!d.isAfter(upToDate)) {
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
        
        int result = JOptionPane.showConfirmDialog(this,
            panel,
            "Set Daily Goal",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
            
        if (result == JOptionPane.OK_OPTION) {
            int hrs = (Integer) hrSpinner.getValue();
            int mins = (Integer) minSpinner.getValue();
            int totalMins = hrs * 60 + mins;
            if (totalMins <= 0) {
                JOptionPane.showMessageDialog(this,
                    "Goal must be greater than 0 minutes.",
                    "Invalid Goal",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            storageService.saveDailyGoalMinutes(totalMins);
            refreshAnalysis();
            JOptionPane.showMessageDialog(this,
                "Daily goal saved successfully!",
                "Goal Saved",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void updateTheme(ThemeManager.AppTheme theme) {
        ThemeManager.saveTheme(theme);
        ThemeManager.ThemeColors colors = ThemeManager.getColors(theme);
        ThemeManager.applyTheme(this, colors);
        if (aiChatLogPane != null) {
            rebuildChatHtml();
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
                cleanupSystemTray();
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

    public void cleanupSystemTray() {
        if (SystemTray.isSupported() && trayIcon != null && trayIconAdded) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
                trayIconAdded = false;
            } catch (Exception ignored) {}
        }
    }

    public void restoreFromTray() {
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
        requestFocus();
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
        addDetailRow(detailsCard, "Avg Attention Span:", insightDetailAttentionSpan, gbc, 6);
        addDetailRow(detailsCard, "Cognitive Focus Score:", insightDetailFocusScore, gbc, 7);
        addDetailRow(detailsCard, "7-Day Accumulated XP:", insightDetailTotalXp, gbc, 8);
        addDetailRow(detailsCard, "Average XP / Active Day:", insightDetailAvgXp, gbc, 9);
        
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

        // Attention span & focus score calculation
        double avgAttentionSpanMin = 0;
        double avgFocusScore = 0;
        if (!recentSessions.isEmpty()) {
            long totalActiveSec = recentSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
            long totalBlocks = recentSessions.stream().mapToLong(s -> getSessionPauseCount(s) + 1).sum();
            avgAttentionSpanMin = (totalActiveSec / (double) totalBlocks) / 60.0;
            avgFocusScore = recentSessions.stream().mapToInt(this::calculateSessionFocusScore).average().orElse(0.0);
        }

        // Format focus score text with classification
        String focusText = "-";
        if (!recentSessions.isEmpty()) {
            String focusLevel = "Fragmented";
            if (avgFocusScore >= 85) focusLevel = "Deep Focus";
            else if (avgFocusScore >= 70) focusLevel = "Good Focus";
            else if (avgFocusScore >= 50) focusLevel = "Moderate Focus";
            focusText = String.format("%d/100 (%s)", (int) avgFocusScore, focusLevel);
        }

        // Update Metrics Labels
        insightDetailHours.setText(String.format("%.1f hrs", totalHours));
        insightDetailDays.setText(consecutiveActiveDays + " days");
        insightDetailLateNights.setText(lateNightSessions + " sessions");
        insightDetailBreakRatio.setText(String.format("%.1f%%", avgBreakRatio * 100));
        insightDetailMidSessionBreaks.setText(formatDuration(totalMidSessionBreaks));
        insightDetailEfficiency.setText(String.format("%.1f%%", efficiencyVal));
        insightDetailAttentionSpan.setText(!recentSessions.isEmpty() ? String.format("%.1f mins", avgAttentionSpanMin) : "-");
        insightDetailFocusScore.setText(focusText);

        // Calculate XP metrics for the last 7 days
        int recentXpGained = (int) Math.round(totalActiveSeconds / 60.0);
        int recentXpDeducted = (int) Math.round(totalMidSessionBreaks / 60.0 * 0.25);
        int recentNetXp = recentXpGained - recentXpDeducted;
        double avgRecentXpPerDay = activeDaysCount > 0 ? (double) recentNetXp / activeDaysCount : 0.0;

        insightDetailTotalXp.setText(recentNetXp + " XP");
        insightDetailAvgXp.setText(String.format("%.1f XP/day", avgRecentXpPerDay));

        // Update Warnings & Recommendations
        warningsContainer.removeAll();
        boolean hasMessages = false;

        if (totalHours > 56.0) {
            addWarningLabel("⚠️ Extreme Load: Tracked over 56 hours. High fatigue risk.");
            hasMessages = true;
        }
        if (activeDaysCount > 0 && avgBreakRatio < 0.15) {
            addWarningLabel("⚠️ Insufficient Breaks: Break ratio is under 15%. Rest more.");
            hasMessages = true;
        }
        if (!recentSessions.isEmpty()) {
            if (avgAttentionSpanMin < 25.0) {
                addTipLabel("💡 Focus Tip: Short focus blocks. Try Pomodoro (25 mins focus / 5 mins break).");
                hasMessages = true;
            } else if (avgAttentionSpanMin > 120.0) {
                addTipLabel("💡 Focus Tip: Very long focus blocks (> 2 hrs). Remember to take a rest every hour.");
                hasMessages = true;
            }
            if (avgFocusScore < 60.0) {
                addTipLabel("💡 Focus Tip: Fragmented study. Remove phone/tab distractions to stay in the zone.");
                hasMessages = true;
            }
            double breakLossPct = recentXpGained > 0 ? ((double) recentXpDeducted / recentXpGained) * 100.0 : 0.0;
            if (breakLossPct > 20.0) {
                addTipLabel(String.format("💡 XP Tip: High break deductions (%.1f%% of XP lost). Try to minimize pauses during active study blocks.", breakLossPct));
                hasMessages = true;
            }
        }
        if (!hasMessages) {
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

    private void addTipLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
        lbl.setForeground(colors.accent);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        warningsContainer.add(lbl);
    }

    private int getSessionPauseCount(SessionRecord s) {
        if (s.getPauseCount() > 0) {
            return s.getPauseCount();
        }
        long span = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
        long active = s.getDurationSeconds();
        long breakSec = Math.max(0, span - active);
        if (breakSec == 0) {
            return 0;
        }
        return (int) Math.max(1, breakSec / 300);
    }

    private int calculateSessionFocusScore(SessionRecord s) {
        String desc = s.getDescription() != null ? s.getDescription() : "";
        if (desc.startsWith("Questions: ")) {
            return 100;
        }

        long activeSec = s.getDurationSeconds();
        if (activeSec < 60) return 100; // Ignore tiny sessions under a minute
        
        int pauses = getSessionPauseCount(s);
        double avgBlockMin = (activeSec / (double) (pauses + 1)) / 60.0;
        
        double blockScore;
        if (avgBlockMin >= 25 && avgBlockMin <= 120) {
            blockScore = 100;
        } else if (avgBlockMin < 25) {
            blockScore = Math.max(20, 100 - (25 - avgBlockMin) * 3);
        } else {
            blockScore = Math.max(50, 100 - (avgBlockMin - 120) * 0.5);
        }
        
        long spanSec = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
        double efficiency = spanSec > 0 ? ((double) activeSec / spanSec) * 100.0 : 100.0;
        
        double efficiencyScore;
        if (efficiency >= 80) {
            efficiencyScore = 100;
        } else {
            efficiencyScore = Math.max(20, 100 - (80 - efficiency) * 1.5);
        }
        
        double score = (blockScore * 0.6) + (efficiencyScore * 0.4);
        return (int) Math.max(0, Math.min(100, score));
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
        private List<SessionRecord> sessions = new ArrayList<>();
        private boolean isAverageMode = false;
        private long daysInPeriod = 1;
        private LocalDate targetDate;

        public DailyTimelinePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(600, 70));
            setToolTipText("");
        }

        public void setSingleDayData(List<SessionRecord> allSessions, LocalDate targetDate) {
            this.isAverageMode = false;
            this.daysInPeriod = 1;
            this.targetDate = targetDate;
            this.sessions = allSessions.stream()
                .filter(s -> s.getStartTime().toLocalDate().equals(targetDate))
                .sorted(java.util.Comparator.comparing(SessionRecord::getStartTime))
                .collect(Collectors.toList());
            repaint();
        }

        public void setAverageData(List<SessionRecord> filteredSessions, long daysInPeriod) {
            this.isAverageMode = true;
            this.daysInPeriod = Math.max(1, daysInPeriod);
            this.targetDate = null;
            this.sessions = new ArrayList<>(filteredSessions);
            repaint();
        }

        private int countDaysActiveAt(int minuteOfDay) {
            java.util.Set<LocalDate> activeDates = new java.util.HashSet<>();
            for (SessionRecord s : sessions) {
                LocalDate date = s.getStartTime().toLocalDate();
                int startMin = s.getStartTime().toLocalTime().toSecondOfDay() / 60;
                int endMin = s.getEndTime().toLocalTime().toSecondOfDay() / 60;
                
                if (endMin < startMin) { // Spans midnight
                    if (minuteOfDay >= startMin || minuteOfDay <= endMin) {
                        activeDates.add(date);
                    }
                } else {
                    if (minuteOfDay >= startMin && minuteOfDay <= endMin) {
                        activeDates.add(date);
                    }
                }
            }
            return activeDates.size();
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            if (sessions.isEmpty()) {
                return isAverageMode ? "No study sessions logged in this period." : "No sessions tracked on this day.";
            }

            int w = getWidth();
            int paddingLeft = 20;
            int paddingRight = 20;
            int chartW = w - paddingLeft - paddingRight;

            int mouseX = e.getX();
            if (mouseX < paddingLeft || mouseX > w - paddingRight) return null;

            if (isAverageMode) {
                double ratio = (double) (mouseX - paddingLeft) / chartW;
                int totalMinutes = 24 * 60;
                int minuteOfDay = (int) (ratio * totalMinutes);
                if (minuteOfDay < 0) minuteOfDay = 0;
                if (minuteOfDay >= totalMinutes) minuteOfDay = totalMinutes - 1;

                int hour = minuteOfDay / 60;
                int minute = minuteOfDay % 60;
                String timeStr = String.format("%02d:%02d", hour, minute);

                int activeDays = countDaysActiveAt(minuteOfDay);
                double percentage = (double) activeDays * 100.0 / daysInPeriod;
                return String.format("<html><b>Time of Day:</b> %s<br><b>Study Frequency:</b> %.1f%% (%d / %d days)</html>",
                    timeStr, percentage, activeDays, daysInPeriod);
            } else {
                LocalDateTime firstStart = sessions.get(0).getStartTime();
                LocalDateTime lastEnd = sessions.get(sessions.size() - 1).getEndTime();
                
                LocalDateTime timelineStart = firstStart.minusMinutes(30);
                LocalDateTime timelineEnd = lastEnd.plusMinutes(30);

                long totalTimelineSec = java.time.Duration.between(timelineStart, timelineEnd).toSeconds();
                if (totalTimelineSec <= 0) return null;

                double ratio = (double) (mouseX - paddingLeft) / chartW;
                long offsetSeconds = (long) (ratio * totalTimelineSec);
                LocalDateTime hoverTime = timelineStart.plusSeconds(offsetSeconds);

                for (SessionRecord s : sessions) {
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

            if (sessions.isEmpty()) {
                g2.setColor(colors.text);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                FontMetrics fm = g2.getFontMetrics();
                String msg = isAverageMode ? "No study sessions logged in this period." : "No study sessions logged on this day.";
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, barY + barH / 2 + 5);
                g2.dispose();
                return;
            }

            if (isAverageMode) {
                // 1. Draw Density Heatmap
                int[] minuteCounts = new int[1440];
                java.util.Map<Integer, java.util.Set<LocalDate>> activeDatesPerMinute = new java.util.HashMap<>();
                for (int m = 0; m < 1440; m++) {
                    activeDatesPerMinute.put(m, new java.util.HashSet<>());
                }
                for (SessionRecord s : sessions) {
                    LocalDate date = s.getStartTime().toLocalDate();
                    int startMin = s.getStartTime().toLocalTime().toSecondOfDay() / 60;
                    int endMin = s.getEndTime().toLocalTime().toSecondOfDay() / 60;

                    if (endMin < startMin) { // Spans midnight
                        for (int m = startMin; m < 1440; m++) {
                            activeDatesPerMinute.get(m).add(date);
                        }
                        for (int m = 0; m <= endMin; m++) {
                            activeDatesPerMinute.get(m).add(date);
                        }
                    } else {
                        for (int m = startMin; m <= endMin; m++) {
                            activeDatesPerMinute.get(m).add(date);
                        }
                    }
                }
                for (int m = 0; m < 1440; m++) {
                    minuteCounts[m] = activeDatesPerMinute.get(m).size();
                }

                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(paddingLeft, barY, chartW, barH, 6, 6));
                java.awt.Color accent = colors.accent;
                for (int x = paddingLeft; x < w - paddingRight; x++) {
                    double ratio = (double) (x - paddingLeft) / chartW;
                    int minuteOfDay = (int) (ratio * 1440);
                    if (minuteOfDay < 0) minuteOfDay = 0;
                    if (minuteOfDay >= 1440) minuteOfDay = 1439;

                    int count = minuteCounts[minuteOfDay];
                    if (count > 0) {
                        double prob = (double) count / daysInPeriod;
                        int alpha = (int) (prob * 255.0);
                        if (alpha > 255) alpha = 255;
                        if (alpha < 0) alpha = 0;
                        g2.setColor(new java.awt.Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
                        g2.drawLine(x, barY, x, barY + barH);
                    }
                }
                g2.setClip(null);

                // 2. Draw Ticks for full 24-hour day
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.setColor(colors.text);
                FontMetrics fm = g2.getFontMetrics();

                for (int hTick = 3; hTick <= 21; hTick += 3) {
                    double ratio = (double) hTick / 24.0;
                    int tickX = paddingLeft + (int) (ratio * chartW);

                    g2.setColor(colors.border);
                    g2.drawLine(tickX, barY + barH, tickX, barY + barH + 4);

                    g2.setColor(colors.text);
                    String tickLabel = String.format("%02d:00", hTick);
                    int lblW = fm.stringWidth(tickLabel);
                    g2.drawString(tickLabel, tickX - lblW / 2, barY + barH + 16);
                }
            } else {
                LocalDateTime firstStart = sessions.get(0).getStartTime();
                LocalDateTime lastEnd = sessions.get(sessions.size() - 1).getEndTime();
                
                LocalDateTime timelineStart = firstStart.minusMinutes(30);
                LocalDateTime timelineEnd = lastEnd.plusMinutes(30);

                long totalTimelineSec = java.time.Duration.between(timelineStart, timelineEnd).toSeconds();
                if (totalTimelineSec <= 0) totalTimelineSec = 1;

                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(paddingLeft, barY, chartW, barH, 6, 6));
                for (SessionRecord s : sessions) {
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
                g2.setClip(null);

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
            }
            g2.dispose();
        }
    }

    private javax.swing.JComponent createAiAssistantPanel() {
        // Main key setup card
        JPanel setupCard = new JPanel(new GridBagLayout());
        setupCard.setName("summaryPanel"); // inherits cardBg from theme
        setupCard.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        
        JPanel setupInner = new JPanel(new GridBagLayout());
        setupInner.setName("summaryPanel");
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        JLabel titleLabelSetup = new JLabel("🔑 AI Assistant Configuration", SwingConstants.CENTER);
        titleLabelSetup.setFont(new Font("SansSerif", Font.BOLD, 18));
        setupInner.add(titleLabelSetup, gbc);
        
        gbc.gridy = 1;
        JLabel descLabel = new JLabel("<html><div style='text-align: center; width: 360px;'>"
            + "Connect your OpenRouter account to enable personalized study coaching, workload analysis, "
            + "and focus insights based on your logged sessions data.</div></html>", SwingConstants.CENTER);
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        setupInner.add(descLabel, gbc);
        
        gbc.gridy = 2;
        JLabel privacyLabel = new JLabel("<html><div style='text-align: center; width: 360px; color: #155724; background-color: #d4edda; border: 1px solid #c3e6cb; padding: 10px;'>"
            + "🔒 <b>Local-First Privacy</b>: Your API key is stored securely in your user home folder "
            + "(<code>~/.timelogger_openrouter_key</code>) and is never sent to any server except the "
            + "official OpenRouter endpoint.</div></html>", SwingConstants.CENTER);
        privacyLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        setupInner.add(privacyLabel, gbc);
        
        gbc.gridy = 3;
        JPanel keyInputPanel = new JPanel(new BorderLayout(8, 0));
        keyInputPanel.setOpaque(false);
        JLabel keyLabel = new JLabel("API Key: ");
        keyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        keyInputPanel.add(keyLabel, BorderLayout.WEST);
        openRouterKeyField.setEchoChar('•');
        openRouterKeyField.setPreferredSize(new Dimension(280, 28));
        keyInputPanel.add(openRouterKeyField, BorderLayout.CENTER);
        setupInner.add(keyInputPanel, gbc);
        
        gbc.gridy = 4;
        ModernButton saveKeyBtn = new ModernButton("Save API Key");
        saveKeyBtn.setPreferredSize(new Dimension(150, 32));
        saveKeyBtn.addActionListener(e -> {
            String key = new String(openRouterKeyField.getPassword()).trim();
            if (key.isEmpty()) {
                JOptionPane.showMessageDialog(this, "API Key cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                storageService.saveOpenRouterApiKey(key);
                showChatInterface();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save key: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        setupInner.add(saveKeyBtn, gbc);
        
        gbc.gridy = 5;
        JLabel linkLabel = new JLabel("<html><div style='text-align: center;'>Don't have an API key? <a href='https://openrouter.ai/keys'>Get one from OpenRouter</a> (free models available)</div></html>", SwingConstants.CENTER);
        linkLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        linkLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://openrouter.ai/keys"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        setupInner.add(linkLabel, gbc);
        
        // Center the inner panel in setupCard
        setupCard.add(setupInner, new GridBagConstraints());
        
        // Chat panel card
        JPanel chatCard = new JPanel(new BorderLayout(12, 12));
        chatCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        
        // Top Toolbar
        JPanel topToolbar = new JPanel(new BorderLayout(8, 8));
        topToolbar.setOpaque(false);
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel("🤖 Study Coach", SwingConstants.LEFT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titlePanel.add(titleLabel);
        
        JPanel modelSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        modelSelectionPanel.setOpaque(false);
        JLabel modelLabel = new JLabel("Model:", SwingConstants.LEFT);
        modelLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        modelLabel.setForeground(Color.GRAY);
        modelSelectionPanel.add(modelLabel);

        JComboBox<String> modelCombo = new JComboBox<>(new String[]{
            "google/gemma-4-26b-a4b-it:free",
            "openai/gpt-oss-120b:free",
            "qwen/qwen3-next-80b-a3b-instruct:free",
            "nvidia/nemotron-3-super-120b-a12b:free",
            "qwen/qwen3-coder:free",
            "meta-llama/llama-3.3-70b-instruct:free"
        });
        modelCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        modelCombo.setPreferredSize(new Dimension(250, 22));
        
        String savedModel = storageService.loadOpenRouterModel();
        modelCombo.setSelectedItem(savedModel);
        
        modelCombo.addActionListener(e -> {
            String selected = (String) modelCombo.getSelectedItem();
            if (selected != null) {
                storageService.saveOpenRouterModel(selected);
            }
        });
        modelSelectionPanel.add(modelCombo);
        titlePanel.add(modelSelectionPanel);
        
        topToolbar.add(titlePanel, BorderLayout.WEST);
        
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topButtons.setOpaque(false);
        
        ModernButton changeKeyBtn = new ModernButton("Change API Key");
        changeKeyBtn.setPreferredSize(new Dimension(120, 26));
        changeKeyBtn.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(this, "Are you sure you want to change your API key?", "Change Key", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                storageService.saveOpenRouterApiKey("");
                openRouterKeyField.setText("");
                java.awt.CardLayout cl = (java.awt.CardLayout) aiCardPanel.getLayout();
                cl.show(aiCardPanel, "KEY_SETUP");
            }
        });
        
        ModernButton clearChatBtn = new ModernButton("Clear Chat");
        clearChatBtn.setPreferredSize(new Dimension(100, 26));
        clearChatBtn.addActionListener(e -> {
            aiChatHistoryList.clear();
            addSystemWelcomeMessage();
            saveChatHistory();
            rebuildChatHtml();
        });
        
        topButtons.add(changeKeyBtn);
        topButtons.add(clearChatBtn);
        topToolbar.add(topButtons, BorderLayout.EAST);
        chatCard.add(topToolbar, BorderLayout.NORTH);
        
        // Chat log scroll pane
        aiChatLogPane.setContentType("text/html");
        aiChatLogPane.setEditable(false);
        aiChatLogPane.putClientProperty(javax.swing.JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        
        JScrollPane logScroll = new JScrollPane(aiChatLogPane);
        logScroll.setBorder(BorderFactory.createEmptyBorder()); // Zero border looks much cleaner!
        chatCard.add(logScroll, BorderLayout.CENTER);
        
        // Bottom send pane with suggestions
        JPanel bottomContainer = new JPanel(new BorderLayout(6, 6));
        bottomContainer.setOpaque(false);
        
        // Suggestions panel
        JPanel suggestionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        suggestionPanel.setOpaque(false);
        
        String[][] suggestions = {
            {"🔥 Burnout Risk", "Check if I am at risk of burnout based on my study hours and breaks."},
            {"📊 Stats Summary", "Summarize my study statistics and streaks for the last 3 days."},
            {"💡 Focus Tips", "Provide some actionable advice to improve my cognitive focus score."}
        };
        
        for (String[] sug : suggestions) {
            ModernButton chip = new ModernButton(sug[0]);
            chip.setFont(new Font("SansSerif", Font.PLAIN, 10));
            chip.setPreferredSize(new Dimension(120, 22));
            chip.addActionListener(e -> {
                aiInputTextArea.setText(sug[1]);
                sendChatToAI();
            });
            suggestionPanel.add(chip);
        }
        bottomContainer.add(suggestionPanel, BorderLayout.NORTH);
        
        // Padded Modern Input Bar
        JPanel inputBar = new JPanel(new BorderLayout(8, 0));
        inputBar.setName("summaryPanel"); // inherits cardBg from theme
        inputBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        
        aiInputTextArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        aiInputTextArea.setBorder(BorderFactory.createEmptyBorder());
        aiInputTextArea.setOpaque(false);
        aiInputTextArea.addActionListener(e -> sendChatToAI());
        inputBar.add(aiInputTextArea, BorderLayout.CENTER);
        
        aiSendBtn.setPreferredSize(new Dimension(75, 26));
        aiSendBtn.addActionListener(e -> sendChatToAI());
        inputBar.add(aiSendBtn, BorderLayout.EAST);
        
        bottomContainer.add(inputBar, BorderLayout.CENTER);
        
        aiStatusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        aiStatusLabel.setForeground(Color.GRAY);
        bottomContainer.add(aiStatusLabel, BorderLayout.SOUTH);
        
        chatCard.add(bottomContainer, BorderLayout.SOUTH);
        
        // Add to main card panel
        aiCardPanel.add(setupCard, "KEY_SETUP");
        aiCardPanel.add(chatCard, "CHAT");
        
        // Check if key is already saved
        String savedKey = storageService.loadOpenRouterApiKey();
        if (savedKey.isEmpty()) {
            java.awt.CardLayout cl = (java.awt.CardLayout) aiCardPanel.getLayout();
            cl.show(aiCardPanel, "KEY_SETUP");
        } else {
            openRouterKeyField.setText(savedKey);
            javax.swing.SwingUtilities.invokeLater(this::showChatInterface);
        }
        
        return aiCardPanel;
    }

    private void showChatInterface() {
        java.awt.CardLayout cl = (java.awt.CardLayout) aiCardPanel.getLayout();
        cl.show(aiCardPanel, "CHAT");
        if (aiChatHistoryList.isEmpty()) {
            aiChatHistoryList.addAll(loadChatHistory());
            if (aiChatHistoryList.isEmpty()) {
                addSystemWelcomeMessage();
            }
        }
        rebuildChatHtml();
    }

    private void addSystemWelcomeMessage() {
        aiChatHistoryList.add(new ChatTurn("assistant", 
            "Hello! I am your AI Study Coach. I can help analyze your logged hours, " +
            "study habits, subject/chapter breakdown, and revision topics. Ask me questions like:\n\n" +
            "- **\"Am I at risk of burnout based on my breaks?\"**\n" +
            "- **\"Which chapters should I prioritize?\"**\n" +
            "- **\"Summarize my study statistics for the last 3 days.\"**"));
    }

    private String compileAIPromptContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the AI Study Coach for Time Logger. Below is the user's complete local study and tracking data. ");
        sb.append("Use this data to answer their questions accurately, provide personalized coaching, identify habit trends, ");
        sb.append("and suggest productivity improvements. There are no privacy restrictions on this data; analyze all of it.\n\n");
        
        List<SessionRecord> allSessions = storageService.loadSessions();
        
        // --- All-Time Summary Stats ---
        long allTimeSessions = allSessions.size();
        double allTimeHours = allSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum() / 3600.0;
        String firstSessionDate = allSessions.isEmpty() ? "N/A" : allSessions.stream().map(SessionRecord::getStartTime).min(java.util.Comparator.naturalOrder()).get().toLocalDate().toString();
        String lastSessionDate = allSessions.isEmpty() ? "N/A" : allSessions.stream().map(SessionRecord::getStartTime).max(java.util.Comparator.naturalOrder()).get().toLocalDate().toString();
        List<String> subjects = storageService.loadSubjects();

        long allTimeActiveSec = allSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
        long allTimeSpanSec = 0;
        for (SessionRecord s : allSessions) {
            long span = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
            allTimeSpanSec += span;
        }
        long allTimeMidSessionBreaks = Math.max(0, allTimeSpanSec - allTimeActiveSec);
        int allTimeXpGained = (int) Math.round(allTimeActiveSec / 60.0);
        int allTimeXpDeducted = (int) Math.round(allTimeMidSessionBreaks / 60.0 * 0.25);
        int allTimeNetXp = allTimeXpGained - allTimeXpDeducted;
        
        sb.append("### ALL-TIME SUMMARY & SYSTEM METRICS:\n");
        sb.append("- Total Logged Sessions (All-Time): ").append(allTimeSessions).append("\n");
        sb.append("- Total Tracked Hours (All-Time): ").append(String.format("%.2f hrs\n", allTimeHours));
        sb.append("- Total Net XP (All-Time): ").append(String.format("%d XP (Gained: +%d, Deducted: -%d)\n", allTimeNetXp, allTimeXpGained, allTimeXpDeducted));
        sb.append("- Tracking Active Period: From ").append(firstSessionDate).append(" to ").append(lastSessionDate).append("\n");
        sb.append("- Configured Subject List: ").append(String.join(", ", subjects)).append("\n\n");

        sb.append("### STUDY METRICS OVERVIEW (Last 7 Days):\n");
        
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);
        
        List<SessionRecord> recentSessions = allSessions.stream()
            .filter(s -> !s.getStartTime().toLocalDate().isBefore(sevenDaysAgo) && !s.getStartTime().toLocalDate().isAfter(today))
            .collect(Collectors.toList());
            
        double totalHours = recentSessions.stream()
            .mapToLong(SessionRecord::getDurationSeconds)
            .sum() / 3600.0;
        sb.append("- Total Hours Tracked: ").append(String.format("%.2f hrs\n", totalHours));
        
        java.util.Set<LocalDate> activeDates = recentSessions.stream()
            .map(s -> s.getStartTime().toLocalDate())
            .collect(Collectors.toSet());
        sb.append("- Active Days: ").append(activeDates.size()).append(" / 7 days\n");
        
        GoalStreakStats streakStats = calculateGoalStreakStats();
        sb.append("- Current Streak: ").append(streakStats.currentStreak).append(" days\n");
        sb.append("- Max Streak: ").append(streakStats.maxStreak).append(" days\n");
        sb.append("- Today's Goal Progress: ").append(streakStats.todayMinutes).append(" / ").append(streakStats.dailyGoalMinutes).append(" mins (Goal Met: ").append(streakStats.todayGoalMet).append(")\n");
        
        // Calculate burnout risk & productivity using same formulas
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
        
        double burnoutScore = totalHours * 1.5;
        if (activeDates.size() > 0 && avgBreakRatio < 0.15) {
            burnoutScore += 20.0;
        }
        int riskPercent = (int) Math.max(0, Math.min(100, burnoutScore));
        sb.append("- Burnout Risk Score: ").append(riskPercent).append("%\n");
        
        double dailyGoalHrs = streakStats.dailyGoalMinutes / 60.0;
        double targetHours = dailyGoalHrs * 7.0;
        double goalAchievementRate = targetHours > 0 ? Math.min(1.0, totalHours / targetHours) : 1.0;
        double pacingFactor = 1.0 - (riskPercent / 200.0);
        int prodIndex = (int) (goalAchievementRate * pacingFactor * 100.0);
        sb.append("- Productivity Index: ").append(prodIndex).append(" / 100\n");
        
        long totalActiveSec = recentSessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();
        double avgAttentionSpanMin = 0;
        double avgFocusScore = 0;
        if (!recentSessions.isEmpty()) {
            long totalBlocks = recentSessions.stream().mapToLong(s -> getSessionPauseCount(s) + 1).sum();
            avgAttentionSpanMin = (totalActiveSec / (double) totalBlocks) / 60.0;
            avgFocusScore = recentSessions.stream().mapToInt(this::calculateSessionFocusScore).average().orElse(0.0);
        }
        sb.append("- Avg Attention Span: ").append(String.format("%.1f mins\n", avgAttentionSpanMin));
        sb.append("- Cognitive Focus Score: ").append(String.format("%.1f / 100\n", avgFocusScore));

        long recentSpanSec = 0;
        for (SessionRecord s : recentSessions) {
            long span = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toSeconds();
            recentSpanSec += span;
        }
        long recentMidSessionBreaks = Math.max(0, recentSpanSec - totalActiveSec);
        int recentXpGained = (int) Math.round(totalActiveSec / 60.0);
        int recentXpDeducted = (int) Math.round(recentMidSessionBreaks / 60.0 * 0.25);
        int recentNetXp = recentXpGained - recentXpDeducted;
        double avgRecentXp = activeDates.size() > 0 ? (double) recentNetXp / activeDates.size() : 0.0;
        sb.append("- 7-Day Net XP: ").append(String.format("%d XP (Gained: +%d, Deducted: -%d)\n", recentNetXp, recentXpGained, recentXpDeducted));
        sb.append("- Avg XP / Active Day: ").append(String.format("%.1f XP/day\n", avgRecentXp));
        
        // Subject Breakdown
        sb.append("\n### SUBJECT & CHAPTER BREAKDOWN (Last 7 Days):\n");
        java.util.Map<String, Long> subjectDurations = new java.util.HashMap<>();
        for (SessionRecord s : recentSessions) {
            String subj = s.getSubject();
            subjectDurations.put(subj, subjectDurations.getOrDefault(subj, 0L) + s.getDurationSeconds());
        }
        for (java.util.Map.Entry<String, Long> entry : subjectDurations.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(formatDuration(entry.getValue())).append("\n");
        }
        
        // Revision Topics
        sb.append("\n### REVISION TOPICS (Last 7 Days):\n");
        java.util.Map<String, Long> revisionDurations = new java.util.HashMap<>();
        for (SessionRecord s : recentSessions) {
            String desc = s.getDescription();
            if (desc.startsWith("Revision: ")) {
                String topic = desc.substring("Revision: ".length()).trim();
                if (topic.isEmpty()) topic = "General/Unnamed";
                revisionDurations.put(topic, revisionDurations.getOrDefault(topic, 0L) + s.getDurationSeconds());
            }
        }
        if (revisionDurations.isEmpty()) {
            sb.append("- No revision sessions tracked yet.\n");
        } else {
            for (java.util.Map.Entry<String, Long> entry : revisionDurations.entrySet()) {
                sb.append("- ").append(entry.getKey()).append(": ").append(formatDuration(entry.getValue())).append("\n");
            }
        }
        
        // Recent Session Details (Increase limit to 120)
        sb.append("\n### RECENT SESSION LOGS (Last 120 Records):\n");
        List<SessionRecord> sortedAll = new ArrayList<>(allSessions);
        sortedAll.sort((s1, s2) -> s2.getStartTime().compareTo(s1.getStartTime())); // newest first
        int limit = Math.min(120, sortedAll.size());
        for (int i = 0; i < limit; i++) {
            SessionRecord s = sortedAll.get(i);
            sb.append(String.format("- [%s] Subj: %s, Duration: %s, Type: %s, Desc: %s (Pauses: %d)\n",
                s.getStartTime().toLocalDate().toString(),
                s.getSubject(),
                formatDuration(s.getDurationSeconds()),
                s.getType().name(),
                s.getDescription(),
                s.getPauseCount()
            ));
        }
        
        sb.append("\nWhen answering, respond concisely using markdown formatting. High contrast or bullet points are encouraged. Do not refer to internal implementation details unless asked.");
        return sb.toString();
    }

    private void sendChatToAI() {
        String userInput = aiInputTextArea.getText().trim();
        if (userInput.isEmpty()) return;
        
        aiInputTextArea.setText("");
        aiInputTextArea.setEnabled(false);
        aiSendBtn.setEnabled(false);
        aiStatusLabel.setText("Thinking...");
        
        aiChatHistoryList.add(new ChatTurn("user", userInput));
        saveChatHistory();
        rebuildChatHtml();
        
        new Thread(() -> {
            try {
                String apiKey = storageService.loadOpenRouterApiKey();
                if (apiKey.isEmpty()) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        aiStatusLabel.setText("API key missing. Please reset API key.");
                        aiInputTextArea.setEnabled(true);
                        aiSendBtn.setEnabled(true);
                    });
                    return;
                }
                
                // Build payload
                String selectedModel = storageService.loadOpenRouterModel();
                StringBuilder json = new StringBuilder();
                json.append("{\n");
                json.append("  \"model\": \"").append(escapeJson(selectedModel)).append("\",\n");
                json.append("  \"messages\": [\n");
                
                String systemContext = compileAIPromptContext();
                json.append("    {\"role\": \"system\", \"content\": \"").append(escapeJson(systemContext)).append("\"}");
                
                for (ChatTurn turn : aiChatHistoryList) {
                    json.append(",\n");
                    json.append("    {\"role\": \"").append(escapeJson(turn.role)).append("\", \"content\": \"").append(escapeJson(turn.text)).append("\"}");
                }
                
                json.append("\n  ]\n");
                json.append("}");
                
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .build();
                    
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://openrouter.ai/api/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "http://localhost")
                    .header("X-Title", "Time Logger")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json.toString(), java.nio.charset.StandardCharsets.UTF_8))
                    .build();
                    
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    String aiText = parseOpenRouterResponseText(responseBody);
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        aiChatHistoryList.add(new ChatTurn("assistant", aiText));
                        saveChatHistory();
                        rebuildChatHtml();
                        aiStatusLabel.setText(" ");
                        aiInputTextArea.setEnabled(true);
                        aiSendBtn.setEnabled(true);
                        aiInputTextArea.requestFocusInWindow();
                    });
                } else {
                    String errBody = response.body();
                    String errMsg = "Error: API returned status " + response.statusCode();
                    if (errBody != null && !errBody.trim().isEmpty()) {
                        String parsedErr = parseOpenRouterResponseText(errBody);
                        if (parsedErr != null && !parsedErr.startsWith("Error:")) {
                            errMsg = parsedErr;
                        }
                    }
                    final String finalErrMsg = errMsg;
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        aiStatusLabel.setText(finalErrMsg);
                        aiInputTextArea.setEnabled(true);
                        aiSendBtn.setEnabled(true);
                    });
                }
            } catch (Exception ex) {
                String errMsg = "Connection error: " + ex.getMessage();
                if (ex instanceof java.net.http.HttpConnectTimeoutException || ex instanceof java.net.SocketTimeoutException) {
                    errMsg = "Connection timeout. Please check your internet connection.";
                }
                final String finalErrMsg = errMsg;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    aiStatusLabel.setText(finalErrMsg);
                    aiInputTextArea.setEnabled(true);
                    aiSendBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private String parseOpenRouterResponseText(String json) {
        if (json == null) return "";
        
        int choicesIdx = json.indexOf("\"choices\"");
        if (choicesIdx == -1) {
            int errorIdx = json.indexOf("\"error\"");
            if (errorIdx != -1) {
                int messageIdx = json.indexOf("\"message\"", errorIdx);
                if (messageIdx != -1) {
                    int contentStart = json.indexOf("\"", messageIdx + 9);
                    if (contentStart != -1) {
                        int contentEnd = json.indexOf("\"", contentStart + 1);
                        while (contentEnd != -1 && json.charAt(contentEnd - 1) == '\\') {
                            contentEnd = json.indexOf("\"", contentEnd + 1);
                        }
                        if (contentEnd != -1) {
                            String errMsg = unescapeJson(json.substring(contentStart + 1, contentEnd));
                            int rawIdx = json.indexOf("\"raw\"", errorIdx);
                            if (rawIdx != -1) {
                                int rawStart = json.indexOf("\"", rawIdx + 5);
                                if (rawStart != -1) {
                                    int rawEnd = json.indexOf("\"", rawStart + 1);
                                    while (rawEnd != -1 && json.charAt(rawEnd - 1) == '\\') {
                                        rawEnd = json.indexOf("\"", rawEnd + 1);
                                    }
                                    if (rawEnd != -1) {
                                        errMsg = unescapeJson(json.substring(rawStart + 1, rawEnd));
                                    }
                                }
                            }
                            return "API Error: " + errMsg;
                        }
                    }
                }
            }
            return "Error: Unexpected API response format.";
        }
        
        int contentIdx = json.indexOf("\"content\"", choicesIdx);
        if (contentIdx == -1) {
            return "Error: Could not find content in choices.";
        }
        
        int colonIdx = json.indexOf(":", contentIdx);
        if (colonIdx == -1) {
            return "Error: Invalid JSON response format.";
        }
        
        int valueStart = json.indexOf("\"", colonIdx);
        if (valueStart == -1) {
            return "Error: Invalid JSON value format.";
        }
        
        int valueEnd = json.indexOf("\"", valueStart + 1);
        while (valueEnd != -1 && json.charAt(valueEnd - 1) == '\\') {
            valueEnd = json.indexOf("\"", valueEnd + 1);
        }
        
        if (valueEnd == -1) {
            return "Error: Unterminated string in JSON response.";
        }
        
        String rawContent = json.substring(valueStart + 1, valueEnd);
        String unescaped = unescapeJson(rawContent);
        unescaped = unescaped.replaceAll("(?i)\\b(\\w+n)['\u2019](?:s|re|or)\\b", "$1't");
        return unescaped;
    }

    private String unescapeJson(String str) {
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
                            String hex = str.substring(i + 2, i + 6);
                            try {
                                char unicode = (char) Integer.parseInt(hex, 16);
                                sb.append(unicode);
                                i += 5;
                            } catch (NumberFormatException e) {
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

    private String escapeJson(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < ' ') {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    private void rebuildChatHtml() {
        ThemeManager.AppTheme theme = ThemeManager.loadTheme();
        ThemeManager.ThemeColors colors = ThemeManager.getColors(theme);
        
        if (aiChatLogPane != null) {
            aiChatLogPane.setBackground(colors.bg);
        }
        
        String hexBg = toHexString(colors.bg);
        String hexText = toHexString(colors.text);
        
        String userBubbleBg;
        String userBubbleText;
        String aiBubbleBg;
        String aiBubbleText;
        
        if (theme == ThemeManager.AppTheme.DARK) {
            userBubbleBg = "#1f58a6";
            userBubbleText = "#ffffff";
            aiBubbleBg = "#2d2d34";
            aiBubbleText = "#f0f0f5";
        } else if (theme == ThemeManager.AppTheme.HIGH_CONTRAST) {
            userBubbleBg = "#000000";
            userBubbleText = "#ffff00";
            aiBubbleBg = "#000000";
            aiBubbleText = "#ffffff";
        } else { // LIGHT
            userBubbleBg = "#e3f2fd"; // Soft blue bubble
            userBubbleText = "#0d47a1"; // Dark blue text
            aiBubbleBg = "#ffffff";
            aiBubbleText = "#212529";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='background-color: ").append(hexBg)
          .append("; color: ").append(hexText)
          .append("; font-family: sans-serif; font-size: 12px; line-height: 1.4; margin: 10px;'>");
          
        for (ChatTurn turn : aiChatHistoryList) {
            boolean isUser = "user".equals(turn.role);
            String align = isUser ? "right" : "left";
            String bubbleBg = isUser ? userBubbleBg : aiBubbleBg;
            String bubbleText = isUser ? userBubbleText : aiBubbleText;
            String borderStyle = (theme == ThemeManager.AppTheme.HIGH_CONTRAST) ? "border: 1px solid #ffffff;" : "border: 1px solid " + toHexString(colors.border) + ";";
            
            sb.append("<table width='100%' cellpadding='0' cellspacing='0' border='0' style='margin-bottom: 10px;'>")
              .append("<tr><td align='").append(align).append("'>");
              
            sb.append("<div style='background-color: ").append(bubbleBg)
              .append("; color: ").append(bubbleText)
              .append("; ").append(borderStyle)
              .append(" padding: 10px 14px; max-width: 80%; font-family: sans-serif;'>");
            
            if (isUser) {
                sb.append(escapeHtml(turn.text).replace("\n", "<br>"));
            } else {
                sb.append(markdownToHtml(turn.text));
            }
            
            sb.append("</div>");
            sb.append("</td></tr></table>");
        }
        
        sb.append("</body></html>");
        
        aiChatLogPane.setText(sb.toString());
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            aiChatLogPane.setCaretPosition(aiChatLogPane.getDocument().getLength());
        });
    }

    private String toHexString(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    private String markdownToHtml(String markdown) {
        if (markdown == null) return "";
        
        String[] lines = markdown.split("\n");
        StringBuilder html = new StringBuilder();
        boolean inList = false;
        boolean inOrderedList = false;
        boolean inCodeBlock = false;
        boolean inTable = false;
        
        ThemeManager.AppTheme theme = ThemeManager.loadTheme();
        String codeBg = (theme == ThemeManager.AppTheme.DARK) ? "#3a3a42" : ((theme == ThemeManager.AppTheme.HIGH_CONTRAST) ? "#000000" : "#e9ecef");
        String codeColor = (theme == ThemeManager.AppTheme.HIGH_CONTRAST) ? "#ffff00" : (theme == ThemeManager.AppTheme.DARK ? "#f0f0f5" : "#212529");
        String codeBorder = (theme == ThemeManager.AppTheme.HIGH_CONTRAST) ? "1px solid #ffffff" : "1px solid " + toHexString(ThemeManager.getColors(theme).border);
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Handle code blocks (e.g., ```java)
            if (trimmed.startsWith("```")) {
                if (inTable) { html.append("</table>"); inTable = false; }
                if (inCodeBlock) {
                    html.append("</div>");
                    inCodeBlock = false;
                } else {
                    html.append("<div style='background-color: ").append(codeBg)
                        .append("; color: ").append(codeColor)
                        .append("; border: ").append(codeBorder)
                        .append("; padding: 6px; font-family: monospace; white-space: pre; margin-top: 4px; margin-bottom: 4px;'>");
                    inCodeBlock = true;
                }
                continue;
            }
            
            if (inCodeBlock) {
                html.append(escapeHtml(line)).append("<br>");
                continue;
            }
            
            // Handle Markdown Tables
            boolean isTableRow = trimmed.startsWith("|") && trimmed.contains("|");
            if (isTableRow) {
                boolean isSeparator = trimmed.replaceAll("[\\s\\-:\\|]", "").isEmpty();
                if (isSeparator) {
                    if (!inTable) {
                        inTable = true;
                    }
                    continue;
                }
                
                if (inList) { html.append("</ul>"); inList = false; }
                if (inOrderedList) { html.append("</ol>"); inOrderedList = false; }
                
                String borderCol = toHexString(ThemeManager.getColors(theme).border);
                
                if (!inTable) {
                    String headerBg = (theme == ThemeManager.AppTheme.DARK) ? "#3a3a42" : ((theme == ThemeManager.AppTheme.HIGH_CONTRAST) ? "#000000" : "#f1f3f5");
                    html.append("<table border='1' cellpadding='4' cellspacing='0' style='border-collapse: collapse; border: 1px solid ")
                        .append(borderCol).append("; margin-top: 6px; margin-bottom: 6px; width: 100%; font-size: 11px;'>");
                    inTable = true;
                    
                    html.append("<tr style='background-color: ").append(headerBg).append(";'>");
                    String[] cells = splitTableRow(trimmed);
                    for (String cell : cells) {
                        html.append("<th align='left' style='border: 1px solid ").append(borderCol).append("; padding: 4px;'><b>")
                            .append(formatInlineMarkdown(cell, codeBg, codeColor))
                            .append("</b></th>");
                    }
                    html.append("</tr>");
                } else {
                    html.append("<tr>");
                    String[] cells = splitTableRow(trimmed);
                    for (String cell : cells) {
                        html.append("<td style='border: 1px solid ").append(borderCol).append("; padding: 4px;'>")
                            .append(formatInlineMarkdown(cell, codeBg, codeColor))
                            .append("</td>");
                    }
                    html.append("</tr>");
                }
                continue;
            } else {
                if (inTable) {
                    html.append("</table>");
                    inTable = false;
                }
            }
            
            boolean isBullet = trimmed.startsWith("- ") || trimmed.startsWith("* ");
            boolean isNumbered = trimmed.matches("^\\d+\\.\\s.*");
            
            if (isBullet) {
                if (!inList) {
                    if (inOrderedList) {
                        html.append("</ol>");
                        inOrderedList = false;
                    }
                    html.append("<ul style='margin-top: 2px; margin-bottom: 2px; padding-left: 20px;'>");
                    inList = true;
                }
                String content = trimmed.substring(2);
                html.append("<li style='margin-bottom: 2px;'>").append(formatInlineMarkdown(content, codeBg, codeColor)).append("</li>");
                continue;
            } else if (isNumbered) {
                if (!inOrderedList) {
                    if (inList) {
                        html.append("</ul>");
                        inList = false;
                    }
                    html.append("<ol style='margin-top: 2px; margin-bottom: 2px; padding-left: 20px;'>");
                    inOrderedList = true;
                }
                int dotIdx = trimmed.indexOf('.');
                String content = trimmed.substring(dotIdx + 1).trim();
                html.append("<li style='margin-bottom: 2px;'>").append(formatInlineMarkdown(content, codeBg, codeColor)).append("</li>");
                continue;
            } else {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                if (inOrderedList) {
                    html.append("</ol>");
                    inOrderedList = false;
                }
            }
            
            if (trimmed.startsWith("### ")) {
                html.append("<div style='font-size: 13px; font-weight: bold; margin-top: 8px; margin-bottom: 4px;'>")
                    .append(formatInlineMarkdown(trimmed.substring(4), codeBg, codeColor))
                    .append("</div>");
            } else if (trimmed.startsWith("## ")) {
                html.append("<div style='font-size: 14px; font-weight: bold; margin-top: 10px; margin-bottom: 4px;'>")
                    .append(formatInlineMarkdown(trimmed.substring(3), codeBg, codeColor))
                    .append("</div>");
            } else if (trimmed.startsWith("# ")) {
                html.append("<div style='font-size: 16px; font-weight: bold; margin-top: 12px; margin-bottom: 6px;'>")
                    .append(formatInlineMarkdown(trimmed.substring(2), codeBg, codeColor))
                    .append("</div>");
            } else if (trimmed.isEmpty()) {
                html.append("<div style='height: 4px;'></div>");
            } else {
                html.append("<div style='margin-bottom: 4px;'>")
                    .append(formatInlineMarkdown(line, codeBg, codeColor))
                    .append("</div>");
            }
        }
        
        if (inList) {
            html.append("</ul>");
        }
        if (inOrderedList) {
            html.append("</ol>");
        }
        if (inCodeBlock) {
            html.append("</div>");
        }
        if (inTable) {
            html.append("</table>");
        }
        
        return html.toString();
    }

    private String[] splitTableRow(String row) {
        if (row.startsWith("|")) {
            row = row.substring(1);
        }
        if (row.endsWith("|")) {
            row = row.substring(0, row.length() - 1);
        }
        String[] parts = row.split("\\|");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private String formatInlineMarkdown(String text, String codeBg, String codeColor) {
        String escaped = escapeHtml(text);
        
        // Restore <br> tags so they render as actual line breaks!
        escaped = escaped.replaceAll("(?i)&lt;br\\s*/?&gt;", "<br>");
        // Unescape escaped pipes used in markdown table cells
        escaped = escaped.replace("\\|", "|");
        
        escaped = escaped.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        escaped = escaped.replaceAll("`(.*?)`", "<font face='monospace' color='" + codeColor + "' style='background-color: " + codeBg + ";'> $1 </font>");
        return escaped;
    }

    private void saveChatHistory() {
        java.nio.file.Path file = java.nio.file.Paths.get(System.getProperty("user.home"), ".timelogger_chat_history");
        try {
            List<String> lines = new ArrayList<>();
            for (ChatTurn turn : aiChatHistoryList) {
                String encodedText = java.util.Base64.getEncoder().encodeToString(turn.text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                lines.add(turn.role + ":" + encodedText);
            }
            java.nio.file.Files.write(file, lines, java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private List<ChatTurn> loadChatHistory() {
        List<ChatTurn> history = new ArrayList<>();
        java.nio.file.Path file = java.nio.file.Paths.get(System.getProperty("user.home"), ".timelogger_chat_history");
        try {
            if (!java.nio.file.Files.exists(file)) {
                return history;
            }
            List<String> lines = java.nio.file.Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8);
            for (String line : lines) {
                int colonIdx = line.indexOf(':');
                if (colonIdx != -1) {
                    String role = line.substring(0, colonIdx);
                    String encodedText = line.substring(colonIdx + 1);
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(encodedText);
                    String text = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                    if ("assistant".equals(role)) {
                        text = text.replaceAll("(?i)\\b(\\w+n)['\u2019](?:s|re|or)\\b", "$1't");
                    }
                    history.add(new ChatTurn(role, text));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return history;
    }

    private void refreshQuestionTopicsDropdown() {
        String qType = (String) stopwatchQuestionTypeCombo.getSelectedItem();
        if (qType == null) {
            qType = "DPP Questions";
        }
        String subject = (String) stopwatchSubjectCombo.getSelectedItem();
        if (subject == null) {
            subject = "";
        }
        List<String> topics = storageService.loadQuestionTopics(qType, subject);
        
        // Remove selection listener temporarily to avoid trigger during model set
        java.awt.event.ActionListener[] listeners = stopwatchQuestionDescCombo.getActionListeners();
        for (java.awt.event.ActionListener l : listeners) {
            stopwatchQuestionDescCombo.removeActionListener(l);
        }
        
        stopwatchQuestionDescCombo.removeAllItems();
        for (String topic : topics) {
            stopwatchQuestionDescCombo.addItem(topic);
        }
        stopwatchQuestionDescCombo.addItem("[Add Custom...]");
        stopwatchQuestionDescCombo.setSelectedIndex(-1);
        
        // Re-add selection listeners
        for (java.awt.event.ActionListener l : listeners) {
            stopwatchQuestionDescCombo.addActionListener(l);
        }
    }

    private void saveCurrentQuestionTopics() {
        String qType = (String) stopwatchQuestionTypeCombo.getSelectedItem();
        if (qType == null) return;
        String subject = (String) stopwatchSubjectCombo.getSelectedItem();
        if (subject == null) return;
        List<String> topics = new ArrayList<>();
        for (int i = 0; i < stopwatchQuestionDescCombo.getItemCount(); i++) {
            String item = stopwatchQuestionDescCombo.getItemAt(i);
            if (item != null && !"[Add Custom...]".equals(item)) {
                topics.add(item);
            }
        }
        storageService.saveQuestionTopics(qType, subject, topics);
    }

    private void revertQuestionDescSelection(String val) {
        if (val == null) {
            stopwatchQuestionDescCombo.setSelectedIndex(-1);
        } else {
            stopwatchQuestionDescCombo.setSelectedItem(val);
        }
    }

    public void selectOrAddQuestionDesc(String desc) {
        if (desc == null || desc.trim().isEmpty()) {
            stopwatchQuestionDescCombo.setSelectedIndex(-1);
            return;
        }
        desc = desc.trim();
        boolean found = false;
        for (int i = 0; i < stopwatchQuestionDescCombo.getItemCount(); i++) {
            if (desc.equalsIgnoreCase(stopwatchQuestionDescCombo.getItemAt(i))) {
                stopwatchQuestionDescCombo.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if (!found) {
            int count = stopwatchQuestionDescCombo.getItemCount();
            if (count > 0) {
                stopwatchQuestionDescCombo.insertItemAt(desc, count - 1);
                stopwatchQuestionDescCombo.setSelectedItem(desc);
                saveCurrentQuestionTopics();
            } else {
                stopwatchQuestionDescCombo.addItem(desc);
                stopwatchQuestionDescCombo.setSelectedItem(desc);
            }
        }
    }

    private void updateCalculatorVisibility() {
        String activityType = (String) stopwatchActivityTypeCombo.getSelectedItem();
        String qType = (String) stopwatchQuestionTypeCombo.getSelectedItem();
        boolean shouldShow = "Questions".equals(activityType) && 
                            ("Practice Book Questions".equals(qType) || "Previous Year Questions".equals(qType));
        
        if (stopwatchCalcBtn != null) {
            stopwatchCalcBtn.setVisible(shouldShow);
        }

        if (shouldShow) {
            if (scientificCalculator == null) {
                scientificCalculator = new ScientificCalculator(this);
            }
            if (!scientificCalculator.isVisible()) {
                scientificCalculator.setVisible(true);
            }
        } else {
            if (scientificCalculator != null && scientificCalculator.isVisible()) {
                scientificCalculator.setVisible(false);
            }
        }
    }
}

