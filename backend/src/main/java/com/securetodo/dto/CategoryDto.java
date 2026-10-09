package com.securetodo.dto;

public class CategoryDto {

    private Long id;
    private String name;
    private String color;
    private String icon;
    private boolean isDefault;

    public CategoryDto() {}

    public CategoryDto(Long id, String name, String color, String icon, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.isDefault = isDefault;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}

