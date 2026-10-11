package com.securetodo.controller;

import com.securetodo.dto.ApiResponse;
import com.securetodo.dto.ReminderDto;
import com.securetodo.entity.User;
import com.securetodo.repository.UserRepository;
import com.securetodo.security.SecurityUtils;
import com.securetodo.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@Tag(name = "Reminders", description = "Endpoints for managing reminders and in-app notifications")
public class ReminderController {

    private final ReminderService reminderService;
    private final UserRepository userRepository;

    public ReminderController(ReminderService reminderService, UserRepository userRepository) {
        this.reminderService = reminderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Get all reminders for the authenticated user")
    public ResponseEntity<List<ReminderDto>> getReminders() {
        User user = SecurityUtils.getCurrentUser(userRepository);
        return ResponseEntity.ok(reminderService.getUserReminders(user));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending notifications for in-app notification center")
    public ResponseEntity<List<ReminderDto>> getPendingReminders() {
        User user = SecurityUtils.getCurrentUser(userRepository);
        return ResponseEntity.ok(reminderService.getPendingReminders(user));
    }

    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss a reminder notification")
    public ResponseEntity<ApiResponse> dismissReminder(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        reminderService.dismissReminder(user, id);
        return ResponseEntity.ok(ApiResponse.ok("Reminder dismissed"));
    }

    @PostMapping("/{id}/snooze")
    @Operation(summary = "Snooze a reminder by specified minutes")
    public ResponseEntity<ApiResponse> snoozeReminder(@PathVariable Long id, @RequestParam(defaultValue = "10") int minutes) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        reminderService.snoozeReminder(user, id, minutes);
        return ResponseEntity.ok(ApiResponse.ok("Reminder snoozed for " + minutes + " minutes"));
    }
}

