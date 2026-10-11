package com.securetodo.dto;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long id;
    private String name;
    private String email;
    private String timezone;
    private String theme;

    public AuthResponse() {}

    public AuthResponse(String token, Long id, String name, String email, String timezone, String theme) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.email = email;
        this.timezone = timezone;
        this.theme = theme;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

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
}

