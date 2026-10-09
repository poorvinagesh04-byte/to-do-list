package com.securetodo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.securetodo.dto.TaskRequest;
import com.securetodo.dto.TaskResponse;
import com.securetodo.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportImportService {

    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    public ExportImportService(TaskService taskService) {
        this.taskService = taskService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public List<TaskResponse> exportJson(User user) {
        return taskService.getTasks(user, null, null, null, null, null, null, null, "scheduledDate", "asc");
    }

    public String exportCsv(User user) {
        List<TaskResponse> tasks = taskService.getTasks(user, null, null, null, null, null, null, null, "scheduledDate", "asc");
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Title,Description,ScheduledDate,StartTime,Deadline,Priority,Status,Category\n");

        for (TaskResponse t : tasks) {
            sb.append(t.getId()).append(",")
              .append("\"").append(escapeCsv(t.getTitle())).append("\",")
              .append("\"").append(escapeCsv(t.getDescription() != null ? t.getDescription() : "")).append("\",")
              .append(t.getScheduledDate()).append(",")
              .append(t.getStartTime() != null ? t.getStartTime() : "").append(",")
              .append(t.getDeadline() != null ? t.getDeadline() : "").append(",")
              .append(t.getPriority()).append(",")
              .append(t.getStatus()).append(",")
              .append("\"").append(escapeCsv(t.getCategoryName() != null ? t.getCategoryName() : "")).append("\"\n");
        }

        return sb.toString();
    }

    @Transactional
    public int importJson(User user, MultipartFile file) throws Exception {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        List<TaskRequest> requests = objectMapper.readValue(content, new TypeReference<List<TaskRequest>>() {});

        int imported = 0;
        for (TaskRequest req : requests) {
            if (req.getTitle() != null && !req.getTitle().isBlank() && req.getScheduledDate() != null) {
                taskService.createTask(user, req);
                imported++;
            }
        }
        return imported;
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }
}

