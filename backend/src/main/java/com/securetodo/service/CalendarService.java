package com.securetodo.service;

import com.securetodo.dto.CalendarDayDto;
import com.securetodo.dto.TaskResponse;
import com.securetodo.entity.Priority;
import com.securetodo.entity.Task;
import com.securetodo.entity.TaskStatus;
import com.securetodo.entity.User;
import com.securetodo.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private final TaskRepository taskRepository;
    private final TaskService taskService;

    public CalendarService(TaskRepository taskRepository, TaskService taskService) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
    }

    public List<CalendarDayDto> getMonthCalendar(User user, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Task> tasks = taskRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscStartTimeAsc(
                user.getId(), startDate, endDate
        );

        Map<LocalDate, List<Task>> tasksByDate = tasks.stream()
                .collect(Collectors.groupingBy(Task::getScheduledDate));

        List<CalendarDayDto> days = new ArrayList<>();
        int daysInMonth = ym.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate current = ym.atDay(day);
            CalendarDayDto dayDto = new CalendarDayDto(current, day);

            List<Task> dayTasks = tasksByDate.getOrDefault(current, Collections.emptyList());
            int completedCount = 0;
            boolean hasHighPriority = false;

            List<TaskResponse> responseList = new ArrayList<>();
            for (Task t : dayTasks) {
                if (t.getStatus() == TaskStatus.COMPLETED) {
                    completedCount++;
                }
                if (t.getPriority() == Priority.HIGH || t.getPriority() == Priority.URGENT) {
                    hasHighPriority = true;
                }
                responseList.add(taskService.mapToResponse(t));
            }

            dayDto.setTotalCount(dayTasks.size());
            dayDto.setCompletedCount(completedCount);
            dayDto.setPendingCount(dayTasks.size() - completedCount);
            dayDto.setHasHighPriority(hasHighPriority);
            dayDto.setTasks(responseList);

            days.add(dayDto);
        }

        return days;
    }

    public Map<String, Object> getYearSummary(User user, int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        List<Task> tasks = taskRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscStartTimeAsc(
                user.getId(), startOfYear, endOfYear
        );

        Map<Integer, List<Task>> tasksByMonth = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getScheduledDate().getMonthValue()));

        List<Map<String, Object>> monthSummaries = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            List<Task> monthTasks = tasksByMonth.getOrDefault(m, Collections.emptyList());
            long completed = monthTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", m);
            monthData.put("monthName", YearMonth.of(year, m).getMonth().name());
            monthData.put("totalTasks", monthTasks.size());
            monthData.put("completedTasks", completed);
            monthData.put("pendingTasks", monthTasks.size() - completed);
            monthSummaries.add(monthData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("totalTasksInYear", tasks.size());
        result.put("months", monthSummaries);
        return result;
    }
}

