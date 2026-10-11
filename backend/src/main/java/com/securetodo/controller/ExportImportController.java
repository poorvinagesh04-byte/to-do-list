package com.securetodo.controller;

import com.securetodo.dto.ApiResponse;
import com.securetodo.dto.TaskResponse;
import com.securetodo.entity.User;
import com.securetodo.repository.UserRepository;
import com.securetodo.security.SecurityUtils;
import com.securetodo.service.ExportImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Export & Import", description = "Endpoints for exporting and importing task data (JSON & CSV)")
public class ExportImportController {

    private final ExportImportService exportImportService;
    private final UserRepository userRepository;

    public ExportImportController(ExportImportService exportImportService, UserRepository userRepository) {
        this.exportImportService = exportImportService;
        this.userRepository = userRepository;
    }

    @GetMapping("/export/json")
    @Operation(summary = "Export all user tasks as JSON file")
    public ResponseEntity<List<TaskResponse>> exportJson() {
        User user = SecurityUtils.getCurrentUser(userRepository);
        List<TaskResponse> tasks = exportImportService.exportJson(user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tasks-backup-" + LocalDate.now() + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(tasks);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export all user tasks as CSV file")
    public ResponseEntity<byte[]> exportCsv() {
        User user = SecurityUtils.getCurrentUser(userRepository);
        String csv = exportImportService.exportCsv(user);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tasks-backup-" + LocalDate.now() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @PostMapping(value = "/import/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import tasks from JSON file")
    public ResponseEntity<ApiResponse> importJson(@RequestParam("file") MultipartFile file) {
        try {
            User user = SecurityUtils.getCurrentUser(userRepository);
            int count = exportImportService.importJson(user, file);
            return ResponseEntity.ok(ApiResponse.ok("Successfully imported " + count + " tasks"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to import tasks: " + e.getMessage()));
        }
    }
}

