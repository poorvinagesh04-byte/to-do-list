package com.securetodo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminders", indexes = {
    @Index(name = "idx_reminders_status_time", columnList = "status, reminder_datetime"),
    @Index(name = "idx_reminders_user_id", columnList = "user_id"),
    @Index(name = "idx_reminders_task_id", columnList = "task_id")
})
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reminder_datetime", nullable = false)
    private LocalDateTime reminderDatetime;

    @Column(name = "offset_minutes", nullable = false)
    private int offsetMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 30)
    private DeliveryMethod deliveryMethod = DeliveryMethod.IN_APP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "deduplication_key", nullable = false, unique = true, length = 150)
    private String deduplicationKey;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Reminder() {}

    public Reminder(Task task, User user, LocalDateTime reminderDatetime, int offsetMinutes, DeliveryMethod deliveryMethod, String deduplicationKey) {
        this.task = task;
        this.user = user;
        this.reminderDatetime = reminderDatetime;
        this.offsetMinutes = offsetMinutes;
        this.deliveryMethod = deliveryMethod != null ? deliveryMethod : DeliveryMethod.IN_APP;
        this.status = ReminderStatus.PENDING;
        this.deduplicationKey = deduplicationKey;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = ReminderStatus.PENDING;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

