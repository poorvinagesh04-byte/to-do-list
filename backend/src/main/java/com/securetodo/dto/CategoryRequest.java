package com.securetodo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Category name cannot exceed 50 characters")
    private String name;

    @Size(max = 20, message = "Color code cannot exceed 20 characters")
    private String color = "#4f46e5";

    @Size(max = 50, message = "Icon cannot exceed 50 characters")
    private String icon = "folder";

    public CategoryRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}

