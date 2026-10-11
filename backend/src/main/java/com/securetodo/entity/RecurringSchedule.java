package com.securetodo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_schedules", indexes = {
    @Index(name = "idx_recurring_task_id", columnList = "task_id")
})
public class RecurringSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, unique = true)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 20)
    private RecurrenceType recurrenceType = RecurrenceType.NONE;

    @Column(name = "recurrence_interval", nullable = false)
    private int recurrenceInterval = 1;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "last_generated_date")
    private LocalDate lastGeneratedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RecurringSchedule() {}

    public RecurringSchedule(RecurrenceType recurrenceType, int recurrenceInterval, LocalDate recurrenceEndDate) {
        this.recurrenceType = recurrenceType;
        this.recurrenceInterval = recurrenceInterval > 0 ? recurrenceInterval : 1;
        this.recurrenceEndDate = recurrenceEndDate;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public RecurrenceType getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(RecurrenceType recurrenceType) { this.recurrenceType = recurrenceType; }

    public int getRecurrenceInterval() { return recurrenceInterval; }
    public void setRecurrenceInterval(int recurrenceInterval) { this.recurrenceInterval = recurrenceInterval; }

    public LocalDate getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) { this.recurrenceEndDate = recurrenceEndDate; }

    public LocalDate getLastGeneratedDate() { return lastGeneratedDate; }
    public void setLastGeneratedDate(LocalDate lastGeneratedDate) { this.lastGeneratedDate = lastGeneratedDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

