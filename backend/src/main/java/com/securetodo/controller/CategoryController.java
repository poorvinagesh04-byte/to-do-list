package com.securetodo.controller;

import com.securetodo.dto.ApiResponse;
import com.securetodo.dto.CategoryDto;
import com.securetodo.dto.CategoryRequest;
import com.securetodo.entity.User;
import com.securetodo.repository.UserRepository;
import com.securetodo.security.SecurityUtils;
import com.securetodo.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Endpoints for managing task categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public CategoryController(CategoryService categoryService, UserRepository userRepository) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Get user categories including standard presets")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        User user = SecurityUtils.getCurrentUser(userRepository);
        return ResponseEntity.ok(categoryService.getCategories(user));
    }

    @PostMapping
    @Operation(summary = "Create a custom category")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryRequest request) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        return ResponseEntity.ok(categoryService.createCategory(user, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a custom category")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser(userRepository);
        categoryService.deleteCategory(user, id);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted successfully"));
    }
}
