package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HelloController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Label errorMessage;

    @FXML
    public void initialize() {
        // --- REQUIREMENT 1.2: 16-Character Alphanumeric Limit ---
        usernameField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("^[a-zA-Z0-9]{0,16}$")) {
                return change;
            }
            return null;
        }));
    }

    @FXML
    protected void onLoginButtonClick() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            errorMessage.setText("Invalid Credentials");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Fetch user data first to check lockout status
            String checkSql = "SELECT userID, password, status, role, name, themePreference, failedAttempts, lockoutTimestamp FROM `user` WHERE BINARY username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, user);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {

                int userId = rs.getInt("userID");
                String dbHash = rs.getString("password");
                String status = rs.getString("status");
                int failedAttempts = rs.getInt("failedAttempts");
                java.sql.Timestamp lockoutTime = rs.getTimestamp("lockoutTimestamp");

                // Check active status
                if ("Archived".equalsIgnoreCase(status)) {
                    errorMessage.setText("Invalid Credentials");
                    return;
                }

                // --- REQUIREMENT 1.14: Brute-Force Lockout Check ---
                if (lockoutTime != null) {
                    long lockTimeMillis = lockoutTime.getTime();
                    long currentTimeMillis = System.currentTimeMillis();
                    long minutesPassed = (currentTimeMillis - lockTimeMillis) / (60 * 1000);

                    if (minutesPassed < 15) {
                        long minutesLeft = 15 - minutesPassed;
                        errorMessage.setText("Account locked. Try again in " + minutesLeft + " minutes.");
                        return;
                    } else {
                        // Lockout expired, reset attempts in memory and DB
                        failedAttempts = 0;
                        resetLockout(userId, conn);
                    }
                }

                // 2. Verify Password Hash
                String hashedAttempt = com.kamotomo.pos.utils.SecurityUtil.hashPassword(pass);

                if (dbHash.equals(hashedAttempt)) {
                    // Success: clear any previous failed attempts
                    if (failedAttempts > 0) resetLockout(userId, conn);

                    com.kamotomo.pos.utils.UserSession.getInstance().startSession(
                            userId, user, rs.getString("name"), rs.getString("role"), rs.getString("themePreference")
                    );

                    com.kamotomo.pos.utils.SystemLogger.logAction("Login", "User successfully authenticated.");

                    FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("dashboard-view.fxml"));
                    Scene scene = new Scene(fxmlLoader.load(), 1200, 700);

                    String theme = rs.getString("themePreference");
                    if (theme != null && theme.equals("light")) {
                        scene.getStylesheets().add(getClass().getResource("/light-theme.css").toExternalForm());
                    } else {
                        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
                    }

                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    stage.setScene(scene);
                    stage.setMaximized(true);
                    stage.centerOnScreen();
                    stage.show();

                } else {
                    // Failure: increment attempts and check threshold
                    failedAttempts++;
                    if (failedAttempts >= 5) {
                        lockAccount(userId, conn);
                        errorMessage.setText("Account locked due to too many failed attempts.");
                        com.kamotomo.pos.utils.SystemLogger.logAction("Security", "Account locked due to brute-force protection: " + user);
                    } else {
                        updateFailedAttempts(userId, conn, failedAttempts);
                        errorMessage.setText("Invalid Credentials");
                    }
                }
            } else {
                // User not found
                errorMessage.setText("Invalid Credentials");
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage.setText("System Error");
        }
    }

    // --- HELPER METHODS FOR REQUIREMENT 1.14 ---
    private void lockAccount(int userId, Connection conn) throws Exception {
        String sql = "UPDATE `user` SET failedAttempts = 5, lockoutTimestamp = CURRENT_TIMESTAMP WHERE userID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private void updateFailedAttempts(int userId, Connection conn, int attempts) throws Exception {
        String sql = "UPDATE `user` SET failedAttempts = ? WHERE userID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attempts);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    private void resetLockout(int userId, Connection conn) throws Exception {
        String sql = "UPDATE `user` SET failedAttempts = 0, lockoutTimestamp = NULL WHERE userID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    @FXML
    protected void onForgotPasswordClick() {
        TextInputDialog userDialog = new TextInputDialog();
        userDialog.setTitle("Account Recovery");
        userDialog.setHeaderText("Identify your account");
        userDialog.setContentText("Enter your Username:");
        applyThemeToDialog(userDialog.getDialogPane());

        userDialog.showAndWait().ifPresent(username -> {
            if (username.trim().isEmpty()) return;

            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT role, securityAnswer FROM `user` WHERE BINARY username = ? AND status = 'Active'";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username.trim());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String role = rs.getString("role");
                    String backupCode = rs.getString("securityAnswer");

                    if ("Employee".equalsIgnoreCase(role)) {
                        showThemedAlert(Alert.AlertType.INFORMATION, "Recovery Instruction", "As an Employee, you cannot reset your own password. Please contact your Store Administrator to issue a secure password reset.");
                        return;
                    }

                    if (backupCode == null || backupCode.isEmpty()) {
                        showThemedAlert(Alert.AlertType.ERROR, "Recovery Unavailable", "No backup recovery code configured for this Admin account.");
                        return;
                    }

                    showAdminRecoveryDialog(username.trim(), backupCode);
                } else {
                    showThemedAlert(Alert.AlertType.ERROR, "Not Found", "Account not found or is inactive.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showThemedAlert(Alert.AlertType.ERROR, "Database Error", "Failed to connect to the recovery database.");
            }
        });
    }

    private void showAdminRecoveryDialog(String username, String correctBackupCode) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Admin Recovery");
        dialog.setHeaderText("Emergency Admin Override");

        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField codeField = new TextField();
        codeField.setPromptText("Enter 16-character Backup Code");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Strict: Min 8, Upper, Lower, Num, Special");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm New Password");

        grid.add(new VBox(5, new Label("Backup Code:"), codeField), 0, 0);
        grid.add(new VBox(5, new Label("New Password:"), newPasswordField), 0, 1);
        grid.add(new VBox(5, new Label("Confirm Password:"), confirmPasswordField), 0, 2);

        dialogPane.setContent(grid);

        ButtonType saveButtonType = new ButtonType("Reset Password", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        final Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String code = codeField.getText().trim();
            String p1 = newPasswordField.getText().trim();
            String p2 = confirmPasswordField.getText().trim();

            if (code.isEmpty() || p1.isEmpty() || p2.isEmpty()) {
                showThemedAlert(Alert.AlertType.WARNING, "Missing Fields", "Please fill out all fields.");
                event.consume();
                return;
            }

            if (!code.equals(correctBackupCode)) {
                showThemedAlert(Alert.AlertType.ERROR, "Verification Failed", "The backup recovery code provided is incorrect.");
                com.kamotomo.pos.utils.SystemLogger.logAction("Security", "Failed admin recovery attempt for user: " + username);
                event.consume();
                return;
            }

            if (!p1.equals(p2)) {
                showThemedAlert(Alert.AlertType.WARNING, "Password Mismatch", "The new passwords do not match.");
                event.consume();
                return;
            }

            if (!com.kamotomo.pos.utils.SecurityUtil.isPasswordStrong(p1)) {
                showThemedAlert(Alert.AlertType.WARNING, "Weak Password",
                        "Password does not meet security requirements:\n" +
                                "• Minimum of 8 characters\n" +
                                "• At least 1 uppercase letter\n" +
                                "• At least 1 lowercase letter\n" +
                                "• At least 1 number\n" +
                                "• At least 1 special character (@$!%*?&)");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String updateSql = "UPDATE `user` SET password = ? WHERE BINARY username = ?";
                    PreparedStatement stmt = conn.prepareStatement(updateSql);
                    stmt.setString(1, com.kamotomo.pos.utils.SecurityUtil.hashPassword(newPasswordField.getText().trim()));
                    stmt.setString(2, username);
                    stmt.executeUpdate();

                    com.kamotomo.pos.utils.SystemLogger.logAction("Security", "Password successfully reset via recovery tool for admin: " + username);
                    showThemedAlert(Alert.AlertType.INFORMATION, "Success", "Your password has been successfully reset. You may now log in.");
                } catch (Exception e) {
                    showThemedAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update the password.");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void applyThemeToDialog(DialogPane dialogPane) {
        if (usernameField == null || usernameField.getScene() == null) return;

        dialogPane.getStylesheets().clear();
        dialogPane.getStylesheets().add(getClass().getResource("/light-theme.css").toExternalForm());

        if (!dialogPane.getStyleClass().contains("custom-dialog")) {
            dialogPane.getStyleClass().addAll("custom-dialog", "root");
        }
    }

    private void showThemedAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }
}