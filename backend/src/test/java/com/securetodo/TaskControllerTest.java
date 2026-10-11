package com.securetodo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securetodo.dto.AuthRequest;
import com.securetodo.dto.AuthResponse;
import com.securetodo.dto.RegisterRequest;
import com.securetodo.dto.TaskRequest;
import com.securetodo.entity.Priority;
import com.securetodo.entity.RecurrenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    public void setup() throws Exception {
        String email = "task_test_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest regReq = new RegisterRequest();
        regReq.setName("Task User");
        regReq.setEmail(email);
        regReq.setPassword("Password123!");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse res = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        userToken = res.getToken();
    }

    @Test
    @DisplayName("Should create task scheduled for years in the future with reminders")
    public void testCreateFutureTask() throws Exception {
        TaskRequest req = new TaskRequest();
        req.setTitle("Review 5-Year Strategic Plan");
        req.setDescription("Comprehensive review of company roadmap");
        req.setScheduledDate(LocalDate.now().plusYears(2)); // 2 years into future
        req.setStartTime(LocalTime.of(14, 30));
        req.setPriority(Priority.HIGH);
        req.setRecurrenceType(RecurrenceType.YEARLY);
        req.setReminderOffsets(List.of(0, 60, 1440)); // At time, 1h before, 1d before

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Review 5-Year Strategic Plan"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.reminders.length()").value(3));
    }

    @Test
    @DisplayName("Should complete task and cancel reminders, then reopen task")
    public void testCompleteAndReopenTask() throws Exception {
        TaskRequest req = new TaskRequest();
        req.setTitle("Pay Monthly Rent");
        req.setScheduledDate(LocalDate.now());
        req.setPriority(Priority.URGENT);
        req.setReminderOffsets(List.of(15));

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // 1. Mark as completed
        mockMvc.perform(patch("/api/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        // 2. Reopen task
        mockMvc.perform(patch("/api/tasks/" + taskId + "/reopen")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    @Test
    @DisplayName("Should duplicate and reschedule task")
    public void testDuplicateAndRescheduleTask() throws Exception {
        TaskRequest req = new TaskRequest();
        req.setTitle("Submit Lab Report");
        req.setScheduledDate(LocalDate.now().plusDays(1));

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Duplicate
        mockMvc.perform(post("/api/tasks/" + taskId + "/duplicate")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Copy of Submit Lab Report"));

        // Reschedule
        LocalDate newDate = LocalDate.now().plusDays(7);
        mockMvc.perform(patch("/api/tasks/" + taskId + "/reschedule?newDate=" + newDate)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledDate").value(newDate.toString()));
    }

    @Test
    @DisplayName("Should fetch dashboard summary metrics correctly")
    public void testDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/tasks/dashboard")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.greeting").isNotEmpty())
                .andExpect(jsonPath("$.weeklyTaskCounts").isMap());
    }
}

