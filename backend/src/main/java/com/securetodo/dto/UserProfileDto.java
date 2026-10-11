package com.securetodo.dto;

import java.time.LocalDateTime;

public class UserProfileDto {

    private Long id;
    private String name;
    private String email;
    private String timezone;
    private String theme;
    private LocalDateTime createdAt;

    public UserProfileDto() {}

    public UserProfileDto(Long id, String name, String email, String timezone, String theme, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.timezone = timezone;
        this.theme = theme;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

