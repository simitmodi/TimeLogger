package com.timelogger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Background notification service and state manager for Apple-inspired reminders.
 * Runs a periodic checker to trigger native Windows desktop toasts and system tray notifications
 * when reminder alerts are due.
 */
public class ReminderService {
    private final StorageService storageService;
    private final List<ReminderItem> reminders = new CopyOnWriteArrayList<>();
    private final List<Consumer<List<ReminderItem>>> listeners = new CopyOnWriteArrayList<>();
    private javax.swing.Timer checkTimer;
    private Consumer<ReminderItem> notificationTrigger;

    public ReminderService(StorageService storageService) {
        this.storageService = storageService;
        reload();
        startScheduler();
    }

    public synchronized void reload() {
        reminders.clear();
        reminders.addAll(storageService.loadReminders());
        notifyListeners();
    }

    public void setNotificationTrigger(Consumer<ReminderItem> trigger) {
        this.notificationTrigger = trigger;
    }

    public void addListener(Consumer<List<ReminderItem>> listener) {
        listeners.add(listener);
        listener.accept(new ArrayList<>(reminders));
    }

    public void removeListener(Consumer<List<ReminderItem>> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        List<ReminderItem> snapshot = new ArrayList<>(reminders);
        for (Consumer<List<ReminderItem>> listener : listeners) {
            try {
                listener.accept(snapshot);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<ReminderItem> getReminders() {
        return new ArrayList<>(reminders);
    }

    public synchronized void addReminder(ReminderItem item) {
        if (item == null) return;
        reminders.add(0, item);
        storageService.saveReminders(new ArrayList<>(reminders));
        notifyListeners();
    }

    public synchronized void updateReminder(ReminderItem item) {
        if (item == null) return;
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getId().equals(item.getId())) {
                reminders.set(i, item);
                break;
            }
        }
        storageService.saveReminders(new ArrayList<>(reminders));
        notifyListeners();
    }

    public synchronized void deleteReminder(String id) {
        if (id == null) return;
        reminders.removeIf(r -> r.getId().equals(id));
        storageService.saveReminders(new ArrayList<>(reminders));
        notifyListeners();
    }

    public synchronized void toggleCompleted(String id) {
        if (id == null) return;
        for (ReminderItem item : reminders) {
            if (item.getId().equals(id)) {
                item.setCompleted(!item.isCompleted());
                break;
            }
        }
        storageService.saveReminders(new ArrayList<>(reminders));
        notifyListeners();
    }

    private void startScheduler() {
        // Check reminders every 15 seconds
        checkTimer = new javax.swing.Timer(15000, e -> checkDueReminders());
        checkTimer.setRepeats(true);
        checkTimer.start();
    }

    public void stopScheduler() {
        if (checkTimer != null) {
            checkTimer.stop();
        }
    }

    public synchronized void checkDueReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        boolean changed = false;

        for (ReminderItem item : reminders) {
            if (item.isCompleted() || item.isNotified()) {
                continue;
            }

            if (item.getDueDate() == null) {
                continue;
            }

            boolean isDue = false;
            // Overdue from a past date
            if (item.getDueDate().isBefore(today)) {
                isDue = true;
            } else if (item.getDueDate().isEqual(today)) {
                // If specific time is set, check if reached
                if (item.getDueTime() != null) {
                    if (!now.isBefore(item.getDueTime())) {
                        isDue = true;
                    }
                } else if (item.getPeriod() != null && item.getPeriod().getDefaultTime() != null) {
                    if (!now.isBefore(item.getPeriod().getDefaultTime())) {
                        isDue = true;
                    }
                }
            }

            if (isDue) {
                item.setNotified(true);
                changed = true;
                if (notificationTrigger != null) {
                    try {
                        notificationTrigger.accept(item);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        if (changed) {
            storageService.saveReminders(new ArrayList<>(reminders));
            notifyListeners();
        }
    }
}
