package com.securetodo.dto;

import com.securetodo.entity.Priority;
import com.securetodo.entity.RecurrenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(max = 255, message = "Task title cannot exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    private LocalTime startTime;

    private LocalDateTime deadline;

    private Priority priority = Priority.MEDIUM;

    private Long categoryId;

    private RecurrenceType recurrenceType = RecurrenceType.NONE;

    private int recurrenceInterval = 1;

    private LocalDate recurrenceEndDate;

    private List<SubtaskDto> subtasks = new ArrayList<>();

    // Reminder offsets in minutes (e.g. 0 = at time, 15 = 15m before, 60 = 1h before, 1440 = 1d before)
    private List<Integer> reminderOffsets = new ArrayList<>();

    public TaskRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public RecurrenceType getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(RecurrenceType recurrenceType) { this.recurrenceType = recurrenceType; }

    public int getRecurrenceInterval() { return recurrenceInterval; }
    public void setRecurrenceInterval(int recurrenceInterval) { this.recurrenceInterval = recurrenceInterval; }

    public LocalDate getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) { this.recurrenceEndDate = recurrenceEndDate; }

    public List<SubtaskDto> getSubtasks() { return subtasks; }
    public void setSubtasks(List<SubtaskDto> subtasks) { this.subtasks = subtasks; }

    public List<Integer> getReminderOffsets() { return reminderOffsets; }
    public void setReminderOffsets(List<Integer> reminderOffsets) { this.reminderOffsets = reminderOffsets; }
}

