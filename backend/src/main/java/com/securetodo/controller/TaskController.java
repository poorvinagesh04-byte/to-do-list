package com.securetodo.controller;

import com.securetodo.dto.*;
import com.securetodo.entity.Priority;
import com.securetodo.entity.TaskStatus;
import com.securetodo.entity.User;
import com.securetodo.repository.UserRepository;
import com.securetodo.security.SecurityUtils;
import com.securetodo.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Endpoints for creating, managing, completing, and rescheduling tasks")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    public TaskController(TaskService taskService, UserRepository userRepository) {
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Get user tasks with filtering, search, and sorting")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "scheduledDate") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder) {

        User user = SecurityUtils.getCurrentUser(userRepository);
        List<TaskResponse> tasks = taskService.getTasks(user, date, startDate, endDate, status, priority, categoryId, keyword, sortBy, sortOrder);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard summary metrics and lists")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        User user = SecurityUtils.getCurrentUser(userRepository);
        return ResponseEntity.ok(taskService.getDashboardSummary(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        return ResponseEntity.ok(taskService.getTaskById(user, id));
    }

    @PostMapping
    @Operation(summary = "Create a new scheduled task")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        TaskResponse response = taskService.createTask(user, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing task")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        TaskResponse response = taskService.updateTask(user, id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark task as completed")
    public ResponseEntity<TaskResponse> completeTask(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        TaskResponse response = taskService.completeTask(user, id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reopen")
    @Operation(summary = "Reopen a completed task")
    public ResponseEntity<TaskResponse> reopenTask(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        TaskResponse response = taskService.reopenTask(user, id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<ApiResponse> deleteTask(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        taskService.deleteTask(user, id);
        return ResponseEntity.ok(ApiResponse.ok("Task deleted successfully"));
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplicate a task")
    public ResponseEntity<TaskResponse> duplicateTask(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        TaskResponse response = taskService.duplicateTask(user, id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule a task to a new date and optional time")
    public ResponseEntity<TaskResponse> rescheduleTask(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newTime) {

        User user = SecurityUtils.getCurrentUser(userRepository);
        TaskResponse response = taskService.rescheduleTask(user, id, newDate, newTime);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/subtasks/{subtaskId}/toggle")
    @Operation(summary = "Toggle subtask completion status")
    public ResponseEntity<SubtaskDto> toggleSubtask(@PathVariable Long subtaskId) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        SubtaskDto response = taskService.toggleSubtask(user, subtaskId);
        return ResponseEntity.ok(response);
    }
}
