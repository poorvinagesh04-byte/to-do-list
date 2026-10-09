package com.securetodo.controller;

import com.securetodo.dto.ApiResponse;
import com.securetodo.dto.UpdateProfileRequest;
import com.securetodo.dto.UserProfileDto;
import com.securetodo.entity.User;
import com.securetodo.repository.UserRepository;
import com.securetodo.security.SecurityUtils;
import com.securetodo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for user profile settings and account deletion")
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public UserController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get user profile")
    public ResponseEntity<UserProfileDto> getProfile() {
        User user = SecurityUtils.getCurrentUser(userRepository);
        return ResponseEntity.ok(authService.getProfile(user));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile, timezone, theme, or change password")
    public ResponseEntity<UserProfileDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        UserProfileDto updated = authService.updateProfile(user, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/account")
    @Operation(summary = "Permanently delete account and all user data")
    public ResponseEntity<ApiResponse> deleteAccount() {
        User user = SecurityUtils.getCurrentUser(userRepository);
        authService.deleteAccount(user);
        return ResponseEntity.ok(ApiResponse.ok("Account and all associated tasks successfully deleted"));
    }
}

