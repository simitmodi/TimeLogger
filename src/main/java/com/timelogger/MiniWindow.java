package com.timelogger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;

public class MiniWindow extends JFrame {
    private final AppFrame parent;
    private final SessionRecord.SessionType mode;
    
    private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel timeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
    
    final JComboBox<String> miniSubjectCombo = new JComboBox<>();
    final JComboBox<String> miniActivityTypeCombo = new JComboBox<>(new String[]{"General", "Questions", "Lecture"});
    final JPanel miniActivitySubPanel = new JPanel(new java.awt.CardLayout());
    final JTextField miniActivityField = new JTextField(15);
    final JComboBox<String> miniQuestionTypeCombo = new JComboBox<>(new String[]{
        "DPP Questions", "Practice Book Questions", "Previous Year Questions"
    });
    final JTextField miniQuestionDescField = new JTextField(10);
    final JTextField miniChapterField = new JTextField(4);
    final JTextField miniLectureField = new JTextField(4);

    private final ModernButton startBtn = new ModernButton("Start");
    private final ModernButton pauseBtn = new ModernButton("Pause");
    private final ModernButton stopBtn = new ModernButton("Stop & Log");
    private final ModernButton resetBtn = new ModernButton("Reset");
    private final ModernButton restoreBtn = new ModernButton("Maximize");

    public MiniWindow(AppFrame parent, SessionRecord.SessionType mode) {
        this.parent = parent;
        this.mode = mode;
        
        setTitle("Time Logger (Mini)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(true);
        if (mode == SessionRecord.SessionType.STOPWATCH) {
            setSize(500, 260);
        } else {
            setSize(380, 180);
        }
        setLocationRelativeTo(parent);
        
        // Window close listener to restore full app
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                restoreToFull();
            }
        });

        // Set window icon same as parent
        setIconImage(parent.getIconImage());

        // Initialize combo box model
        miniSubjectCombo.setModel(new javax.swing.DefaultComboBoxModel<>(
            parent.getSubjectsSortedByUsage().toArray(new String[0])
        ));

        initComponents();
        syncFromParent();
        updateState();
        
        ThemeManager.applyTheme(this, ThemeManager.getColors(ThemeManager.loadTheme()));
    }

    private void syncFromParent() {
        if (mode == SessionRecord.SessionType.STOPWATCH) {
            miniSubjectCombo.setSelectedItem(parent.stopwatchSubjectCombo.getSelectedItem());
            miniActivityTypeCombo.setSelectedItem(parent.stopwatchActivityTypeCombo.getSelectedItem());
            miniActivityField.setText(parent.stopwatchActivityField.getText());
            miniQuestionTypeCombo.setSelectedItem(parent.stopwatchQuestionTypeCombo.getSelectedItem());
            miniQuestionDescField.setText(parent.stopwatchQuestionDescField.getText());
            miniChapterField.setText(parent.stopwatchChapterField.getText());
            miniLectureField.setText(parent.stopwatchLectureField.getText());
        } else {
            miniSubjectCombo.setSelectedItem(parent.timerSubjectCombo.getSelectedItem());
        }
    }

    private void syncToParent() {
        if (mode == SessionRecord.SessionType.STOPWATCH) {
            parent.stopwatchSubjectCombo.setSelectedItem(miniSubjectCombo.getSelectedItem());
            parent.stopwatchActivityTypeCombo.setSelectedItem(miniActivityTypeCombo.getSelectedItem());
            parent.stopwatchActivityField.setText(miniActivityField.getText());
            parent.stopwatchQuestionTypeCombo.setSelectedItem(miniQuestionTypeCombo.getSelectedItem());
            parent.stopwatchQuestionDescField.setText(miniQuestionDescField.getText());
            parent.stopwatchChapterField.setText(miniChapterField.getText());
            parent.stopwatchLectureField.setText(miniLectureField.getText());
        } else {
            parent.timerSubjectCombo.setSelectedItem(miniSubjectCombo.getSelectedItem());
        }
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Configuration layout
        JPanel topConfig = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        topConfig.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1;
        topConfig.add(miniSubjectCombo, gbc);

        if (mode == SessionRecord.SessionType.STOPWATCH) {
            gbc.gridx = 2;
            topConfig.add(new JLabel("Type:"), gbc);
            gbc.gridx = 3;
            topConfig.add(miniActivityTypeCombo, gbc);

            // Card Layout for details
            JPanel generalCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            generalCard.add(new JLabel("Desc:"));
            generalCard.add(miniActivityField);

            JPanel questionsCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            questionsCard.add(new JLabel("QType:"));
            questionsCard.add(miniQuestionTypeCombo);
            questionsCard.add(new JLabel("Desc:"));
            questionsCard.add(miniQuestionDescField);

            JPanel lectureCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            lectureCard.add(new JLabel("Ch:"));
            lectureCard.add(miniChapterField);
            lectureCard.add(new JLabel("Lec:"));
            lectureCard.add(miniLectureField);

            miniActivitySubPanel.add(generalCard, "General");
            miniActivitySubPanel.add(questionsCard, "Questions");
            miniActivitySubPanel.add(lectureCard, "Lecture");

            java.awt.CardLayout cardLayout = (java.awt.CardLayout) miniActivitySubPanel.getLayout();
            miniActivityTypeCombo.addActionListener(e -> {
                String selected = (String) miniActivityTypeCombo.getSelectedItem();
                cardLayout.show(miniActivitySubPanel, selected);
            });

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 4;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            topConfig.add(miniActivitySubPanel, gbc);
        }

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 32));

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        centerPanel.add(statusLabel);
        centerPanel.add(timeLabel);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        
        startBtn.addActionListener(e -> {
            syncToParent();
            if (mode == SessionRecord.SessionType.STOPWATCH) {
                parent.startStopwatch();
            } else {
                parent.startTimer();
            }
            updateState();
        });

        pauseBtn.addActionListener(e -> {
            if (mode == SessionRecord.SessionType.STOPWATCH) {
                parent.togglePauseStopwatch();
            } else {
                parent.togglePauseTimer();
            }
            updateState();
        });

        stopBtn.addActionListener(e -> {
            syncToParent();
            if (mode == SessionRecord.SessionType.STOPWATCH) {
                parent.stopAndLogStopwatch();
            } else {
                parent.stopTimer();
            }
            updateState();
        });

        restoreBtn.addActionListener(e -> restoreToFull());

        resetBtn.addActionListener(e -> {
            if (mode == SessionRecord.SessionType.STOPWATCH) {
                parent.resetStopwatch();
            } else {
                parent.resetTimer();
            }
            updateState();
        });

        // Mnemonics
        startBtn.setMnemonic('S');
        pauseBtn.setMnemonic('P');
        resetBtn.setMnemonic('R');
        restoreBtn.setMnemonic('M');
        if (mode == SessionRecord.SessionType.STOPWATCH) {
            stopBtn.setMnemonic('L');
        } else {
            stopBtn.setMnemonic('T');
        }

        // Key bindings for the root pane
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "toggleStartPause");
        getRootPane().getActionMap().put("toggleStartPause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == SessionRecord.SessionType.STOPWATCH) {
                    if (parent.isStopwatchRunning()) {
                        parent.togglePauseStopwatch();
                    } else {
                        syncToParent();
                        parent.startStopwatch();
                    }
                } else {
                    if (parent.isTimerRunning()) {
                        parent.togglePauseTimer();
                    } else {
                        syncToParent();
                        parent.startTimer();
                    }
                }
                updateState();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "stopAndLog");
        getRootPane().getActionMap().put("stopAndLog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncToParent();
                if (mode == SessionRecord.SessionType.STOPWATCH) {
                    if (parent.isStopwatchStarted()) {
                        parent.stopAndLogStopwatch();
                    }
                } else {
                    if (parent.isTimerStarted()) {
                        parent.stopTimer();
                    }
                }
                updateState();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "maximize");
        getRootPane().getActionMap().put("maximize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                restoreToFull();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt R"), "resetSession");
        getRootPane().getActionMap().put("resetSession", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == SessionRecord.SessionType.STOPWATCH) {
                    parent.resetStopwatch();
                } else {
                    parent.resetTimer();
                }
                updateState();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F1"), "showHelp");
        getRootPane().getActionMap().put("showHelp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.showShortcutsHelp();
            }
        });

        controls.add(startBtn);
        controls.add(pauseBtn);
        controls.add(stopBtn);
        controls.add(resetBtn);
        controls.add(restoreBtn);

        panel.add(topConfig, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.SOUTH);

        add(panel);
    }

    public void updateTime(String timeStr) {
        timeLabel.setText(timeStr);
    }

    private void refreshSubjects() {
        boolean started = (mode == SessionRecord.SessionType.STOPWATCH) ? parent.isStopwatchStarted() : parent.isTimerStarted();
        if (!started) {
            String previous = (String) miniSubjectCombo.getSelectedItem();
            miniSubjectCombo.setModel(new javax.swing.DefaultComboBoxModel<>(
                parent.getSubjectsSortedByUsage().toArray(new String[0])
            ));
            if (previous != null) {
                miniSubjectCombo.setSelectedItem(previous);
            }
        }
    }

    public void updateState() {
        refreshSubjects();
        if (mode == SessionRecord.SessionType.STOPWATCH) {
            String subject = (String) miniSubjectCombo.getSelectedItem();
            statusLabel.setText("Stopwatch: " + (subject != null ? subject : "None"));
            
            boolean started = parent.isStopwatchStarted();
            boolean running = parent.isStopwatchRunning();
            
            startBtn.setEnabled(!running);
            pauseBtn.setEnabled(started);
            pauseBtn.setText(running ? "Pause" : "Resume");
            stopBtn.setEnabled(started);
            stopBtn.setText("Stop & Log");
            resetBtn.setEnabled(started);
            
            miniSubjectCombo.setEnabled(!started);
            miniActivityTypeCombo.setEnabled(!started);
            miniActivityField.setEnabled(!started);
            miniQuestionTypeCombo.setEnabled(!started);
            miniQuestionDescField.setEnabled(!started);
            miniChapterField.setEnabled(!started);
            miniLectureField.setEnabled(!started);
        } else {
            String subject = (String) miniSubjectCombo.getSelectedItem();
            statusLabel.setText("Timer: " + (subject != null ? subject : "None"));
            
            boolean started = parent.isTimerStarted();
            boolean running = parent.isTimerRunning();
            
            startBtn.setEnabled(!running);
            pauseBtn.setEnabled(started);
            pauseBtn.setText(running ? "Pause" : "Resume");
            stopBtn.setEnabled(started);
            stopBtn.setText("Stop");
            resetBtn.setEnabled(started);
            
            miniSubjectCombo.setEnabled(!started);
        }
    }

    private void restoreToFull() {
        syncToParent();
        parent.closeMiniWindow();
    }
}
