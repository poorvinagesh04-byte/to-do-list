package com.securetodo.scheduler;

import com.securetodo.entity.Reminder;
import com.securetodo.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private final ReminderService reminderService;

    public ReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * Periodically checks database for due reminders.
     * Persistent recovery: Any reminders that matured during server downtime are automatically
     * recovered and processed here without losing events.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.reminder-check-interval-ms:30000}")
    public void processDueReminders() {
        try {
            List<Reminder> dueReminders = reminderService.getDueReminders();
            if (!dueReminders.isEmpty()) {
                log.info("Processing {} due reminders", dueReminders.size());
                for (Reminder reminder : dueReminders) {
                    try {
                        log.info("Triggering reminder [ID: {}, User: {}, Task: '{}']",
                                reminder.getId(),
                                reminder.getUser().getEmail(),
                                reminder.getTask().getTitle());

                        reminderService.markSent(reminder);
                    } catch (Exception ex) {
                        log.error("Failed to process reminder ID {}: {}", reminder.getId(), ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in reminder scheduler: {}", e.getMessage());
        }
    }
}

