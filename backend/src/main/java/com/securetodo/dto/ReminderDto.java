package com.securetodo.dto;

import com.securetodo.entity.DeliveryMethod;
import com.securetodo.entity.ReminderStatus;

import java.time.LocalDateTime;

public class ReminderDto {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private LocalDateTime reminderDatetime;
    private int offsetMinutes;
    private DeliveryMethod deliveryMethod;
    private ReminderStatus status;
    private String deduplicationKey;

    public ReminderDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public LocalDateTime getReminderDatetime() { return reminderDatetime; }
    public void setReminderDatetime(LocalDateTime reminderDatetime) { this.reminderDatetime = reminderDatetime; }

    public int getOffsetMinutes() { return offsetMinutes; }
    public void setOffsetMinutes(int offsetMinutes) { this.offsetMinutes = offsetMinutes; }

    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(DeliveryMethod deliveryMethod) { this.deliveryMethod = deliveryMethod; }

    public ReminderStatus getStatus() { return status; }
    public void setStatus(ReminderStatus status) { this.status = status; }

    public String getDeduplicationKey() { return deduplicationKey; }
    public void setDeduplicationKey(String deduplicationKey) { this.deduplicationKey = deduplicationKey; }
}

