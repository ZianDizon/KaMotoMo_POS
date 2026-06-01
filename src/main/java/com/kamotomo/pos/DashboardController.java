package com.kamotomo.pos;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.util.Optional;

public class DashboardController {

    @FXML private StackPane contentArea;

    // Sidebar buttons
    @FXML private Button overviewBtn;
    @FXML private Button posBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button stockMonitorBtn;
    @FXML private Button reportsBtn;
    @FXML private Button usersBtn;
    @FXML private Button logsBtn;
    @FXML private Button maintenanceBtn;
    @FXML private Button helpBtn;

    // Sidebar Dynamic Profile Labels
    @FXML private javafx.scene.control.Label avatarLabel;
    @FXML private javafx.scene.control.Label userNameLabel;
    @FXML private javafx.scene.control.Label userRoleLabel;

    // The ADMIN text above the restricted buttons
    @FXML private javafx.scene.control.Label adminSectionLabel;

    @FXML private javafx.scene.control.Label welcomeLabel;
    @FXML private javafx.scene.control.Label clockLabel;
    @FXML private javafx.scene.control.Label screenTitle;

    @FXML
    public void initialize() {
        // --- REAL-TIME CLOCK SETUP ---
        if (clockLabel != null) {
            clockLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: -kmtm-primary; -fx-font-weight: bold;");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy, hh:mm:ss a");
            Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
                clockLabel.setText(LocalDateTime.now().format(formatter));
            }), new KeyFrame(Duration.seconds(1)));
            clock.setCycleCount(Animation.INDEFINITE);
            clock.play();
        }

        setupDynamicUserProfile();
        enforceRoleRestrictions();

        onOverviewButtonClick();
    }

    // --- DYNAMIC USER PROFILE UPDATE ---
    private void setupDynamicUserProfile() {
        String name = com.kamotomo.pos.utils.UserSession.getInstance().getName();
        String role = com.kamotomo.pos.utils.UserSession.getInstance().getRole();
        String username = com.kamotomo.pos.utils.UserSession.getInstance().getUsername();

        String displayName = (name != null && !name.trim().isEmpty()) ? name : username;

        if (userNameLabel != null) userNameLabel.setText(displayName);
        if (avatarLabel != null && displayName.length() > 0) {
            avatarLabel.setText(displayName.substring(0, 1).toUpperCase());
        }
        if (userRoleLabel != null) {
            userRoleLabel.setText(role.toUpperCase());
            // Make the employee role text look less "admin-like" (dimmer color)
            if ("Employee".equalsIgnoreCase(role)) {
                userRoleLabel.setStyle("-fx-text-fill: -kmtm-text-dim; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
            }
        }
    }

    // --- ROLE-BASED ACCESS CONTROL (RBAC) ---
    private void enforceRoleRestrictions() {
        String role = com.kamotomo.pos.utils.UserSession.getInstance().getRole();

        if ("Employee".equalsIgnoreCase(role)) {
            // Hide the actual section text
            if (adminSectionLabel != null) {
                adminSectionLabel.setVisible(false);
                adminSectionLabel.setManaged(false);
            }
            if (reportsBtn != null) {
                reportsBtn.setVisible(false);
                reportsBtn.setManaged(false);
            }
            if (usersBtn != null) {
                usersBtn.setVisible(false);
                usersBtn.setManaged(false);
            }
            if (logsBtn != null) {
                logsBtn.setVisible(false);
                logsBtn.setManaged(false);
            }
            // --- ADDED MAINTENANCE BUTTON LOCKDOWN ---
            if (maintenanceBtn != null) {
                maintenanceBtn.setVisible(false);
                maintenanceBtn.setManaged(false);
            }
        }
    }

    // --- VISUAL ROUTING LOGIC ---
    private void setActiveButton(Button activeBtn) {
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: -kmtm-text-dim; -fx-border-width: 0 0 0 4; -fx-border-color: transparent; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 20 12 25; -fx-cursor: hand; -fx-font-family: 'IBM Plex Sans', sans-serif;";
        String activeStyle = "-fx-background-color: -kmtm-primary-glow; -fx-text-fill: -kmtm-primary; -fx-border-width: 0 0 0 4; -fx-border-color: -kmtm-primary; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 20 12 25; -fx-cursor: hand; -fx-font-family: 'IBM Plex Sans', sans-serif;";

        if (overviewBtn != null) overviewBtn.setStyle(inactiveStyle);
        if (posBtn != null) posBtn.setStyle(inactiveStyle);
        if (inventoryBtn != null) inventoryBtn.setStyle(inactiveStyle);
        if (stockMonitorBtn != null) stockMonitorBtn.setStyle(inactiveStyle);
        if (reportsBtn != null) reportsBtn.setStyle(inactiveStyle);
        if (usersBtn != null) usersBtn.setStyle(inactiveStyle);
        if (logsBtn != null) logsBtn.setStyle(inactiveStyle);
        if (maintenanceBtn != null) maintenanceBtn.setStyle(inactiveStyle);
        if (helpBtn != null) helpBtn.setStyle(inactiveStyle);

        if (activeBtn != null) activeBtn.setStyle(activeStyle);
    }

    private void loadModule(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFileName));
            Node module = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(module);
        } catch (IOException e) {
            System.out.println("Could not load module: " + fxmlFileName);
            e.printStackTrace();
        }
    }

    // --- BUTTON CLICK LISTENERS ---
    @FXML protected void onOverviewButtonClick() { setActiveButton(overviewBtn); if (screenTitle != null) screenTitle.setText("DASHBOARD"); loadModule("overview-view.fxml"); }
    @FXML protected void onPosButtonClick() { setActiveButton(posBtn); if (screenTitle != null) screenTitle.setText("POINT OF SALE"); loadModule("pos-view.fxml"); }
    @FXML protected void onInventoryButtonClick() { setActiveButton(inventoryBtn); if (screenTitle != null) screenTitle.setText("INVENTORY"); loadModule("inventory-view.fxml"); }
    @FXML protected void onStockMonitorButtonClick() { setActiveButton(stockMonitorBtn); if (screenTitle != null) screenTitle.setText("STOCK MONITOR"); loadModule("stock-monitor-view.fxml"); }
    @FXML protected void onReportsButtonClick() { setActiveButton(reportsBtn); if (screenTitle != null) screenTitle.setText("REPORTS"); loadModule("reports-view.fxml"); }
    @FXML protected void onUsersButtonClick() { setActiveButton(usersBtn); if (screenTitle != null) screenTitle.setText("USERS"); loadModule("users-view.fxml"); }
    @FXML protected void onLogsButtonClick() { setActiveButton(logsBtn); if (screenTitle != null) screenTitle.setText("SYSTEM LOGS"); loadModule("logs-view.fxml"); }
    @FXML
    protected void onMaintenanceButtonClick() {
        setActiveButton(maintenanceBtn);
        if (screenTitle != null) screenTitle.setText("SYSTEM MAINTENANCE");
        loadModule("maintenance-view.fxml");
    }

    @FXML
    protected void onHelpButtonClick() {
        setActiveButton(helpBtn);
        if (screenTitle != null) screenTitle.setText("HELP & SUPPORT");
        loadModule("help-view.fxml");
    }

    public void setWelcomeMessage(String username, String role) {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + username + "!");
        }
    }

    // --- UPDATED LOGOUT METHOD WITH CONFIRMATION ---
    @FXML
    protected void onLogoutClick() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Logout");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to log out of the system?");

        applyThemeToDialog(confirm.getDialogPane());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // 1. Log the action
                com.kamotomo.pos.utils.SystemLogger.logAction("System", "User logged out.");

                // 2. Clear the session entirely
                com.kamotomo.pos.utils.UserSession.getInstance().clearSession();

                // 3. Load the Login Screen
                FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 400, 500);

                // --- FIX: THE STRICT LIGHT MODE ENFORCER ---
                // Ignore all user preferences, clear inherited styles, and forcefully apply light theme
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/light-theme.css").toExternalForm());

                // 5. Setup the stage
                Stage stage = (Stage) contentArea.getScene().getWindow();
                stage.setMaximized(false);
                stage.setWidth(400);
                stage.setHeight(500);

                stage.setScene(scene);
                stage.centerOnScreen();
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/motorcycle.png")));
                stage.setTitle("KaMotoMo - Login");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    protected void onThemeToggleClick() {
        String currentTheme = com.kamotomo.pos.utils.UserSession.getInstance().getThemePreference();
        if (currentTheme == null) currentTheme = "light";

        String newTheme = currentTheme.equals("dark") ? "light" : "dark";

        javafx.scene.Parent rootNode = contentArea.getScene().getRoot();
        rootNode.getStylesheets().clear();

        if (newTheme.equals("light")) {
            rootNode.getStylesheets().add(getClass().getResource("/light-theme.css").toExternalForm());
        } else {
            rootNode.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        }

        com.kamotomo.pos.utils.UserSession.getInstance().setThemePreference(newTheme);

        try (java.sql.Connection conn = com.kamotomo.pos.database.DatabaseConnection.getConnection()) {
            String sql = "UPDATE `user` SET themePreference = ? WHERE userID = ?";
            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newTheme);
            stmt.setInt(2, com.kamotomo.pos.utils.UserSession.getInstance().getUserId());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- THE TARGETED THEME HUNTER (Replaced old static method) ---
    private void applyThemeToDialog(DialogPane dialogPane) {
        if (contentArea == null || contentArea.getScene() == null) return;

        String activeThemeUrl = "";
        javafx.scene.Parent current = contentArea;

        while (current != null) {
            for (String stylesheet : current.getStylesheets()) {
                if (stylesheet.contains("dark-theme.css") || stylesheet.contains("light-theme.css")) {
                    activeThemeUrl = stylesheet;
                    break;
                }
            }
            if (!activeThemeUrl.isEmpty()) break;
            current = current.getParent();
        }

        if (activeThemeUrl.isEmpty()) {
            for (String stylesheet : contentArea.getScene().getStylesheets()) {
                if (stylesheet.contains("dark-theme.css") || stylesheet.contains("light-theme.css")) {
                    activeThemeUrl = stylesheet;
                    break;
                }
            }
        }

        dialogPane.getStylesheets().clear();
        if (!activeThemeUrl.isEmpty()) {
            dialogPane.getStylesheets().add(activeThemeUrl);
        }

        if (!dialogPane.getStyleClass().contains("custom-dialog")) {
            dialogPane.getStyleClass().addAll("custom-dialog", "root");
        }
    }
}