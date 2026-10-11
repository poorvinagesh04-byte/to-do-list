package com.securetodo.controller;

import com.securetodo.dto.CalendarDayDto;
import com.securetodo.entity.User;
import com.securetodo.repository.UserRepository;
import com.securetodo.security.SecurityUtils;
import com.securetodo.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@Tag(name = "Calendar", description = "Endpoints for monthly calendar and yearly planning")
public class CalendarController {

    private final CalendarService calendarService;
    private final UserRepository userRepository;

    public CalendarController(CalendarService calendarService, UserRepository userRepository) {
        this.calendarService = calendarService;
        this.userRepository = userRepository;
    }

    @GetMapping("/month")
    @Operation(summary = "Get daily calendar tasks and status for a specific month and year")
    public ResponseEntity<List<CalendarDayDto>> getMonthCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        User user = SecurityUtils.getCurrentUser(userRepository);
        LocalDate now = LocalDate.now();
        int targetYear = (year != null && year >= 1970) ? year : now.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();

        List<CalendarDayDto> days = calendarService.getMonthCalendar(user, targetYear, targetMonth);
        return ResponseEntity.ok(days);
    }

    @GetMapping("/year")
    @Operation(summary = "Get high-level yearly planning summary for any year")
    public ResponseEntity<Map<String, Object>> getYearSummary(
            @RequestParam(required = false) Integer year) {

        User user = SecurityUtils.getCurrentUser(userRepository);
        int targetYear = (year != null && year >= 1970) ? year : LocalDate.now().getYear();

        Map<String, Object> summary = calendarService.getYearSummary(user, targetYear);
        return ResponseEntity.ok(summary);
    }
}

