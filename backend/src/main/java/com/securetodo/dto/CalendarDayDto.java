package com.securetodo.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarDayDto {

    private LocalDate date;
    private int dayOfMonth;
    private int totalCount;
    private int completedCount;
    private int pendingCount;
    private boolean hasHighPriority;
    private List<TaskResponse> tasks = new ArrayList<>();

    public CalendarDayDto() {}

    public CalendarDayDto(LocalDate date, int dayOfMonth) {
        this.date = date;
        this.dayOfMonth = dayOfMonth;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public boolean isHasHighPriority() { return hasHighPriority; }
    public void setHasHighPriority(boolean hasHighPriority) { this.hasHighPriority = hasHighPriority; }

    public List<TaskResponse> getTasks() { return tasks; }
    public void setTasks(List<TaskResponse> tasks) { this.tasks = tasks; }
}

