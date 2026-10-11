package com.securetodo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securetodo.dto.AuthRequest;
import com.securetodo.dto.PasswordResetRequest;
import com.securetodo.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully register a new user and return JWT token")
    public void testSuccessfulRegistration() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setEmail("reg_test_" + System.currentTimeMillis() + "@example.com");
        req.setPassword("Password123!");
        req.setTimezone("UTC");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(req.getEmail()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Should reject registration with invalid email")
    public void testRegistrationInvalidEmail() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setEmail("invalid-email-format");
        req.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration with short password")
    public void testRegistrationShortPassword() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setEmail("short_pass@example.com");
        req.setPassword("123"); // under 6 chars

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login registered user and return JWT")
    public void testSuccessfulLogin() throws Exception {
        String email = "login_test_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest regReq = new RegisterRequest();
        regReq.setName("Login Tester");
        regReq.setEmail(email);
        regReq.setPassword("Password123!");
        regReq.setTimezone("UTC");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        AuthRequest authReq = new AuthRequest(email, "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject login with incorrect password")
    public void testLoginWrongPassword() throws Exception {
        String email = "wrong_pass_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest regReq = new RegisterRequest();
        regReq.setName("Wrong Pass Tester");
        regReq.setEmail(email);
        regReq.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        AuthRequest authReq = new AuthRequest(email, "IncorrectPassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reset password and allow login with new password")
    public void testPasswordResetFlow() throws Exception {
        String email = "reset_test_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest regReq = new RegisterRequest();
        regReq.setName("Reset Tester");
        regReq.setEmail(email);
        regReq.setPassword("OldPassword123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        // Reset password
        PasswordResetRequest resetReq = new PasswordResetRequest();
        resetReq.setEmail(email);
        resetReq.setNewPassword("NewSecurePassword456");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());

        // Login with new password
        AuthRequest authReq = new AuthRequest(email, "NewSecurePassword456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq)))
                .andExpect(status().isOk());
    }
}

