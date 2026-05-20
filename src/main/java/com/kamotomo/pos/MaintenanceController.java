package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.utils.SystemLogger;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class MaintenanceController {

    private void showThemedAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        DashboardController.applyThemeToDialog(dialogPane);
        dialogPane.getStyleClass().add("custom-dialog");

        alert.showAndWait();
    }

    @FXML
    protected void onBackupDatabase() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Database Backup");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL File", "*.sql"));

        String dateStamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        fileChooser.setInitialFileName("KaMotoMo_Backup_" + dateStamp + ".sql");

        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            // A hardcoded list of your tables. If you add more tables later, add them here!
            String[] tables = {"user", "PRODUCT", "PURCHASE_ORDER", "PO_ITEM", "TRANSACTION", "TRANSACTION_DETAILS", "system_log"};

            try (Connection conn = DatabaseConnection.getConnection();
                 PrintWriter writer = new PrintWriter(file)) {

                writer.println("-- KaMotoMo POS System Full Backup");
                writer.println("-- Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.println("SET FOREIGN_KEY_CHECKS=0;\n");

                for (String table : tables) {
                    writer.println("-- Dump for table: " + table);

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
                                    // Escape single quotes for SQL safety
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
                showThemedAlert(Alert.AlertType.INFORMATION, "Backup Successful", "Database backup saved to: " + file.getName());

            } catch (Exception e) {
                e.printStackTrace();
                showThemedAlert(Alert.AlertType.ERROR, "Backup Failed", "An error occurred while generating the backup.");
            }
        }
    }

    @FXML
    protected void onRestoreDatabase() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Restore");
        confirm.setHeaderText("OVERWRITE WARNING");
        confirm.setContentText("This feature requires running the generated .sql file directly in MySQL Workbench or via the MySQL command line interface. Opening a file here will not automatically execute the script for security reasons.\n\nDo you want to log a database restoration attempt?");

        DashboardController.applyThemeToDialog(confirm.getDialogPane());
        confirm.getDialogPane().getStyleClass().add("custom-dialog");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SystemLogger.logAction("Maintenance", "Admin initiated database restoration protocol.");
        }
    }
}