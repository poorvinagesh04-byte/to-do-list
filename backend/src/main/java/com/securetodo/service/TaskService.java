package com.securetodo.service;

import com.securetodo.dto.*;
import com.securetodo.entity.*;
import com.securetodo.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final SubtaskRepository subtaskRepository;
    private final RecurringScheduleRepository recurringScheduleRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final ReminderService reminderService;

    public TaskService(TaskRepository taskRepository,
                       CategoryRepository categoryRepository,
                       SubtaskRepository subtaskRepository,
                       RecurringScheduleRepository recurringScheduleRepository,
                       TaskHistoryRepository taskHistoryRepository,
                       ReminderService reminderService) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.subtaskRepository = subtaskRepository;
        this.recurringScheduleRepository = recurringScheduleRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.reminderService = reminderService;
    }

    public List<TaskResponse> getTasks(User user,
                                       LocalDate date,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       TaskStatus status,
                                       Priority priority,
                                       Long categoryId,
                                       String keyword,
                                       String sortBy,
                                       String sortOrder) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = switch (sortBy != null ? sortBy.toLowerCase() : "scheduleddate") {
            case "priority" -> "priority";
            case "deadline" -> "deadline";
            case "createdat" -> "createdAt";
            case "title" -> "title";
            default -> "scheduledDate";
        };
        Sort sort = Sort.by(direction, sortProperty);

        List<Task> tasks;
        if (date != null) {
            tasks = taskRepository.findByUserIdAndScheduledDate(user.getId(), date);
        } else if (startDate != null && endDate != null) {
            tasks = taskRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscStartTimeAsc(user.getId(), startDate, endDate);
        } else if (keyword != null && !keyword.isBlank()) {
            tasks = taskRepository.searchTasks(user.getId(), keyword.trim());
        } else if (status != null) {
            tasks = taskRepository.findByUserIdAndStatusOrderByScheduledDateDesc(user.getId(), status);
        } else {
            tasks = taskRepository.findAllByUserId(user.getId(), sort);
        }

        // Apply remaining filters in-memory
        return tasks.stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> categoryId == null || (t.getCategory() != null && t.getCategory().getId().equals(categoryId)))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(User user, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse createTask(User user, TaskRequest request) {
        Task task = new Task();
        task.setUser(user);
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setScheduledDate(request.getScheduledDate());
        task.setStartTime(request.getStartTime());
        task.setDeadline(request.getDeadline());
        task.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        task.setStatus(TaskStatus.PENDING);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .filter(c -> c.getUser() == null || c.getUser().getId().equals(user.getId()))
                    .orElse(null);
            task.setCategory(category);
        }

        Task savedTask = taskRepository.save(task);

        // Subtasks
        if (request.getSubtasks() != null) {
            for (SubtaskDto subDto : request.getSubtasks()) {
                if (subDto.getTitle() != null && !subDto.getTitle().isBlank()) {
                    Subtask subtask = new Subtask(subDto.getTitle().trim(), subDto.isCompleted());
                    savedTask.addSubtask(subtask);
                }
            }
        }

        // Recurring schedule
        if (request.getRecurrenceType() != null && request.getRecurrenceType() != RecurrenceType.NONE) {
            RecurringSchedule schedule = new RecurringSchedule(
                    request.getRecurrenceType(),
                    request.getRecurrenceInterval() > 0 ? request.getRecurrenceInterval() : 1,
                    request.getRecurrenceEndDate()
            );
            savedTask.setRecurringSchedule(schedule);
        }

        savedTask = taskRepository.save(savedTask);

        // Reminders
        if (request.getReminderOffsets() != null && !request.getReminderOffsets().isEmpty()) {
            reminderService.createOrUpdateRemindersForTask(savedTask, request.getReminderOffsets());
        }

        // History
        taskHistoryRepository.save(new TaskHistory(savedTask.getId(), user, TaskAction.CREATED, "Task created: " + savedTask.getTitle()));

        return mapToResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(User user, Long taskId, TaskRequest request) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setScheduledDate(request.getScheduledDate());
        task.setStartTime(request.getStartTime());
        task.setDeadline(request.getDeadline());
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .filter(c -> c.getUser() == null || c.getUser().getId().equals(user.getId()))
                    .orElse(null);
            task.setCategory(category);
        } else {
            task.setCategory(null);
        }

        // Update subtasks
        task.getSubtasks().clear();
        if (request.getSubtasks() != null) {
            for (SubtaskDto subDto : request.getSubtasks()) {
                if (subDto.getTitle() != null && !subDto.getTitle().isBlank()) {
                    Subtask subtask = new Subtask(subDto.getTitle().trim(), subDto.isCompleted());
                    task.addSubtask(subtask);
                }
            }
        }

        // Update recurring schedule
        if (request.getRecurrenceType() != null && request.getRecurrenceType() != RecurrenceType.NONE) {
            RecurringSchedule schedule = task.getRecurringSchedule();
            if (schedule == null) {
                schedule = new RecurringSchedule();
            }
            schedule.setRecurrenceType(request.getRecurrenceType());
            schedule.setRecurrenceInterval(request.getRecurrenceInterval() > 0 ? request.getRecurrenceInterval() : 1);
            schedule.setRecurrenceEndDate(request.getRecurrenceEndDate());
            task.setRecurringSchedule(schedule);
        } else {
            task.setRecurringSchedule(null);
        }

        Task updatedTask = taskRepository.save(task);

        // Recalculate reminders
        if (request.getReminderOffsets() != null && !request.getReminderOffsets().isEmpty()) {
            reminderService.createOrUpdateRemindersForTask(updatedTask, request.getReminderOffsets());
        }

        taskHistoryRepository.save(new TaskHistory(updatedTask.getId(), user, TaskAction.UPDATED, "Task updated: " + updatedTask.getTitle()));

        return mapToResponse(updatedTask);
    }

    @Transactional
    public TaskResponse completeTask(User user, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        // Automatically cancel pending reminders for completed task
        reminderService.cancelRemindersForTask(taskId);

        taskHistoryRepository.save(new TaskHistory(saved.getId(), user, TaskAction.COMPLETED, "Task marked as completed"));

        return mapToResponse(saved);
    }

    @Transactional
    public TaskResponse reopenTask(User user, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        task.setStatus(TaskStatus.PENDING);
        task.setCompletedAt(null);
        Task saved = taskRepository.save(task);

        taskHistoryRepository.save(new TaskHistory(saved.getId(), user, TaskAction.REOPENED, "Task reopened"));

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteTask(User user, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        taskHistoryRepository.save(new TaskHistory(taskId, user, TaskAction.DELETED, "Task deleted: " + task.getTitle()));
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse duplicateTask(User user, Long taskId) {
        Task original = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        Task duplicate = new Task();
        duplicate.setUser(user);
        duplicate.setCategory(original.getCategory());
        duplicate.setTitle("Copy of " + original.getTitle());
        duplicate.setDescription(original.getDescription());
        duplicate.setScheduledDate(original.getScheduledDate());
        duplicate.setStartTime(original.getStartTime());
        duplicate.setDeadline(original.getDeadline());
        duplicate.setPriority(original.getPriority());
        duplicate.setStatus(TaskStatus.PENDING);

        Task saved = taskRepository.save(duplicate);

        for (Subtask s : original.getSubtasks()) {
            saved.addSubtask(new Subtask(s.getTitle(), false));
        }

        saved = taskRepository.save(saved);
        taskHistoryRepository.save(new TaskHistory(saved.getId(), user, TaskAction.CREATED, "Duplicated from task #" + original.getId()));

        return mapToResponse(saved);
    }

    @Transactional
    public TaskResponse rescheduleTask(User user, Long taskId, LocalDate newDate, LocalTime newTime) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        task.setScheduledDate(newDate);
        if (newTime != null) {
            task.setStartTime(newTime);
        }
        Task saved = taskRepository.save(task);

        // Recalculate reminders if any exist
        List<Reminder> reminders = task.getReminders();
        if (reminders != null && !reminders.isEmpty()) {
            List<Integer> offsets = reminders.stream().map(Reminder::getOffsetMinutes).collect(Collectors.toList());
            reminderService.createOrUpdateRemindersForTask(saved, offsets);
        }

        taskHistoryRepository.save(new TaskHistory(saved.getId(), user, TaskAction.RESCHEDULED, "Rescheduled to " + newDate + (newTime != null ? " at " + newTime : "")));

        return mapToResponse(saved);
    }

    @Transactional
    public SubtaskDto toggleSubtask(User user, Long subtaskId) {
        Subtask subtask = subtaskRepository.findByIdAndUserId(subtaskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtask not found"));

        subtask.setCompleted(!subtask.isCompleted());
        Subtask saved = subtaskRepository.save(subtask);
        return new SubtaskDto(saved.getId(), saved.getTitle(), saved.isCompleted());
    }

    public DashboardSummaryDto getDashboardSummary(User user) {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        String greeting = "Good morning";
        if (nowTime.isAfter(LocalTime.of(12, 0)) && nowTime.isBefore(LocalTime.of(17, 0))) {
            greeting = "Good afternoon";
        } else if (nowTime.isAfter(LocalTime.of(17, 0))) {
            greeting = "Good evening";
        }

        long todayTotal = taskRepository.countByUserIdAndScheduledDate(user.getId(), today);
        long todayCompleted = taskRepository.countByUserIdAndScheduledDateAndStatus(user.getId(), today, TaskStatus.COMPLETED);
        long todayPending = todayTotal - todayCompleted;
        int progress = todayTotal > 0 ? (int) Math.round(((double) todayCompleted / todayTotal) * 100) : 0;

        long overdueCount = taskRepository.countOverdueTasks(user.getId(), today);

        List<Task> todayTasks = taskRepository.findByUserIdAndScheduledDate(user.getId(), today);
        List<Task> overdueTasks = taskRepository.findOverdueTasks(user.getId(), today);
        List<Task> upcomingTasks = taskRepository.findUpcomingTasks(user.getId(), today, today.plusDays(7));
        List<Task> highPriorityTasks = taskRepository.findHighPriorityTasks(user.getId());

        // 7-day strip count
        Map<String, Long> weeklyCounts = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.plusDays(i);
            long count = taskRepository.countByUserIdAndScheduledDate(user.getId(), d);
            weeklyCounts.put(d.toString(), count);
        }

        DashboardSummaryDto dto = new DashboardSummaryDto();
        dto.setGreeting(greeting);
        dto.setFormattedDate(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dto.setTodayTotalCount(todayTotal);
        dto.setTodayCompletedCount(todayCompleted);
        dto.setTodayPendingCount(todayPending);
        dto.setDailyProgressPercentage(progress);
        dto.setOverdueCount(overdueCount);
        dto.setUpcomingCount(upcomingTasks.size());
        dto.setHighPriorityCount(highPriorityTasks.size());
        dto.setTodayTasks(todayTasks.stream().map(this::mapToResponse).collect(Collectors.toList()));
        dto.setOverdueTasks(overdueTasks.stream().map(this::mapToResponse).collect(Collectors.toList()));
        dto.setUpcomingDeadlines(upcomingTasks.stream().map(this::mapToResponse).collect(Collectors.toList()));
        dto.setHighPriorityTasks(highPriorityTasks.stream().map(this::mapToResponse).collect(Collectors.toList()));
        dto.setWeeklyTaskCounts(weeklyCounts);

        return dto;
    }

    public TaskResponse mapToResponse(Task task) {
        TaskResponse res = new TaskResponse();
        res.setId(task.getId());
        res.setTitle(task.getTitle());
        res.setDescription(task.getDescription());
        res.setScheduledDate(task.getScheduledDate());
        res.setStartTime(task.getStartTime());
        res.setDeadline(task.getDeadline());
        res.setPriority(task.getPriority());
        res.setStatus(task.getStatus());
        res.setCompletedAt(task.getCompletedAt());
        res.setCreatedAt(task.getCreatedAt());
        res.setUpdatedAt(task.getUpdatedAt());

        boolean isOverdue = task.getStatus() == TaskStatus.PENDING &&
                task.getScheduledDate().isBefore(LocalDate.now());
        res.setOverdue(isOverdue);

        if (task.getCategory() != null) {
            res.setCategoryId(task.getCategory().getId());
            res.setCategoryName(task.getCategory().getName());
            res.setCategoryColor(task.getCategory().getColor());
            res.setCategoryIcon(task.getCategory().getIcon());
        }

        if (task.getRecurringSchedule() != null) {
            res.setRecurrenceType(task.getRecurringSchedule().getRecurrenceType());
            res.setRecurrenceInterval(task.getRecurringSchedule().getRecurrenceInterval());
            res.setRecurrenceEndDate(task.getRecurringSchedule().getRecurrenceEndDate());
        }

        if (task.getSubtasks() != null) {
            res.setSubtasks(task.getSubtasks().stream()
                    .map(s -> new SubtaskDto(s.getId(), s.getTitle(), s.isCompleted()))
                    .collect(Collectors.toList()));
        }

        if (task.getReminders() != null) {
            res.setReminders(task.getReminders().stream()
                    .map(reminderService::mapToDto)
                    .collect(Collectors.toList()));
        }

        return res;
    }
}

