package com.securetodo.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardSummaryDto {

    private String greeting;
    private String formattedDate;
    private long todayTotalCount;
    private long todayCompletedCount;
    private long todayPendingCount;
    private int dailyProgressPercentage;

    private long overdueCount;
    private long upcomingCount;
    private long highPriorityCount;

    private List<TaskResponse> todayTasks = new ArrayList<>();
    private List<TaskResponse> overdueTasks = new ArrayList<>();
    private List<TaskResponse> upcomingDeadlines = new ArrayList<>();
    private List<TaskResponse> highPriorityTasks = new ArrayList<>();

    // Map of date (YYYY-MM-DD) to task count for the 7-day strip
    private Map<String, Long> weeklyTaskCounts;

    public DashboardSummaryDto() {}

    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }

    public String getFormattedDate() { return formattedDate; }
    public void setFormattedDate(String formattedDate) { this.formattedDate = formattedDate; }

    public long getTodayTotalCount() { return todayTotalCount; }
    public void setTodayTotalCount(long todayTotalCount) { this.todayTotalCount = todayTotalCount; }

    public long getTodayCompletedCount() { return todayCompletedCount; }
    public void setTodayCompletedCount(long todayCompletedCount) { this.todayCompletedCount = todayCompletedCount; }

    public long getTodayPendingCount() { return todayPendingCount; }
    public void setTodayPendingCount(long todayPendingCount) { this.todayPendingCount = todayPendingCount; }

    public int getDailyProgressPercentage() { return dailyProgressPercentage; }
    public void setDailyProgressPercentage(int dailyProgressPercentage) { this.dailyProgressPercentage = dailyProgressPercentage; }

    public long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }

    public long getUpcomingCount() { return upcomingCount; }
    public void setUpcomingCount(long upcomingCount) { this.upcomingCount = upcomingCount; }

    public long getHighPriorityCount() { return highPriorityCount; }
    public void setHighPriorityCount(long highPriorityCount) { this.highPriorityCount = highPriorityCount; }

    public List<TaskResponse> getTodayTasks() { return todayTasks; }
    public void setTodayTasks(List<TaskResponse> todayTasks) { this.todayTasks = todayTasks; }

    public List<TaskResponse> getOverdueTasks() { return overdueTasks; }
    public void setOverdueTasks(List<TaskResponse> overdueTasks) { this.overdueTasks = overdueTasks; }

    public List<TaskResponse> getUpcomingDeadlines() { return upcomingDeadlines; }
    public void setUpcomingDeadlines(List<TaskResponse> upcomingDeadlines) { this.upcomingDeadlines = upcomingDeadlines; }

    public List<TaskResponse> getHighPriorityTasks() { return highPriorityTasks; }
    public void setHighPriorityTasks(List<TaskResponse> highPriorityTasks) { this.highPriorityTasks = highPriorityTasks; }

    public Map<String, Long> getWeeklyTaskCounts() { return weeklyTaskCounts; }
    public void setWeeklyTaskCounts(Map<String, Long> weeklyTaskCounts) { this.weeklyTaskCounts = weeklyTaskCounts; }
}

