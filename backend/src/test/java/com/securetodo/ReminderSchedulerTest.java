package com.securetodo;

import com.securetodo.entity.*;
import com.securetodo.repository.ReminderRepository;
import com.securetodo.repository.TaskRepository;
import com.securetodo.repository.UserRepository;
import com.securetodo.scheduler.ReminderScheduler;
import com.securetodo.service.ReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ReminderSchedulerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderScheduler reminderScheduler;

    @Test
    @Transactional
    @DisplayName("Should create persistent reminders, prevent duplicates, and process due reminders")
    public void testPersistentRemindersAndScheduler() {
        // Create user
        User user = new User("Rem User", "rem_test_" + System.currentTimeMillis() + "@example.com", "hash", "UTC");
        user = userRepository.save(user);

        // Create task
        Task task = new Task();
        task.setUser(user);
        task.setTitle("Annual System Audit");
        task.setScheduledDate(LocalDate.now().plusDays(2));
        task.setStartTime(LocalTime.of(10, 0));
        task.setPriority(Priority.HIGH);
        task.setStatus(TaskStatus.PENDING);
        task = taskRepository.save(task);

        // Schedule reminders (at time, 15m before)
        reminderService.createOrUpdateRemindersForTask(task, List.of(0, 15));

        List<Reminder> reminders = reminderRepository.findByTaskId(task.getId());
        assertEquals(2, reminders.size(), "Should have exactly 2 persistent reminders");

        // Test deduplication: calling again with same offsets should not duplicate records
        reminderService.createOrUpdateRemindersForTask(task, List.of(0, 15));
        reminders = reminderRepository.findByTaskId(task.getId());
        assertEquals(2, reminders.size(), "Should prevent duplicate reminder records via deduplication keys");

        // Manually adjust one reminder to past due (simulating time passing or restart recovery)
        Reminder dueReminder = reminders.get(0);
        dueReminder.setReminderDatetime(LocalDateTime.now().minusMinutes(5));
        reminderRepository.save(dueReminder);

        // Run scheduler tick
        reminderScheduler.processDueReminders();

        // Verify reminder is now marked SENT
        Reminder refreshed = reminderRepository.findById(dueReminder.getId()).orElseThrow();
        assertEquals(ReminderStatus.SENT, refreshed.getStatus());
        assertNotNull(refreshed.getSentAt());

        // Test cancel on task completion
        reminderService.cancelRemindersForTask(task.getId());
        Reminder cancelledRem = reminderRepository.findById(reminders.get(1).getId()).orElseThrow();
        assertEquals(ReminderStatus.CANCELLED, cancelledRem.getStatus());
    }
}
