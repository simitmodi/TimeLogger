package com.timelogger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Apple Reminders inspired dashboard panel for TimeLogger.
 * Features:
 * - Smart Top Cards: Today, Scheduled, All, Flagged, Completed
 * - Grouping by Day Periods: Morning (9:00 AM), Afternoon (2:00 PM), Evening (7:00 PM), Any Time
 * - No lists: Strictly general reminders with direct Subject affiliation and day-period assignment
 * - Smooth Fluent/Cupertino circular checkbox toggle
 * - In-place creation bar and detail modal
 */
public class RemindersPanel extends JPanel {

    public enum FilterView {
        TODAY("Today", new Color(0, 122, 255)),
        SCHEDULED("Scheduled", new Color(255, 59, 48)),
        ALL("All", new Color(110, 115, 128)),
        FLAGGED("Flagged", new Color(255, 149, 0)),
        COMPLETED("Completed", new Color(52, 199, 89));

        private final String title;
        private final Color accent;

        FilterView(String title, Color accent) {
            this.title = title;
            this.accent = accent;
        }

        public String getTitle() {
            return title;
        }

        public Color getAccent() {
            return accent;
        }
    }

    private final StorageService storageService;
    private final ReminderService reminderService;
    private FilterView currentFilter = FilterView.TODAY;

    // UI Components
    private final JPanel cardsGrid = new JPanel(new GridLayout(1, 5, 12, 0));
    private final JPanel listContainer = new JPanel();
    private final JLabel headerTitleLabel = new JLabel("Today");
    private final JLabel headerCountLabel = new JLabel("0 Reminders");

    // Top Card Badges
    private final JLabel todayCountBadge = new JLabel("0", SwingConstants.RIGHT);
    private final JLabel scheduledCountBadge = new JLabel("0", SwingConstants.RIGHT);
    private final JLabel allCountBadge = new JLabel("0", SwingConstants.RIGHT);
    private final JLabel flaggedCountBadge = new JLabel("0", SwingConstants.RIGHT);
    private final JLabel completedCountBadge = new JLabel("0", SwingConstants.RIGHT);

    private final List<ModernCardPanel> filterCardPanels = new ArrayList<>();

    public RemindersPanel(StorageService storageService, ReminderService reminderService) {
        this.storageService = storageService;
        this.reminderService = reminderService;
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        initUI();
        reminderService.addListener(items -> SwingUtilities.invokeLater(this::refreshView));
    }

    private void initUI() {
        // --- 1. Top Section: Apple-Style Smart Metric Cards ---
        cardsGrid.setOpaque(false);
        cardsGrid.setPreferredSize(new Dimension(800, 88));

        addFilterCard(FilterView.TODAY, todayCountBadge);
        addFilterCard(FilterView.SCHEDULED, scheduledCountBadge);
        addFilterCard(FilterView.ALL, allCountBadge);
        addFilterCard(FilterView.FLAGGED, flaggedCountBadge);
        addFilterCard(FilterView.COMPLETED, completedCountBadge);

        JPanel topContainer = new JPanel(new BorderLayout(0, 14));
        topContainer.setOpaque(false);
        topContainer.add(cardsGrid, BorderLayout.NORTH);

        // --- 2. Action Bar & Add Button ---
        JPanel actionRow = new JPanel(new BorderLayout(12, 0));
        actionRow.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleBox.setOpaque(false);
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        headerCountLabel.setForeground(new Color(130, 135, 145));
        titleBox.add(headerTitleLabel);
        titleBox.add(headerCountLabel);
        actionRow.add(titleBox, BorderLayout.WEST);

        ModernButton newReminderBtn = new ModernButton("+ New Reminder", ModernButton.ButtonStyle.PRIMARY);
        newReminderBtn.setPreferredSize(new Dimension(150, 36));
        newReminderBtn.addActionListener(e -> showAddReminderDialog());
        actionRow.add(newReminderBtn, BorderLayout.EAST);

        topContainer.add(actionRow, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // --- 3. Center Section: Grouped Reminders by Day Periods ---
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void addFilterCard(FilterView view, JLabel countLabel) {
        ModernCardPanel card = new ModernCardPanel(10);
        card.setLayout(new BorderLayout(6, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Top row: Vector Icon Badge and Count Badge
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JComponent iconBadge = createFilterIconBadge(view);
        top.add(iconBadge, BorderLayout.WEST);

        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        countLabel.setForeground(view.getAccent());
        top.add(countLabel, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Bottom row: Title
        JLabel titleLbl = new JLabel(view.getTitle());
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(new Color(100, 105, 115));
        card.add(titleLbl, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentFilter = view;
                updateCardSelections();
                refreshView();
            }
        });

        filterCardPanels.add(card);
        cardsGrid.add(card);
    }

    private JComponent createFilterIconBadge(FilterView view) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(28, 28));
                setMinimumSize(new Dimension(28, 28));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 26;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // Round icon background
                g2.setColor(view.getAccent());
                g2.fillOval(x, y, size, size);

                // White glyph
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = x + size / 2;
                int cy = y + size / 2;

                switch (view) {
                    case TODAY:
                        // Calendar icon with day number
                        g2.drawRoundRect(cx - 6, cy - 6, 12, 12, 3, 3);
                        g2.drawLine(cx - 6, cy - 2, cx + 6, cy - 2);
                        g2.fillRect(cx - 2, cy + 1, 4, 3);
                        break;
                    case SCHEDULED:
                        // Clock / Calendar schedule glyph
                        g2.drawOval(cx - 6, cy - 6, 12, 12);
                        g2.drawLine(cx, cy, cx, cy - 4);
                        g2.drawLine(cx, cy, cx + 3, cy);
                        break;
                    case ALL:
                        // Tray / list icon
                        g2.drawRoundRect(cx - 6, cy - 5, 12, 10, 2, 2);
                        g2.drawLine(cx - 3, cy - 1, cx + 3, cy - 1);
                        g2.drawLine(cx - 3, cy + 2, cx + 3, cy + 2);
                        break;
                    case FLAGGED:
                        // Pennant flag
                        g2.drawLine(cx - 4, cy - 6, cx - 4, cy + 6);
                        int[] fx = {cx - 4, cx + 5, cx - 4};
                        int[] fy = {cy - 6, cy - 2, cy + 2};
                        g2.fillPolygon(fx, fy, 3);
                        break;
                    case COMPLETED:
                        // Checkmark
                        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
                        g2.drawLine(cx - 1, cy + 4, cx + 5, cy - 4);
                        break;
                }

                g2.dispose();
            }
        };
    }

    private void updateCardSelections() {
        for (int i = 0; i < FilterView.values().length; i++) {
            FilterView v = FilterView.values()[i];
            ModernCardPanel p = filterCardPanels.get(i);
            if (v == currentFilter) {
                p.setCustomBorder(v.getAccent());
            } else {
                p.setCustomBorder(null);
            }
        }
        cardsGrid.repaint();
    }

    public void refreshView() {
        List<ReminderItem> all = reminderService.getReminders();
        LocalDate today = LocalDate.now();

        // Calculate counts
        long todayCount = all.stream().filter(r -> !r.isCompleted() && (r.isDueToday() || (r.getDueDate() != null && r.getDueDate().isBefore(today)))).count();
        long schedCount = all.stream().filter(r -> !r.isCompleted() && r.getDueDate() != null).count();
        long allCount = all.stream().filter(r -> !r.isCompleted()).count();
        long flagCount = all.stream().filter(r -> !r.isCompleted() && r.isFlagged()).count();
        long compCount = all.stream().filter(ReminderItem::isCompleted).count();

        todayCountBadge.setText(String.valueOf(todayCount));
        scheduledCountBadge.setText(String.valueOf(schedCount));
        allCountBadge.setText(String.valueOf(allCount));
        flaggedCountBadge.setText(String.valueOf(flagCount));
        completedCountBadge.setText(String.valueOf(compCount));

        // Filter items
        List<ReminderItem> filtered;
        switch (currentFilter) {
            case TODAY:
                filtered = all.stream().filter(r -> !r.isCompleted() && (r.isDueToday() || (r.getDueDate() != null && r.getDueDate().isBefore(today)))).collect(Collectors.toList());
                break;
            case SCHEDULED:
                filtered = all.stream().filter(r -> !r.isCompleted() && r.getDueDate() != null).collect(Collectors.toList());
                break;
            case FLAGGED:
                filtered = all.stream().filter(r -> !r.isCompleted() && r.isFlagged()).collect(Collectors.toList());
                break;
            case COMPLETED:
                filtered = all.stream().filter(ReminderItem::isCompleted).collect(Collectors.toList());
                break;
            case ALL:
            default:
                filtered = all.stream().filter(r -> !r.isCompleted()).collect(Collectors.toList());
                break;
        }

        headerTitleLabel.setText(currentFilter.getTitle());
        headerTitleLabel.setForeground(currentFilter.getAccent());
        headerCountLabel.setText(filtered.size() + (filtered.size() == 1 ? " Reminder" : " Reminders"));

        updateCardSelections();
        renderGroupedReminders(filtered);
    }

    private void renderGroupedReminders(List<ReminderItem> items) {
        listContainer.removeAll();

        if (items.isEmpty() && currentFilter != FilterView.SCHEDULED) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            emptyPanel.setOpaque(false);
            emptyPanel.setBorder(new EmptyBorder(50, 20, 50, 20));

            JLabel emptyIcon = new JLabel("✨", SwingConstants.CENTER);
            emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));

            JLabel emptyText = new JLabel("No Reminders in " + currentFilter.getTitle(), SwingConstants.CENTER);
            emptyText.setFont(new Font("Segoe UI", Font.BOLD, 16));
            emptyText.setForeground(new Color(130, 135, 145));

            JLabel subText = new JLabel("Tap '+ New Reminder' above to stay on track with your study sessions.", SwingConstants.CENTER);
            subText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            subText.setForeground(new Color(160, 165, 175));

            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setOpaque(false);
            emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyText.setAlignmentX(Component.CENTER_ALIGNMENT);
            subText.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(emptyIcon);
            box.add(Box.createVerticalStrut(10));
            box.add(emptyText);
            box.add(Box.createVerticalStrut(4));
            box.add(subText);

            emptyPanel.add(box);
            listContainer.add(emptyPanel);
            listContainer.revalidate();
            listContainer.repaint();
            return;
        }

        // Apple Reminders layout:
        // 1. TODAY view: grouped by Day Periods (Morning, Afternoon, Evening, Night, Any Time)
        // 2. SCHEDULED view: grouped by Apple timeline headers (Today, Tomorrow, Day-by-Day upcoming days, Rest of Month, Future Months)
        // 3. ALL / FLAGGED / COMPLETED view: chronological / period grouped
        if (currentFilter == FilterView.TODAY) {
            renderPeriodSection("Morning (09:00 AM)", ReminderItem.TimePeriod.MORNING, filterByPeriod(items, ReminderItem.TimePeriod.MORNING));
            renderPeriodSection("Afternoon (02:00 PM)", ReminderItem.TimePeriod.AFTERNOON, filterByPeriod(items, ReminderItem.TimePeriod.AFTERNOON));
            renderPeriodSection("Evening (07:00 PM)", ReminderItem.TimePeriod.EVENING, filterByPeriod(items, ReminderItem.TimePeriod.EVENING));
            renderPeriodSection("Night (09:30 PM)", ReminderItem.TimePeriod.NIGHT, filterByPeriod(items, ReminderItem.TimePeriod.NIGHT));
            renderPeriodSection("Any Time / All Day", ReminderItem.TimePeriod.ALL_DAY, filterByPeriod(items, ReminderItem.TimePeriod.ALL_DAY));
        } else if (currentFilter == FilterView.SCHEDULED) {
            renderScheduledTimeline(items);
        } else {
            // ALL, FLAGGED, COMPLETED: Group by date then render
            renderScheduledTimeline(items);
        }

        listContainer.add(Box.createVerticalGlue());
        listContainer.revalidate();
        listContainer.repaint();
    }

    private void renderScheduledTimeline(List<ReminderItem> items) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusDays(7);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // Sort chronologically
        List<ReminderItem> sorted = items.stream().sorted((a, b) -> {
            LocalDate da = a.getDueDate() != null ? a.getDueDate() : LocalDate.MIN;
            LocalDate db = b.getDueDate() != null ? b.getDueDate() : LocalDate.MIN;
            int dateCmp = da.compareTo(db);
            if (dateCmp != 0) return dateCmp;
            LocalTime ta = a.getDueTime() != null ? a.getDueTime() : (a.getPeriod() != null ? a.getPeriod().getDefaultTime() : LocalTime.MIN);
            LocalTime tb = b.getDueTime() != null ? b.getDueTime() : (b.getPeriod() != null ? b.getPeriod().getDefaultTime() : LocalTime.MIN);
            if (ta == null) ta = LocalTime.MIN;
            if (tb == null) tb = LocalTime.MIN;
            return ta.compareTo(tb);
        }).collect(Collectors.toList());

        // Group into Apple Scheduled sections
        List<ReminderItem> overdueItems = new ArrayList<>();
        List<ReminderItem> todayItems = new ArrayList<>();
        List<ReminderItem> tomorrowItems = new ArrayList<>();
        java.util.Map<LocalDate, List<ReminderItem>> dayByDayItems = new java.util.LinkedHashMap<>();
        List<ReminderItem> restOfMonthItems = new ArrayList<>();
        java.util.Map<java.time.YearMonth, List<ReminderItem>> futureMonthsItems = new java.util.LinkedHashMap<>();

        for (ReminderItem r : sorted) {
            LocalDate d = r.getDueDate();
            if (d == null) {
                todayItems.add(r);
                continue;
            }
            if (d.isBefore(today)) {
                overdueItems.add(r);
            } else if (d.isEqual(today)) {
                todayItems.add(r);
            } else if (d.isEqual(tomorrow)) {
                tomorrowItems.add(r);
            } else if (d.isBefore(nextWeek)) {
                dayByDayItems.computeIfAbsent(d, k -> new ArrayList<>()).add(r);
            } else if (d.getYear() == today.getYear() && d.getMonth() == today.getMonth()) {
                restOfMonthItems.add(r);
            } else {
                java.time.YearMonth ym = java.time.YearMonth.from(d);
                futureMonthsItems.computeIfAbsent(ym, k -> new ArrayList<>()).add(r);
            }
        }

        // Render each section in exact Apple Reminders order (always present even if empty, matching Apple Reminders)
        if (!overdueItems.isEmpty()) {
            renderDateSection("Overdue", overdueItems, new Color(255, 69, 58));
        }

        // 1. Today
        renderDateSection("Today", todayItems, new Color(255, 255, 255));

        // 2. Tomorrow
        renderDateSection("Tomorrow", tomorrowItems, new Color(255, 255, 255));

        // 3. Upcoming individual days (next 5 days: Thu 15 Oct, Fri 16 Oct, ...)
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE d MMM");
        for (int i = 2; i <= 6; i++) {
            LocalDate d = today.plusDays(i);
            List<ReminderItem> dayList = dayByDayItems.getOrDefault(d, java.util.Collections.emptyList());
            renderDateSection(d.format(dayFmt), dayList, new Color(255, 255, 255));
        }

        // 4. Rest of current month
        String curMonthName = today.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
        renderDateSection("Rest of " + curMonthName, restOfMonthItems, new Color(255, 255, 255));

        // 5. Future months (next 8 months: November, December, January 2027, ...)
        for (int m = 1; m <= 8; m++) {
            java.time.YearMonth ym = java.time.YearMonth.from(today).plusMonths(m);
            List<ReminderItem> mList = futureMonthsItems.getOrDefault(ym, java.util.Collections.emptyList());
            String label;
            if (ym.getYear() == today.getYear()) {
                label = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
            } else {
                label = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + ym.getYear();
            }
            renderDateSection(label, mList, new Color(255, 255, 255));
        }
    }

    private void renderDateSection(String title, List<ReminderItem> sectionItems, Color headerColor) {
        if (sectionItems.isEmpty() && currentFilter != FilterView.SCHEDULED) {
            return;
        }

        JPanel section = new JPanel(new BorderLayout(0, 4)) {
            @Override
            public Dimension getMaximumSize() {
                Dimension pref = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, pref.height);
            }
        };
        section.setOpaque(false);
        section.setBorder(new EmptyBorder(4, 0, 8, 0));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Apple section header with count
        String headerTitleText = sectionItems.isEmpty() ? title : title + " (" + sectionItems.size() + ")";
        JLabel sectionTitle = new JLabel(headerTitleText);
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        ThemeManager.ThemeColors colors = ThemeManager.getColors(ThemeManager.loadTheme());
        boolean isDark = (ThemeManager.loadTheme() == ThemeManager.AppTheme.DARK 
                       || ThemeManager.loadTheme() == ThemeManager.AppTheme.HIGH_CONTRAST);
        
        if (title.contains("Overdue")) {
            sectionTitle.setForeground(new Color(255, 69, 58));
        } else {
            sectionTitle.setForeground(isDark ? Color.WHITE : new Color(30, 32, 38));
        }
        sectionTitle.setBorder(new EmptyBorder(4, 4, 6, 4));
        section.add(sectionTitle, BorderLayout.NORTH);

        ModernCardPanel cardWrap = new ModernCardPanel(10);
        cardWrap.setLayout(new BoxLayout(cardWrap, BoxLayout.Y_AXIS));
        cardWrap.setBorder(new EmptyBorder(2, 8, 2, 8));
        cardWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (sectionItems.isEmpty()) {
            // Apple style dotted empty circle placeholder
            JPanel emptyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            emptyRow.setOpaque(false);
            emptyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JButton emptyDot = createDottedCircle();
            emptyDot.setToolTipText("No reminders scheduled for " + title);
            emptyRow.add(emptyDot);
            cardWrap.add(emptyRow);
        } else {
            for (int i = 0; i < sectionItems.size(); i++) {
                ReminderItem item = sectionItems.get(i);
                cardWrap.add(createReminderRow(item));
                if (i < sectionItems.size() - 1) {
                    cardWrap.add(createDivider());
                }
            }
        }

        section.add(cardWrap, BorderLayout.CENTER);
        listContainer.add(section);
        listContainer.add(Box.createVerticalStrut(4));
    }

    private JButton createDottedCircle() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 18;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(new Color(120, 125, 135, 120));
                // Dotted dashed stroke
                float[] dash = {2.0f, 2.5f};
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                g2.drawOval(x, y, size - 1, size - 1);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(26, 26));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private List<ReminderItem> filterByPeriod(List<ReminderItem> items, ReminderItem.TimePeriod period) {
        return items.stream()
                .filter(r -> r.getPeriod() == period)
                .sorted(Comparator.comparingInt((ReminderItem r) -> -r.getPriority().getLevel()))
                .collect(Collectors.toList());
    }

    private void renderPeriodSection(String headerText, ReminderItem.TimePeriod period, List<ReminderItem> periodItems) {
        if (periodItems.isEmpty()) return;

        JPanel section = new JPanel(new BorderLayout(0, 4)) {
            @Override
            public Dimension getMaximumSize() {
                Dimension pref = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, pref.height);
            }
        };
        section.setOpaque(false);
        section.setBorder(new EmptyBorder(8, 0, 12, 0));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Period Section Header with Vector Period Icon
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent periodIcon = createPeriodVectorIcon(period);
        headerRow.add(periodIcon);

        JLabel sectionTitle = new JLabel(headerText + " (" + periodItems.size() + ")");
        sectionTitle.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
        sectionTitle.setForeground(new Color(90, 95, 105));
        sectionTitle.setBorder(new EmptyBorder(2, 2, 2, 4));
        headerRow.add(sectionTitle);

        section.add(headerRow, BorderLayout.NORTH);

        ModernCardPanel cardWrap = new ModernCardPanel(10);
        cardWrap.setLayout(new BoxLayout(cardWrap, BoxLayout.Y_AXIS));
        cardWrap.setBorder(new EmptyBorder(4, 8, 4, 8));
        cardWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int i = 0; i < periodItems.size(); i++) {
            ReminderItem item = periodItems.get(i);
            cardWrap.add(createReminderRow(item));
            if (i < periodItems.size() - 1) {
                cardWrap.add(createDivider());
            }
        }

        section.add(cardWrap, BorderLayout.CENTER);
        listContainer.add(section);
        listContainer.add(Box.createVerticalStrut(8));
    }

    private JComponent createPeriodVectorIcon(ReminderItem.TimePeriod period) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(18, 18));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                switch (period) {
                    case MORNING: // Sun with rays
                        g2.setColor(new Color(255, 179, 0));
                        g2.fillOval(cx - 4, cy - 4, 8, 8);
                        g2.setStroke(new BasicStroke(1.2f));
                        for (int i = 0; i < 8; i++) {
                            double angle = i * Math.PI / 4.0;
                            int x1 = (int) (cx + 6 * Math.cos(angle));
                            int y1 = (int) (cy + 6 * Math.sin(angle));
                            int x2 = (int) (cx + 8 * Math.cos(angle));
                            int y2 = (int) (cy + 8 * Math.sin(angle));
                            g2.drawLine(x1, y1, x2, y2);
                        }
                        break;
                    case AFTERNOON: // High bright sun
                        g2.setColor(new Color(255, 149, 0));
                        g2.fillOval(cx - 5, cy - 5, 10, 10);
                        break;
                    case EVENING: // Crescent moon
                        g2.setColor(new Color(90, 120, 200));
                        g2.fillOval(cx - 5, cy - 5, 10, 10);
                        g2.setColor(getBackground() != null && getBackground().getAlpha() > 0 ? getBackground() : new Color(245, 247, 250));
                        g2.fillOval(cx - 2, cy - 7, 9, 9);
                        break;
                    case NIGHT: // Deep night star
                        g2.setColor(new Color(120, 100, 210));
                        int[] sx = {cx, cx + 2, cx + 6, cx + 3, cx + 4, cx, cx - 4, cx - 3, cx - 6, cx - 2};
                        int[] sy = {cy - 6, cy - 2, cy - 2, cy + 1, cy + 6, cy + 3, cy + 6, cy + 1, cy - 2, cy - 2};
                        g2.fillPolygon(sx, sy, 10);
                        break;
                    case ALL_DAY:
                    default: // Clipboard / list
                        g2.setColor(new Color(130, 135, 145));
                        g2.drawRoundRect(cx - 5, cy - 5, 10, 11, 2, 2);
                        g2.drawLine(cx - 2, cy - 5, cx + 2, cy - 5);
                        g2.drawLine(cx - 3, cy - 1, cx + 3, cy - 1);
                        g2.drawLine(cx - 3, cy + 2, cx + 3, cy + 2);
                        break;
                }
                g2.dispose();
            }
        };
    }

    private void renderPlainSection(List<ReminderItem> items) {
        ModernCardPanel cardWrap = new ModernCardPanel(10);
        cardWrap.setLayout(new BoxLayout(cardWrap, BoxLayout.Y_AXIS));
        cardWrap.setBorder(new EmptyBorder(4, 8, 4, 8));
        cardWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int i = 0; i < items.size(); i++) {
            ReminderItem item = items.get(i);
            cardWrap.add(createReminderRow(item));
            if (i < items.size() - 1) {
                cardWrap.add(createDivider());
            }
        }
        listContainer.add(cardWrap);
    }

    private Component createDivider() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(225, 230, 238));
        return sep;
    }

    private JPanel createReminderRow(ReminderItem item) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 8, 10, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        // Circular Apple Checkbox
        JButton checkBtn = createAppleCircleCheck(item.isCompleted(), item.getPriority());
        checkBtn.addActionListener(e -> reminderService.toggleCompleted(item.getId()));
        row.add(checkBtn, BorderLayout.WEST);

        // Center Content
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Title Row
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLbl = new JLabel(item.getTitle());
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        if (item.isCompleted()) {
            titleLbl.setForeground(new Color(150, 155, 165));
            titleLbl.setText("<html><strike>" + escapeHtml(item.getTitle()) + "</strike></html>");
        }
        titleRow.add(titleLbl);

        if (item.getPriority() != ReminderItem.Priority.NONE) {
            JLabel prioLbl = new JLabel(item.getPriority().getBadge());
            prioLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            prioLbl.setForeground(new Color(235, 75, 60));
            titleRow.add(prioLbl);
        }

        if (item.isFlagged()) {
            JComponent flagBadge = new JComponent() {
                {
                    setPreferredSize(new Dimension(14, 14));
                }
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 149, 0));
                    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(2, 1, 2, 13);
                    int[] fx = {2, 11, 2};
                    int[] fy = {1, 5, 9};
                    g2.fillPolygon(fx, fy, 3);
                    g2.dispose();
                }
            };
            titleRow.add(flagBadge);
        }

        center.add(titleRow);

        // Details Subtitle: Subject pill, Date & Time, Notes
        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        subRow.setOpaque(false);
        subRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Subject Badge
        if (item.getSubject() != null && !item.getSubject().isEmpty()) {
            JLabel subBadge = new JLabel(item.getSubject());
            subBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            subBadge.setForeground(new Color(0, 102, 204));
            subBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 210, 245), 1, true),
                    new EmptyBorder(1, 5, 1, 5)
            ));
            subRow.add(subBadge);
        }

        // Time / Date info
        StringBuilder dateStr = new StringBuilder();
        if (item.getDueDate() != null) {
            if (item.isDueToday()) {
                dateStr.append("Today");
            } else if (item.getDueDate().isEqual(LocalDate.now().plusDays(1))) {
                dateStr.append("Tomorrow");
            } else {
                dateStr.append(item.getDueDate().format(DateTimeFormatter.ofPattern("MMM d")));
            }
        }
        if (item.getDueTime() != null) {
            dateStr.append(" at ").append(item.getDueTime().format(DateTimeFormatter.ofPattern("hh:mm a")));
        } else if (item.getPeriod() != null) {
            dateStr.append(" • ").append(item.getPeriod().getDisplayName());
        }

        JLabel timeLbl = new JLabel(dateStr.toString());
        timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        if (item.isOverdue() && !item.isCompleted()) {
            timeLbl.setForeground(new Color(220, 50, 40)); // Red for overdue
            timeLbl.setText("• Overdue • " + timeLbl.getText());
        } else {
            timeLbl.setForeground(new Color(130, 135, 145));
        }
        subRow.add(timeLbl);

        if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
            JLabel notesLbl = new JLabel(item.getNotes());
            notesLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            notesLbl.setForeground(new Color(140, 145, 155));
            subRow.add(notesLbl);
        }

        center.add(subRow);
        row.add(center, BorderLayout.CENTER);

        // Right Actions (Flag, Edit, Delete) - Crisp vector buttons with consistent spacing
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);

        JButton flagBtn = createIconButton(IconType.FLAG, item.isFlagged() ? new Color(255, 149, 0) : new Color(160, 165, 175));
        flagBtn.setToolTipText(item.isFlagged() ? "Unflag reminder" : "Flag reminder");
        flagBtn.addActionListener(e -> {
            item.setFlagged(!item.isFlagged());
            reminderService.updateReminder(item);
        });

        JButton editBtn = createIconButton(IconType.EDIT, new Color(120, 125, 135));
        editBtn.setToolTipText("Edit Reminder");
        editBtn.addActionListener(e -> showEditReminderDialog(item));

        JButton deleteBtn = createIconButton(IconType.DELETE, new Color(220, 60, 50));
        deleteBtn.setToolTipText("Delete Reminder");
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete reminder \"" + item.getTitle() + "\"?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                reminderService.deleteReminder(item.getId());
            }
        });

        actions.add(flagBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);
        row.add(actions, BorderLayout.EAST);

        return row;
    }

    private enum IconType { FLAG, EDIT, DELETE }

    private JButton createIconButton(IconType type, Color iconColor) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int cx = w / 2;
                int cy = h / 2;

                if (getModel().isRollover()) {
                    g2.setColor(new Color(150, 155, 165, 40));
                    g2.fillRoundRect(2, 2, w - 4, h - 4, 6, 6);
                }

                g2.setColor(iconColor);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                switch (type) {
                    case FLAG:
                        g2.drawLine(cx - 4, cy - 6, cx - 4, cy + 6);
                        int[] fx = {cx - 4, cx + 5, cx - 4};
                        int[] fy = {cy - 6, cy - 2, cy + 2};
                        g2.fillPolygon(fx, fy, 3);
                        break;
                    case EDIT:
                        // Crisp pencil glyph
                        g2.drawLine(cx - 4, cy + 4, cx + 3, cy - 3);
                        g2.drawLine(cx + 3, cy - 3, cx + 5, cy - 1);
                        g2.drawLine(cx + 5, cy - 1, cx - 2, cy + 6);
                        g2.drawLine(cx - 4, cy + 4, cx - 2, cy + 6);
                        g2.drawLine(cx - 5, cy + 5, cx - 4, cy + 4);
                        break;
                    case DELETE:
                        // Crisp trash can glyph
                        g2.drawLine(cx - 5, cy - 4, cx + 5, cy - 4); // lid
                        g2.drawLine(cx - 2, cy - 6, cx + 2, cy - 6); // handle
                        g2.drawRoundRect(cx - 4, cy - 3, 8, 9, 2, 2); // can
                        g2.drawLine(cx - 1, cy - 1, cx - 1, cy + 3);
                        g2.drawLine(cx + 1, cy - 1, cx + 1, cy + 3);
                        break;
                }
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(28, 28));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createAppleCircleCheck(boolean completed, ReminderItem.Priority priority) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 20;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                if (completed) {
                    g2.setColor(new Color(52, 199, 89)); // Apple Success Green
                    g2.fillOval(x, y, size, size);

                    // Checkmark
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 5, y + 10, x + 9, y + 14);
                    g2.drawLine(x + 9, y + 14, x + 15, y + 6);
                } else {
                    Color ringColor = (priority == ReminderItem.Priority.HIGH) ? new Color(255, 59, 48) :
                            (priority == ReminderItem.Priority.MEDIUM) ? new Color(255, 149, 0) : new Color(190, 195, 205);
                    g2.setColor(ringColor);
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawOval(x, y, size - 1, size - 1);
                }
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(30, 30));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showAddReminderDialog() {
        showReminderEditorModal(null);
    }

    private void showEditReminderDialog(ReminderItem item) {
        showReminderEditorModal(item);
    }

    private void showReminderEditorModal(ReminderItem existing) {
        boolean isEdit = (existing != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "Edit Reminder" : "New Reminder", true);
        dialog.setLayout(new BorderLayout(0, 12));
        dialog.setSize(520, 560);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(20, 24, 16, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 0);

        // 1. Title Input
        JLabel titleLbl = new JLabel("Title");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextField titleField = new JTextField(isEdit ? existing.getTitle() : "");
        titleField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleField.setPreferredSize(new Dimension(460, 34));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        content.add(titleLbl, gbc);
        gbc.gridy = 1;
        content.add(titleField, gbc);

        // 2. Notes Input
        JLabel notesLbl = new JLabel("Notes / Details (Optional)");
        notesLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextArea notesArea = new JTextArea(isEdit ? existing.getNotes() : "", 3, 20);
        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setPreferredSize(new Dimension(460, 68));

        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 4, 0);
        content.add(notesLbl, gbc);
        gbc.gridy = 3;
        gbc.insets = new Insets(2, 0, 4, 0);
        content.add(notesScroll, gbc);

        // 3. Subject Selector
        JLabel subLbl = new JLabel("Subject");
        subLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        List<String> subjects = new ArrayList<>(storageService.loadSubjects());
        if (!subjects.contains("General")) {
            subjects.add(0, "General");
        }
        JComboBox<String> subjectCombo = new JComboBox<>(subjects.toArray(new String[0]));
        subjectCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subjectCombo.setPreferredSize(new Dimension(460, 34));
        if (isEdit && existing.getSubject() != null) {
            subjectCombo.setSelectedItem(existing.getSubject());
        }

        gbc.gridy = 4;
        gbc.insets = new Insets(10, 0, 4, 0);
        content.add(subLbl, gbc);
        gbc.gridy = 5;
        gbc.insets = new Insets(2, 0, 4, 0);
        content.add(subjectCombo, gbc);

        // 4. Date & Time Selection (Side by side columns)
        JLabel dateLbl = new JLabel("Due Date");
        dateLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel timeLbl = new JLabel("Due Time (30-min interval)");
        timeLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));

        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(10, 0, 4, 8);
        gbc.gridx = 0; gbc.gridy = 6;
        content.add(dateLbl, gbc);

        gbc.insets = new Insets(10, 8, 4, 0);
        gbc.gridx = 1; gbc.gridy = 6;
        content.add(timeLbl, gbc);

        // Date Button with vector calendar icon
        LocalDate initialDate = isEdit && existing.getDueDate() != null ? existing.getDueDate() : LocalDate.now();
        final LocalDate[] chosenDate = new LocalDate[]{ initialDate };
        DateTimeFormatter dateBtnFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd (EEE)");

        JButton dateBtn = new JButton(chosenDate[0].format(dateBtnFmt));
        dateBtn.setIcon(createCalendarIcon(16, 16));
        dateBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateBtn.setFocusPainted(false);
        dateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dateBtn.setHorizontalAlignment(SwingConstants.LEFT);
        dateBtn.setIconTextGap(8);
        dateBtn.setPreferredSize(new Dimension(220, 34));

        dateBtn.addActionListener(e -> {
            JDialog calDialog = new JDialog(dialog, "Select Date", true);
            calDialog.setLayout(new BorderLayout());
            calDialog.setResizable(false);

            DatePickerCalendar calPanel = new DatePickerCalendar(chosenDate[0], sel -> {
                chosenDate[0] = sel;
                dateBtn.setText(sel.format(dateBtnFmt));
                calDialog.dispose();
            });
            calDialog.add(calPanel, BorderLayout.CENTER);
            calDialog.pack();
            calDialog.setLocationRelativeTo(dateBtn);
            calDialog.setVisible(true);
        });

        // 30-minute interval editable time combobox
        List<String> timeSlots = new ArrayList<>();
        DateTimeFormatter timeSlotFmt = DateTimeFormatter.ofPattern("hh:mm a");
        for (int h = 0; h < 24; h++) {
            timeSlots.add(LocalTime.of(h, 0).format(timeSlotFmt));
            timeSlots.add(LocalTime.of(h, 30).format(timeSlotFmt));
        }

        JComboBox<String> timeCombo = new JComboBox<>(timeSlots.toArray(new String[0]));
        timeCombo.setEditable(true);
        timeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        timeCombo.setPreferredSize(new Dimension(220, 34));

        LocalTime initialTime = isEdit && existing.getDueTime() != null ? existing.getDueTime() : LocalTime.of(9, 0);
        timeCombo.setSelectedItem(initialTime.format(timeSlotFmt));

        gbc.insets = new Insets(2, 0, 4, 8);
        gbc.gridx = 0; gbc.gridy = 7;
        content.add(dateBtn, gbc);

        gbc.insets = new Insets(2, 8, 4, 0);
        gbc.gridx = 1; gbc.gridy = 7;
        content.add(timeCombo, gbc);

        // 5. Priority and Flag Row
        JLabel prioLbl = new JLabel("Priority");
        prioLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel flagTitleLbl = new JLabel("Flag Status");
        flagTitleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));

        gbc.insets = new Insets(10, 0, 4, 8);
        gbc.gridx = 0; gbc.gridy = 8;
        content.add(prioLbl, gbc);

        gbc.insets = new Insets(10, 8, 4, 0);
        gbc.gridx = 1; gbc.gridy = 8;
        content.add(flagTitleLbl, gbc);

        JComboBox<ReminderItem.Priority> prioCombo = new JComboBox<>(ReminderItem.Priority.values());
        prioCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        prioCombo.setPreferredSize(new Dimension(220, 34));
        if (isEdit) {
            prioCombo.setSelectedItem(existing.getPriority());
        }

        JCheckBox flagCheck = new JCheckBox("Flagged");
        flagCheck.setFont(new Font("Segoe UI", Font.BOLD, 13));
        flagCheck.setIcon(createCheckboxFlagIcon(false));
        flagCheck.setSelectedIcon(createCheckboxFlagIcon(true));
        flagCheck.setIconTextGap(6);
        flagCheck.setPreferredSize(new Dimension(220, 34));
        if (isEdit) {
            flagCheck.setSelected(existing.isFlagged());
        }

        gbc.insets = new Insets(2, 0, 4, 8);
        gbc.gridx = 0; gbc.gridy = 9;
        content.add(prioCombo, gbc);

        gbc.insets = new Insets(2, 8, 4, 0);
        gbc.gridx = 1; gbc.gridy = 9;
        content.add(flagCheck, gbc);

        dialog.add(content, BorderLayout.CENTER);

        // Buttons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.ButtonStyle.STANDARD);
        ModernButton saveBtn = new ModernButton(isEdit ? "Save Changes" : "Create Reminder", ModernButton.ButtonStyle.PRIMARY);

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a reminder title.", "Missing Title", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDate targetDate = chosenDate[0];

            // Parse time from editable combobox
            LocalTime targetTime;
            String timeText = (timeCombo.getSelectedItem() != null) ? timeCombo.getSelectedItem().toString().trim() : "";
            targetTime = parseTimeInput(timeText);

            ReminderItem.TimePeriod period = ReminderItem.TimePeriod.fromTime(targetTime);
            String subject = (String) subjectCombo.getSelectedItem();
            ReminderItem.Priority priority = (ReminderItem.Priority) prioCombo.getSelectedItem();
            boolean isFlagged = flagCheck.isSelected();

            if (isEdit) {
                existing.setTitle(title);
                existing.setNotes(notesArea.getText().trim());
                existing.setDueDate(targetDate);
                existing.setDueTime(targetTime);
                existing.setPeriod(period);
                existing.setSubject(subject);
                existing.setPriority(priority);
                existing.setFlagged(isFlagged);
                existing.setNotified(false);
                reminderService.updateReminder(existing);
            } else {
                ReminderItem newItem = new ReminderItem(title, targetDate, period, subject);
                newItem.setDueTime(targetTime);
                newItem.setNotes(notesArea.getText().trim());
                newItem.setPriority(priority);
                newItem.setFlagged(isFlagged);
                reminderService.addReminder(newItem);
            }
            dialog.dispose();
        });

        btnBar.add(cancelBtn);
        btnBar.add(saveBtn);
        dialog.add(btnBar, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private LocalTime parseTimeInput(String text) {
        if (text == null || text.trim().isEmpty()) {
            return LocalTime.of(9, 0);
        }
        text = text.trim();
        // Try multiple standard formats: "hh:mm a", "h:mm a", "HH:mm", "H:mm", "hh:mma", "h:mma"
        String[] patterns = {"hh:mm a", "h:mm a", "hh:mma", "h:mma", "HH:mm", "H:mm", "h a", "ha"};
        for (String pattern : patterns) {
            try {
                DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern(pattern)
                        .toFormatter(java.util.Locale.ENGLISH);
                return LocalTime.parse(text, fmt);
            } catch (Exception ignored) {
            }
        }
        // Fallback: try manual extraction of digits and am/pm
        try {
            boolean isPm = text.toLowerCase().contains("pm");
            boolean isAm = text.toLowerCase().contains("am");
            String cleaned = text.replaceAll("[^0-9:]", "");
            if (cleaned.contains(":")) {
                String[] parts = cleaned.split(":");
                int hour = Integer.parseInt(parts[0]);
                int min = Integer.parseInt(parts[1]);
                if (isPm && hour < 12) hour += 12;
                if (isAm && hour == 12) hour = 0;
                return LocalTime.of(Math.max(0, Math.min(23, hour)), Math.max(0, Math.min(59, min)));
            } else if (!cleaned.isEmpty()) {
                int hour = Integer.parseInt(cleaned);
                if (isPm && hour < 12) hour += 12;
                if (isAm && hour == 12) hour = 0;
                return LocalTime.of(Math.max(0, Math.min(23, hour)), 0);
            }
        } catch (Exception ignored) {
        }
        return LocalTime.of(9, 0);
    }

    private Icon createCalendarIcon(int width, int height) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 122, 255));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Calendar body
                g2.drawRoundRect(x + 1, y + 2, width - 2, height - 3, 3, 3);
                // Top bar line
                g2.drawLine(x + 1, y + 6, x + width - 1, y + 6);
                // Binder hooks
                g2.drawLine(x + 4, y + 1, x + 4, y + 3);
                g2.drawLine(x + width - 5, y + 1, x + width - 5, y + 3);
                // Inner day mark
                g2.fillRect(x + width / 2 - 2, y + 8, 4, 3);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return width;
            }

            @Override
            public int getIconHeight() {
                return height;
            }
        };
    }

    private Icon createCheckboxFlagIcon(boolean selected) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int boxSize = 14;
                int bx = x;
                int by = y + 1;

                // Checkbox outline
                if (selected) {
                    g2.setColor(new Color(255, 149, 0));
                    g2.fillRoundRect(bx, by, boxSize, boxSize, 4, 4);

                    // Pennant flag inside
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(bx + 3, by + 2, bx + 3, by + 11);
                    int[] fx = {bx + 3, bx + 10, bx + 3};
                    int[] fy = {by + 2, by + 5, by + 8};
                    g2.fillPolygon(fx, fy, 3);
                } else {
                    g2.setColor(new Color(190, 195, 205));
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawRoundRect(bx, by, boxSize, boxSize, 4, 4);

                    // Gray subtle flag outline
                    g2.setColor(new Color(170, 175, 185));
                    g2.drawLine(bx + 3, by + 2, bx + 3, by + 11);
                    int[] fx = {bx + 3, bx + 10, bx + 3};
                    int[] fy = {by + 2, by + 5, by + 8};
                    g2.drawPolygon(fx, fy, 3);
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 18;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Compact, elegant calendar date picker panel.
     */
    private static class DatePickerCalendar extends JPanel {
        private LocalDate selectedDate;
        private java.time.YearMonth displayedMonth;
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel daysGrid = new JPanel(new java.awt.GridLayout(0, 7, 3, 3));
        private final java.util.function.Consumer<LocalDate> onDateSelected;

        public DatePickerCalendar(LocalDate initialDate, java.util.function.Consumer<LocalDate> onDateSelected) {
            this.selectedDate = (initialDate != null) ? initialDate : LocalDate.now();
            this.displayedMonth = java.time.YearMonth.from(this.selectedDate);
            this.onDateSelected = onDateSelected;

            setLayout(new BorderLayout(8, 8));
            setBorder(new EmptyBorder(12, 14, 12, 14));
            setPreferredSize(new Dimension(300, 260));

            // Header navigation: [<] Month Year [>]
            JPanel header = new JPanel(new BorderLayout(6, 0));
            header.setOpaque(false);

            JButton prevBtn = new JButton("<");
            prevBtn.setPreferredSize(new Dimension(36, 26));
            prevBtn.setFocusPainted(false);
            prevBtn.setMargin(new Insets(0, 0, 0, 0));
            prevBtn.addActionListener(e -> {
                displayedMonth = displayedMonth.minusMonths(1);
                updateCalendar();
            });

            JButton nextBtn = new JButton(">");
            nextBtn.setPreferredSize(new Dimension(36, 26));
            nextBtn.setFocusPainted(false);
            nextBtn.setMargin(new Insets(0, 0, 0, 0));
            nextBtn.addActionListener(e -> {
                displayedMonth = displayedMonth.plusMonths(1);
                updateCalendar();
            });

            monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            header.add(prevBtn, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(nextBtn, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            daysGrid.setOpaque(false);
            add(daysGrid, BorderLayout.CENTER);

            // Bottom bar: Today shortcut
            JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
            bottomBar.setOpaque(false);
            JButton todayBtn = new JButton("Select Today");
            todayBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            todayBtn.addActionListener(e -> {
                selectedDate = LocalDate.now();
                displayedMonth = java.time.YearMonth.from(selectedDate);
                updateCalendar();
                if (onDateSelected != null) {
                    onDateSelected.accept(selectedDate);
                }
            });
            bottomBar.add(todayBtn);
            add(bottomBar, BorderLayout.SOUTH);

            updateCalendar();
        }

        private void updateCalendar() {
            daysGrid.removeAll();

            String mName = displayedMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
            monthLabel.setText(mName + " " + displayedMonth.getYear());

            String[] colHeaders = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
            for (String h : colHeaders) {
                JLabel lbl = new JLabel(h, SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                lbl.setForeground(new Color(120, 125, 135));
                daysGrid.add(lbl);
            }

            LocalDate firstOfMonth = displayedMonth.atDay(1);
            int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
            for (int i = 1; i < dayOfWeek; i++) {
                daysGrid.add(new JLabel(""));
            }

            int daysInMonth = displayedMonth.lengthOfMonth();
            LocalDate today = LocalDate.now();

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = displayedMonth.atDay(day);
                JButton dayBtn = new JButton(String.valueOf(day));
                dayBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                dayBtn.setMargin(new Insets(2, 2, 2, 2));
                dayBtn.setFocusPainted(false);
                dayBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                boolean isSelected = date.equals(selectedDate);
                boolean isToday = date.equals(today);

                if (isSelected) {
                    dayBtn.setBackground(new Color(0, 122, 255));
                    dayBtn.setForeground(Color.WHITE);
                } else if (isToday) {
                    dayBtn.setBackground(new Color(230, 240, 255));
                    dayBtn.setForeground(new Color(0, 102, 204));
                    dayBtn.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255), 1));
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
}
