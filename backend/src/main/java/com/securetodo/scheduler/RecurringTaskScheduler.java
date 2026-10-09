package com.securetodo.scheduler;

import com.securetodo.entity.RecurringSchedule;
import com.securetodo.entity.Task;
import com.securetodo.entity.TaskStatus;
import com.securetodo.repository.RecurringScheduleRepository;
import com.securetodo.repository.TaskRepository;
import com.securetodo.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class RecurringTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringTaskScheduler.class);

    private final RecurringScheduleRepository recurringScheduleRepository;
    private final TaskRepository taskRepository;

    public RecurringTaskScheduler(RecurringScheduleRepository recurringScheduleRepository,
                                  TaskRepository taskRepository) {
        this.recurringScheduleRepository = recurringScheduleRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Runs hourly to spawn next recurring instances for completed recurring tasks or due recurring schedules.
     */
    @Scheduled(fixedDelay = 3600000) // every hour
    @Transactional
    public void processRecurringTasks() {
        try {
            List<RecurringSchedule> schedules = recurringScheduleRepository.findAllActiveSchedules();
            LocalDate today = LocalDate.now();

            for (RecurringSchedule schedule : schedules) {
                Task originalTask = schedule.getTask();
                if (originalTask.getStatus() == TaskStatus.COMPLETED &&
                    (schedule.getLastGeneratedDate() == null || schedule.getLastGeneratedDate().isBefore(today))) {

                    LocalDate nextDate = switch (schedule.getRecurrenceType()) {
                        case DAILY -> originalTask.getScheduledDate().plusDays(schedule.getRecurrenceInterval());
                        case WEEKLY -> originalTask.getScheduledDate().plusWeeks(schedule.getRecurrenceInterval());
                        case MONTHLY -> originalTask.getScheduledDate().plusMonths(schedule.getRecurrenceInterval());
                        case YEARLY -> originalTask.getScheduledDate().plusYears(schedule.getRecurrenceInterval());
                        default -> null;
                    };

                    if (nextDate != null && (schedule.getRecurrenceEndDate() == null || !nextDate.isAfter(schedule.getRecurrenceEndDate()))) {
                        Task nextTask = new Task();
                        nextTask.setUser(originalTask.getUser());
                        nextTask.setCategory(originalTask.getCategory());
                        nextTask.setTitle(originalTask.getTitle());
                        nextTask.setDescription(originalTask.getDescription());
                        nextTask.setScheduledDate(nextDate);
                        nextTask.setStartTime(originalTask.getStartTime());
                        nextTask.setPriority(originalTask.getPriority());
                        nextTask.setStatus(TaskStatus.PENDING);

                        Task savedNext = taskRepository.save(nextTask);

                        schedule.setLastGeneratedDate(today);
                        recurringScheduleRepository.save(schedule);

                        log.info("Generated recurring instance for task '{}' on {}", savedNext.getTitle(), nextDate);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in recurring task scheduler: {}", e.getMessage());
        }
    }
}

