package com.securetodo.service;

import com.securetodo.dto.CategoryDto;
import com.securetodo.dto.CategoryRequest;
import com.securetodo.entity.Category;
import com.securetodo.entity.User;
import com.securetodo.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDto> getCategories(User user) {
        return categoryRepository.findAllForUser(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto createCategory(User user, CategoryRequest request) {
        Category category = new Category(
                user,
                request.getName().trim(),
                request.getColor(),
                request.getIcon()
        );
        Category saved = categoryRepository.save(category);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteCategory(User user, Long categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or cannot be deleted"));
        categoryRepository.delete(category);
    }

    public CategoryDto mapToDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getIcon(),
                category.getUser() == null
        );
    }
}

