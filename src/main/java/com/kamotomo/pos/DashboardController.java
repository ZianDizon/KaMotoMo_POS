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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private ScheduledExecutorService autoSyncService;

    // --- NEW: Heartbeat Status Tracker ---
    private boolean isDbOnline = true;

    @FXML
    public void initialize() {
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

        startAutoSyncDaemon();

        onOverviewButtonClick();
    }

    // --- REVISION: Dynamic Toast Notifications ---
    private void showToastNotification(String message, String bgColor) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Label toastLabel = new javafx.scene.control.Label(message);
            toastLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-padding: 12 24; -fx-background-radius: 30; -fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 4);");

            StackPane.setAlignment(toastLabel, javafx.geometry.Pos.TOP_CENTER);
            StackPane.setMargin(toastLabel, new javafx.geometry.Insets(30, 0, 0, 0));

            contentArea.getChildren().add(toastLabel);

            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), toastLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), toastLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(javafx.util.Duration.seconds(4));
            fadeOut.setOnFinished(e -> contentArea.getChildren().remove(toastLabel));

            fadeIn.play();
            fadeIn.setOnFinished(e -> fadeOut.play());
        });
    }

    // --- REVISION: The Heartbeat Monitor ---
    private void startAutoSyncDaemon() {
        autoSyncService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        // Pings the database every 5 seconds
        autoSyncService.scheduleAtFixedRate(() -> {
            boolean currentStatus = false;
            try (Connection conn = com.kamotomo.pos.database.DatabaseConnection.getConnection()) {
                if (conn != null && conn.isValid(2)) {
                    currentStatus = true;
                }
            } catch (Exception e) {
                // Connection failed
            }

            final boolean isNowOnline = currentStatus;

            // If the state has changed since the last check, update the UI
            if (isNowOnline != isDbOnline) {
                isDbOnline = isNowOnline;
                javafx.application.Platform.runLater(() -> handleConnectionChange(isNowOnline));
            }

            // Only attempt file sync if we are online
            if (isNowOnline) {
                checkAndSyncOfflineData();
            }
        }, 2, 5, TimeUnit.SECONDS);
    }

    // --- NEW: Dynamic Navigation Lockdown ---
    private void handleConnectionChange(boolean isOnline) {
        if (!isOnline) {
            // OFFLINE LOCKDOWN
            overviewBtn.setDisable(true);
            inventoryBtn.setDisable(true);
            stockMonitorBtn.setDisable(true);
            reportsBtn.setDisable(true);
            usersBtn.setDisable(true);
            logsBtn.setDisable(true);
            maintenanceBtn.setDisable(true);

            showToastNotification("⚠️ Database Offline: Restricted to POS Mode.", "#f03d3d");

            // Auto-redirect if they are somewhere they shouldn't be
            String currentTitle = screenTitle != null ? screenTitle.getText() : "";
            if (!currentTitle.equals("POINT OF SALE") && !currentTitle.equals("HELP & SUPPORT")) {
                onPosButtonClick();
            }
        } else {
            // ONLINE RESTORE
            overviewBtn.setDisable(false);
            inventoryBtn.setDisable(false);
            stockMonitorBtn.setDisable(false);
            reportsBtn.setDisable(false);
            usersBtn.setDisable(false);
            logsBtn.setDisable(false);
            maintenanceBtn.setDisable(false);

            enforceRoleRestrictions(); // Re-hide admin buttons if user is an Employee
            showToastNotification("✅ Database Online: Full access restored.", "#3adf8a");
        }
    }

    private void checkAndSyncOfflineData() {
        File file = new File("offline_sales.csv");
        if (!file.exists()) return;

        try (Connection conn = com.kamotomo.pos.database.DatabaseConnection.getConnection()) {
            if (conn == null || !conn.isValid(2)) return;

            conn.setAutoCommit(false);
            int successCount = 0;
            List<String> detailedReceiptLogs = new ArrayList<>();

            try (Scanner scanner = new Scanner(file)) {
                String txSql = "INSERT INTO TRANSACTION (transactionDate, userID, totalAmount, paymentMethod, discountAmount, discountReason, amountTendered, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'Completed')";
                PreparedStatement txStmt = conn.prepareStatement(txSql, java.sql.Statement.RETURN_GENERATED_KEYS);

                String detailsSql = "INSERT INTO TRANSACTION_DETAILS (transactionID, productID, quantity, subtotal) VALUES (?, ?, ?, ?)";
                PreparedStatement detailsStmt =prepareStatement(conn, detailsSql);

                String stockSql = "UPDATE PRODUCT SET stockQuantity = stockQuantity - ? WHERE productID = ?";
                PreparedStatement stockStmt = prepareStatement(conn, stockSql);

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split("\\|");
                    if (parts.length < 9) continue;

                    String dateStr = parts[0];
                    int userId = Integer.parseInt(parts[1]);
                    double total = Double.parseDouble(parts[2]);
                    double tendered = Double.parseDouble(parts[3]);
                    double discount = Double.parseDouble(parts[4]);
                    String reason = parts[5];
                    String payMethod = parts[6];
                    String itemsData = parts[8];

                    txStmt.setString(1, dateStr);
                    txStmt.setInt(2, userId);
                    txStmt.setDouble(3, total);
                    txStmt.setString(4, payMethod);
                    txStmt.setDouble(5, discount);
                    txStmt.setString(6, reason);
                    txStmt.setDouble(7, tendered);
                    txStmt.executeUpdate();

                    ResultSet rs = txStmt.getGeneratedKeys();
                    int newTxId = 0;
                    if (rs.next()) newTxId = rs.getInt(1);

                    StringBuilder digitalReceipt = new StringBuilder("Offline Sale #").append(newTxId)
                            .append(" (").append(dateStr).append(") | Total: ₱").append(String.format("%.2f", total)).append(" | Items: [");

                    String[] items = itemsData.split(";");
                    for (String itemStr : items) {
                        if (itemStr.isEmpty()) continue;
                        String[] itemParts = itemStr.split(",");
                        int prodId = Integer.parseInt(itemParts[0]);
                        int qty = Integer.parseInt(itemParts[1]);
                        double sub = Double.parseDouble(itemParts[2]);

                        detailsStmt.setInt(1, newTxId);
                        detailsStmt.setInt(2, prodId);
                        detailsStmt.setInt(3, qty);
                        detailsStmt.setDouble(4, sub);
                        detailsStmt.addBatch();

                        stockStmt.setInt(1, qty);
                        stockStmt.setInt(2, prodId);
                        stockStmt.addBatch();

                        digitalReceipt.append(" ID-").append(prodId).append("(").append(qty).append("x) ");
                    }
                    digitalReceipt.append("]");
                    detailedReceiptLogs.add(digitalReceipt.toString());

                    detailsStmt.executeBatch();
                    stockStmt.executeBatch();
                    successCount++;
                }

                conn.commit();

                for (String receiptLog : detailedReceiptLogs) {
                    com.kamotomo.pos.utils.SystemLogger.logAction("Transaction (Offline)", receiptLog);
                }

                com.kamotomo.pos.utils.SystemLogger.logAction("System Auto-Sync", "Background thread pushed " + successCount + " offline transactions.");

                final int finalCount = successCount;
                showToastNotification("🔄 Cloud Sync Complete: " + finalCount + " offline records restored!", "#8b5cf6");

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

            File archiveFile = new File("offline_sales_synced_" + System.currentTimeMillis() + ".csv");
            file.renameTo(archiveFile);

        } catch (Exception e) {
            // DB still off, fail silently
        }
    }

    // Helper to prevent repeated code logic
    private PreparedStatement prepareStatement(Connection conn, String sql) throws java.sql.SQLException {
        return conn.prepareStatement(sql);
    }

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
            if ("Employee".equalsIgnoreCase(role)) {
                userRoleLabel.setStyle("-fx-text-fill: -kmtm-text-dim; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
            }
        }
    }

    private void enforceRoleRestrictions() {
        String role = com.kamotomo.pos.utils.UserSession.getInstance().getRole();

        if ("Employee".equalsIgnoreCase(role)) {
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
            if (maintenanceBtn != null) {
                maintenanceBtn.setVisible(false);
                maintenanceBtn.setManaged(false);
            }
        }
    }

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
                if (autoSyncService != null && !autoSyncService.isShutdown()) {
                    autoSyncService.shutdownNow();
                }

                com.kamotomo.pos.utils.SystemLogger.logAction("System", "User logged out.");
                com.kamotomo.pos.utils.UserSession.getInstance().clearSession();

                FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 400, 500);

                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/light-theme.css").toExternalForm());

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