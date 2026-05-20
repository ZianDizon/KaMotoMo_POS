package com.kamotomo.pos.utils;

public class UserSession {
    private static UserSession instance;

    private int userId;
    private String username;
    private String name;
    private String role;
    private String themePreference;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void startSession(int userId, String username, String name, String role, String themePreference) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.role = role;
        this.themePreference = themePreference;
    }

    public void clearSession() {
        userId = 0;
        username = null;
        name = null;
        role = null;
        themePreference = null;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getThemePreference() { return themePreference; }
    public void setThemePreference(String theme) { this.themePreference = theme; }
}