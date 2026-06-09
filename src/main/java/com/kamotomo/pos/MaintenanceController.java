package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.utils.SystemLogger;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.geometry.Insets;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Scanner;

public class MaintenanceController {

    @FXML private VBox maintenanceRoot;
    @FXML private TextField vatRateField;
    @FXML private TextField scPwdDiscountField;
    @FXML private TextField wholesaleDiscountField;
    @FXML private TextField employeeDiscountField;

    @FXML
    public void initialize() {
        if (vatRateField != null && scPwdDiscountField != null) {
            double currentVat = com.kamotomo.pos.utils.SystemSettings.getTaxRate() * 100;
            double currentScPwd = com.kamotomo.pos.utils.SystemSettings.getScPwdDiscountRate() * 100;
            double currentWholesale = com.kamotomo.pos.utils.SystemSettings.getWholesaleDiscountRate() * 100;
            double currentEmp = com.kamotomo.pos.utils.SystemSettings.getEmployeeDiscountRate() * 100;

            vatRateField.setText(String.format("%.0f", currentVat));
            scPwdDiscountField.setText(String.format("%.0f", currentScPwd));
            wholesaleDiscountField.setText(String.format("%.0f", currentWholesale));
            employeeDiscountField.setText(String.format("%.0f", currentEmp));

            applyNumericValidation(vatRateField);
            applyNumericValidation(scPwdDiscountField);
            applyNumericValidation(wholesaleDiscountField);
            applyNumericValidation(employeeDiscountField);
        }
    }

    private void applyNumericValidation(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                field.setText(oldVal);
            }
        });
    }

    @FXML
    protected void onSaveConfigClick() {
        try {
            double newVat = Double.parseDouble(vatRateField.getText()) / 100.0;
            double newScPwd = Double.parseDouble(scPwdDiscountField.getText()) / 100.0;
            double newWholesale = Double.parseDouble(wholesaleDiscountField.getText()) / 100.0;
            double newEmp = Double.parseDouble(employeeDiscountField.getText()) / 100.0;

            if (newVat < 0 || newVat > 1.0 || newScPwd < 0 || newScPwd > 1.0 || newWholesale < 0 || newWholesale > 1.0 || newEmp < 0 || newEmp > 1.0) {
                showThemedAlert(Alert.AlertType.WARNING, "Invalid Input", "Rates must be between 0 and 100 percent.");
                return;
            }

            com.kamotomo.pos.utils.SystemSettings.setTaxRate(newVat);
            com.kamotomo.pos.utils.SystemSettings.setScPwdDiscountRate(newScPwd);
            com.kamotomo.pos.utils.SystemSettings.setWholesaleDiscountRate(newWholesale);
            com.kamotomo.pos.utils.SystemSettings.setEmployeeDiscountRate(newEmp);

            SystemLogger.logAction("System Configuration", "Admin updated system tax and discount rates.");
            showThemedAlert(Alert.AlertType.INFORMATION, "Configuration Saved", "System config successfully updated.");

        } catch (NumberFormatException e) {
            showThemedAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numeric percentages.");
        }
    }

    // --- NEW: PASSWORD VERIFICATION GATEWAY (SHA-256 UPGRADED) ---
    private boolean verifyAdminPassword() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Administrator Authentication");
        dialog.setHeaderText("Critical System Action\nPlease verify your password to proceed.");

        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        ButtonType confirmType = new ButtonType("Authenticate", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(confirmType, ButtonType.CANCEL);

        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("Enter your password");
        pwdField.setStyle("-fx-background-color: -kmtm-surface; -fx-text-fill: -kmtm-text; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-padding: 10;");

        VBox vbox = new VBox(pwdField);
        vbox.setPadding(new Insets(20, 0, 0, 0));
        dialogPane.setContent(vbox);

        javafx.application.Platform.runLater(pwdField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmType) {
                return pwdField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String enteredPwd = result.get();
            int userId = com.kamotomo.pos.utils.UserSession.getInstance().getUserId();

            try (Connection conn = DatabaseConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT password FROM user WHERE userID = ?");
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String dbPassword = rs.getString("password");

                    // --- THE FIX: Hash the input before comparing! ---
                    String hashedInput = hashPasswordSHA256(enteredPwd);

                    if (hashedInput != null && dbPassword.equals(hashedInput)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            showThemedAlert(Alert.AlertType.ERROR, "Authentication Failed", "Incorrect administrator password. Access denied.");
        }
        return false;
    }

    @FXML
    protected void onRestoreDatabase() {
        if (!verifyAdminPassword()) return; // Abort if password fails

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Database Backup File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL File", "*.sql"));

        File file = fileChooser.showOpenDialog(maintenanceRoot.getScene().getWindow());

        if (file != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Restore");
            confirm.setHeaderText(null);
            confirm.setContentText("WARNING: This will completely overwrite the current database with the selected backup.\n\nAre you sure you want to proceed?");

            applyThemeToDialog(confirm.getDialogPane());

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    String mysqlPath = "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe";
                    String[] command = { mysqlPath, "--user=root", "--password=admin", "kamotomo_db" };

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectInput(file);
                    Process process = pb.start();

                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()));
                    StringBuilder errorMessage = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorMessage.append(line).append("\n");
                    }

                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        SystemLogger.logAction("Maintenance", "Database restored from: " + file.getName());
                        showThemedAlert(Alert.AlertType.INFORMATION, "Restore Successful", "The database has been successfully restored from the backup.\n\nPlease restart the application to reflect the changes.");
                    } else {
                        String finalError = errorMessage.toString().isEmpty() ? "Unknown Error." : errorMessage.toString();
                        showThemedAlert(Alert.AlertType.ERROR, "MySQL Rejected File", "Reason:\n" + finalError);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    showThemedAlert(Alert.AlertType.ERROR, "System Error", "Could not execute the restoration process.");
                }
            }
        }
    }

    @FXML
    protected void onBackupDatabase() {
        if (!verifyAdminPassword()) return; // Abort if password fails

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Database Backup");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL File", "*.sql"));

        String dateStamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        fileChooser.setInitialFileName("KaMotoMo_Backup_" + dateStamp + ".sql");

        File file = fileChooser.showSaveDialog(maintenanceRoot.getScene().getWindow());

        if (file != null) {
            String[] tables = {"user", "PRODUCT", "PURCHASE_ORDER", "PO_ITEM", "TRANSACTION", "TRANSACTION_DETAILS", "system_log"};

            try (Connection conn = DatabaseConnection.getConnection();
                 PrintWriter writer = new PrintWriter(file)) {

                writer.println("-- KaMotoMo POS System Full Backup");
                writer.println("-- Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.println("SET FOREIGN_KEY_CHECKS=0;\n");

                for (String table : tables) {
                    writer.println("-- Dump for table: " + table);
                    writer.println("TRUNCATE TABLE `" + table + "`;");

                    try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM `" + table + "`");
                         ResultSet rs = stmt.executeQuery()) {

                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();

                        while (rs.next()) {
                            StringBuilder insert = new StringBuilder("INSERT INTO `" + table + "` VALUES (");
                            for (int i = 1; i <= colCount; i++) {
                                Object obj = rs.getObject(i);
                                if (obj == null) {
                                    insert.append("NULL");
                                } else {
                                    String val = obj.toString().replace("'", "''");
                                    insert.append("'").append(val).append("'");
                                }
                                if (i < colCount) insert.append(", ");
                            }
                            insert.append(");");
                            writer.println(insert.toString());
                        }
                    } catch (Exception ex) {
                        System.out.println("Skipped table " + table);
                    }
                    writer.println("\n");
                }

                writer.println("SET FOREIGN_KEY_CHECKS=1;");
                SystemLogger.logAction("Maintenance", "Generated full system SQL backup.");
                showThemedAlert(Alert.AlertType.INFORMATION, "Backup Successful", "Database backup saved to:\n" + file.getName());

            } catch (Exception e) {
                e.printStackTrace();
                showThemedAlert(Alert.AlertType.ERROR, "Backup Failed", "An error occurred while generating the backup.");
            }
        }
    }

    @FXML
    protected void onSyncOfflineClick() {
        File file = new File("offline_sales.csv");
        if (!file.exists()) {
            showThemedAlert(Alert.AlertType.INFORMATION, "Sync Status", "No offline transactions found. System is up to date.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null || !conn.isValid(2)) {
                showThemedAlert(Alert.AlertType.ERROR, "Sync Failed", "Cannot sync. The database is still offline.");
                return;
            }

            conn.setAutoCommit(false);
            int successCount = 0;

            try (Scanner scanner = new Scanner(file)) {
                String txSql = "INSERT INTO TRANSACTION (transactionDate, userID, totalAmount, paymentMethod, discountAmount, discountReason, amountTendered, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'Completed')";
                PreparedStatement txStmt = conn.prepareStatement(txSql, java.sql.Statement.RETURN_GENERATED_KEYS);

                String detailsSql = "INSERT INTO TRANSACTION_DETAILS (transactionID, productID, quantity, subtotal) VALUES (?, ?, ?, ?)";
                PreparedStatement detailsStmt = conn.prepareStatement(detailsSql);

                String stockSql = "UPDATE PRODUCT SET stockQuantity = stockQuantity - ? WHERE productID = ?";
                PreparedStatement stockStmt = conn.prepareStatement(stockSql);

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
                    }

                    detailsStmt.executeBatch();
                    stockStmt.executeBatch();
                    successCount++;
                }

                conn.commit();
                SystemLogger.logAction("System Sync", "Synchronized " + successCount + " offline transactions via manual override.");
                showThemedAlert(Alert.AlertType.INFORMATION, "Sync Complete", "Successfully synchronized " + successCount + " offline transactions!");

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

            File archiveFile = new File("offline_sales_synced_" + System.currentTimeMillis() + ".csv");
            file.renameTo(archiveFile);

        } catch (Exception e) {
            e.printStackTrace();
            showThemedAlert(Alert.AlertType.ERROR, "Sync Error", "An error occurred while pushing offline data. Check logs.");
        }
    }

    private void applyThemeToDialog(DialogPane dialogPane) {
        if (maintenanceRoot == null || maintenanceRoot.getScene() == null) return;

        String activeThemeUrl = "";
        javafx.scene.Parent current = maintenanceRoot;

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
            for (String stylesheet : maintenanceRoot.getScene().getStylesheets()) {
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

    private void showThemedAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    // --- SECURITY: SHA-256 Hashing Utility (BASE64) ---
    private String hashPasswordSHA256(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Convert the raw bytes directly into a Base64 encoded string
            return java.util.Base64.getEncoder().encodeToString(hash);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}