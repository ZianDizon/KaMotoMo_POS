package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.utils.SecurityUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HelloController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private javafx.scene.control.Label errorMessage;


    @FXML
    protected void onLoginButtonClick() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            errorMessage.setText("Please enter both username and password.");
            return;
        }

        // Updated query to match your new DDL exactly
        String sql = "SELECT * FROM `user` WHERE username = ? AND password = ? AND status = 'Active'";

        try (java.sql.Connection conn = com.kamotomo.pos.database.DatabaseConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user);
            stmt.setString(2, pass);

            java.sql.ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Login Success! Save the user details to the session
                com.kamotomo.pos.utils.UserSession.getInstance().startSession(
                        rs.getInt("userID"),
                        rs.getString("username"),
                        rs.getString("name"), // Pulling the new 'name' column
                        rs.getString("role"),
                        rs.getString("themePreference")
                );

                com.kamotomo.pos.utils.SystemLogger.logAction("Login", "User successfully authenticated.");

                // Load the Dashboard
                javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(HelloApplication.class.getResource("dashboard-view.fxml"));
                javafx.scene.Scene scene = new javafx.scene.Scene(fxmlLoader.load(), 1200, 700);

                // Apply the saved theme preference
                String theme = rs.getString("themePreference");
                if (theme != null && theme.equals("light")) {
                    scene.getStylesheets().add(getClass().getResource("/light-theme.css").toExternalForm());
                } else {
                    scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
                }

                javafx.stage.Stage stage = (javafx.stage.Stage) usernameField.getScene().getWindow();
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.centerOnScreen();
                stage.show();

            } else {
                // If it fails, we check if the user exists but is archived
                checkIfArchived(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorMessage.setText("Database connection error.");
        }
    }

    // A helpful method to tell the user WHY they can't log in
    private void checkIfArchived(String username) {
        String sql = "SELECT status FROM `user` WHERE username = ?";
        try (java.sql.Connection conn = com.kamotomo.pos.database.DatabaseConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getString("status").equals("Archived")) {
                errorMessage.setText("This account has been archived.");
            } else {
                errorMessage.setText("Invalid username or password.");
            }
        } catch (Exception e) {
            errorMessage.setText("Invalid username or password.");
        }
    }
}