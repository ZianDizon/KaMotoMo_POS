package com.kamotomo.pos.utils;

import com.kamotomo.pos.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class SystemLogger {

    public static void logAction(String action, String details) {
        // Automatically pull the username from the active session
        String username = UserSession.getInstance().getUsername();
        if (username == null) username = "System"; // Fallback for pre-login errors

        String sql = "INSERT INTO `system_log` (username, action, details) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("Failed to write to system log: " + e.getMessage());
        }
    }
}