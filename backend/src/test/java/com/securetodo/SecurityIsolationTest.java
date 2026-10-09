package com.securetodo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securetodo.dto.AuthResponse;
import com.securetodo.dto.RegisterRequest;
import com.securetodo.dto.TaskRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String name, String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPassword("Password123!");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse res = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return res.getToken();
    }

    @Test
    @DisplayName("Cross-User Isolation: User B cannot access, modify, or delete User A's tasks")
    public void testStrictUserIsolation() throws Exception {
        long timestamp = System.currentTimeMillis();
        String tokenA = registerAndGetToken("User A", "userA_" + timestamp + "@example.com");
        String tokenB = registerAndGetToken("User B", "userB_" + timestamp + "@example.com");

        // 1. User A creates a confidential task
        TaskRequest taskReq = new TaskRequest();
        taskReq.setTitle("Confidential Task of User A");
        taskReq.setScheduledDate(LocalDate.now());

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andExpect(status().isOk())
                .andReturn();

        Long taskAId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. User B tries to GET User A's task -> Must be rejected (404/403)
        mockMvc.perform(get("/api/tasks/" + taskAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // 3. User B tries to update User A's task -> Must be rejected
        TaskRequest maliciousUpdate = new TaskRequest();
        maliciousUpdate.setTitle("Hacked by User B");
        maliciousUpdate.setScheduledDate(LocalDate.now());

        mockMvc.perform(put("/api/tasks/" + taskAId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousUpdate)))
                .andExpect(status().isNotFound());

        // 4. User B tries to complete User A's task -> Must be rejected
        mockMvc.perform(patch("/api/tasks/" + taskAId + "/complete")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // 5. User B tries to delete User A's task -> Must be rejected
        mockMvc.perform(delete("/api/tasks/" + taskAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // 6. User B lists all tasks -> Must NOT contain User A's task
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        // 7. Unauthenticated request without token must be rejected (401/403)
        mockMvc.perform(get("/api/tasks/" + taskAId))
                .andExpect(status().isForbidden());
    }
}

