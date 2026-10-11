package com.securetodo.service;

import com.securetodo.dto.*;
import com.securetodo.entity.Category;
import com.securetodo.entity.User;
import com.securetodo.repository.CategoryRepository;
import com.securetodo.repository.UserRepository;
import com.securetodo.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       CategoryRepository categoryRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(
                request.getName().trim(),
                request.getEmail().toLowerCase().trim(),
                encodedPassword,
                request.getTimezone() != null ? request.getTimezone() : "UTC"
        );

        user = userRepository.save(user);

        // Seed default user categories if none exist globally
        seedDefaultCategoriesForUser(user);

        String token = tokenProvider.generateToken(user.getEmail());
        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTimezone(),
                user.getTheme()
        );
    }

    public AuthResponse login(AuthRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = tokenProvider.generateToken(user.getEmail());
        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTimezone(),
                user.getTheme()
        );
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public UserProfileDto getProfile(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTimezone(),
                user.getTheme(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public UserProfileDto updateProfile(User user, UpdateProfileRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            user.setTimezone(request.getTimezone().trim());
        }
        if (request.getTheme() != null && !request.getTheme().isBlank()) {
            user.setTheme(request.getTheme().trim());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password does not match");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        user = userRepository.save(user);
        return getProfile(user);
    }

    @Transactional
    public void deleteAccount(User user) {
        userRepository.delete(user);
    }

    private void seedDefaultCategoriesForUser(User user) {
        // Ensure default categories exist
        List<Category> existing = categoryRepository.findAllForUser(user.getId());
        if (existing.isEmpty()) {
            categoryRepository.save(new Category(user, "College", "#3b82f6", "graduation-cap"));
            categoryRepository.save(new Category(user, "Work", "#10b981", "briefcase"));
            categoryRepository.save(new Category(user, "Personal", "#8b5cf6", "user"));
            categoryRepository.save(new Category(user, "Health", "#ef4444", "heart-pulse"));
            categoryRepository.save(new Category(user, "Shopping", "#f59e0b", "shopping-cart"));
        }
    }
}

