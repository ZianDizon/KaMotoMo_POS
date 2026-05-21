package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.utils.SystemLogger;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class MaintenanceController {

    @FXML private VBox maintenanceRoot;

    // --- THE TARGETED THEME HUNTER ---
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

    @FXML
    protected void onRestoreDatabase() {
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
                    // VERIFY THIS PATH MATCHES YOUR PROGRAM FILES FOLDER EXACTLY
                    String mysqlPath = "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe";

                    // 1. Notice how clean the command array is now! No "-e" or "source"
                    String[] command = {
                            mysqlPath,
                            "--user=root",
                            "--password=admin",
                            "kamotomo_db"
                    };

                    ProcessBuilder pb = new ProcessBuilder(command);

                    // 2. THE MAGIC LINE: Pipe the .sql file directly into the database engine
                    pb.redirectInput(file);

                    Process process = pb.start();

                    // --- CAPTURE MYSQL'S HIDDEN ERROR MESSAGE ---
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()));
                    StringBuilder errorMessage = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorMessage.append(line).append("\n");
                    }

                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        com.kamotomo.pos.utils.SystemLogger.logAction("Maintenance", "Database restored from: " + file.getName());
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
                        System.out.println("Skipped table " + table + " (may not exist yet).");
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
}