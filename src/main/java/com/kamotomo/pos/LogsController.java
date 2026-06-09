package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.utils.SystemLogger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class LogsController {

    @FXML private TableView<LogEntry> logsTable;
    @FXML private TableColumn<LogEntry, String> colTime;
    @FXML private TableColumn<LogEntry, String> colUser;
    @FXML private TableColumn<LogEntry, String> colAction;
    @FXML private TableColumn<LogEntry, String> colDetails;
    @FXML private TableColumn<LogEntry, LogEntry> colActionBtn;

    // The Intuitive Filter Elements
    @FXML private TextField searchField;
    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<String> actionFilter;
    @FXML private ComboBox<String> userFilter;

    private ObservableList<LogEntry> masterData = FXCollections.observableArrayList();
    private FilteredList<LogEntry> filteredData;
    private boolean isAlertShowing = false;

    @FXML
    public void initialize() {
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));

        setupActionColumn();

        filteredData = new FilteredList<>(masterData, p -> true);
        logsTable.setItems(filteredData);

        loadLogs();

        // Bind all intuitive filters to the apply method
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (actionFilter != null) actionFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (userFilter != null) userFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupActionColumn() {
        if (colActionBtn == null) return;

        colActionBtn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colActionBtn.setCellFactory(tc -> new TableCell<LogEntry, LogEntry>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    // INCLUDES OFFLINE SYNC TRANSACTIONS
                } else if (item.getAction().toLowerCase().contains("transaction")) {
                    Button viewBtn = new Button("📄 Receipt");
                    viewBtn.setStyle("-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-primary; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-cursor: hand; -fx-font-size: 11px;");
                    viewBtn.setOnAction(e -> parseAndShowReceipt(item));
                    setGraphic(viewBtn);
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private void loadLogs() {
        masterData.clear();

        // Use Sets to automatically gather unique values for our intuitive filters
        java.util.Set<String> actionTypes = new java.util.TreeSet<>();
        java.util.Set<String> users = new java.util.TreeSet<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM `system_log` ORDER BY timestamp DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String timestamp = rs.getString("timestamp");
                String user = rs.getString("username");
                String action = rs.getString("action");
                String details = rs.getString("details");

                masterData.add(new LogEntry(timestamp, user, action, details));

                if (action != null) actionTypes.add(action);
                if (user != null) users.add(user);
            }
        } catch (Exception e) { e.printStackTrace(); }

        // Populate dropdowns dynamically based on what actually exists in the database
        if (actionFilter != null) {
            ObservableList<String> actionItems = FXCollections.observableArrayList("All Actions");
            actionItems.addAll(actionTypes);
            actionFilter.setItems(actionItems);
            actionFilter.getSelectionModel().selectFirst();
        }

        if (userFilter != null) {
            ObservableList<String> userItems = FXCollections.observableArrayList("All Users");
            userItems.addAll(users);
            userFilter.setItems(userItems);
            userFilter.getSelectionModel().selectFirst();
        }

        applyFilters();
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();
        String selectedAction = actionFilter != null ? actionFilter.getValue() : "All Actions";
        String selectedUser = userFilter != null ? userFilter.getValue() : "All Users";

        filteredData.setPredicate(log -> {
            boolean matchesSearch = search.isEmpty() || log.getDetails().toLowerCase().contains(search);

            boolean matchesDate = true;
            if (dateFilter.getValue() != null) {
                matchesDate = log.getTimestamp().startsWith(dateFilter.getValue().toString());
            }

            boolean matchesAction = selectedAction == null || selectedAction.equals("All Actions") || log.getAction().equalsIgnoreCase(selectedAction);
            boolean matchesUser = selectedUser == null || selectedUser.equals("All Users") || log.getUsername().equalsIgnoreCase(selectedUser);

            return matchesSearch && matchesDate && matchesAction && matchesUser;
        });
    }

    private void parseAndShowReceipt(LogEntry log) {
        try {
            int txId;
            String details = log.getDetails();

            // Handles both normal "Sale ID:" and "Offline Sale #" strings
            if (log.getAction().contains("Offline") && details.contains("#")) {
                String idPart = details.substring(details.indexOf("#") + 1);
                txId = Integer.parseInt(idPart.split(" ")[0].trim());
            } else if (details.contains("Sale ID:")) {
                String cleanDetails = details.replace("Sale ID: ", "");
                txId = Integer.parseInt(cleanDetails.split(" ")[0].trim());
            } else {
                throw new Exception("Unrecognized receipt log format.");
            }

            renderReceiptPopup(txId, log.getTimestamp(), log.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            showThemedAlert(Alert.AlertType.ERROR, "Parse Error", "Could not reconstruct the receipt for this specific log entry.");
        }
    }

    private void renderReceiptPopup(int txId, String timestamp, String cashier) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transaction Archive — Receipt Reprint");
        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);
        dialogPane.getStyleClass().add("custom-dialog");

        String paymentMethod = "Unknown";
        double discountAmount = 0.0;
        String discountReason = "";
        double finalTotal = 0.0;
        double tenderedAmount = 0.0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT paymentMethod, discountAmount, discountReason, totalAmount, amountTendered FROM TRANSACTION WHERE transactionID = ?");
            stmt.setInt(1, txId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                paymentMethod = rs.getString("paymentMethod");
                discountAmount = rs.getDouble("discountAmount");
                discountReason = rs.getString("discountReason");
                finalTotal = rs.getDouble("totalAmount");
                tenderedAmount = rs.getDouble("amountTendered");
            }
        } catch (Exception e) { e.printStackTrace(); }

        VBox receiptBox = new VBox(8);
        receiptBox.setAlignment(Pos.TOP_CENTER);
        receiptBox.setPrefWidth(250);
        receiptBox.setMaxWidth(250);
        receiptBox.setPadding(new Insets(10));
        receiptBox.setStyle("-fx-background-color: white; -fx-font-family: 'Monospaced'; -fx-font-size: 11px; -fx-text-fill: black;");

        Label title = new Label("KaMotoMo\nMotor Parts");
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label details = new Label("TXN: " + txId + " [REPRINT]\nDate: " + timestamp + "\nCashier: " + cashier + "\nPay: " + paymentMethod);
        details.setAlignment(Pos.CENTER_LEFT);
        details.setMaxWidth(Double.MAX_VALUE);

        Label divider1 = new Label("------------------------");
        Label divider2 = new Label("------------------------");

        VBox itemsBox = new VBox(2);
        double rawSubtotal = 0.0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.productName, td.quantity, td.subtotal " +
                    "FROM TRANSACTION_DETAILS td " +
                    "JOIN PRODUCT p ON td.productID = p.productID " +
                    "WHERE td.transactionID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, txId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String pName = rs.getString("productName");
                int qty = rs.getInt("quantity");
                double subTotal = rs.getDouble("subtotal");

                rawSubtotal += subTotal;
                double unitPrice = subTotal / qty;

                Label nameRow = new Label(pName);
                nameRow.setWrapText(true);
                nameRow.setMaxWidth(230);
                Label priceRow = new Label(String.format("  %dx @ ₱%.2f = ₱%.2f", qty, unitPrice, subTotal));

                itemsBox.getChildren().addAll(nameRow, priceRow);
            }
        } catch (Exception ex) {
            itemsBox.getChildren().add(new Label("Error fetching line items."));
        }

        receiptBox.getChildren().addAll(title, divider1, details, divider2, itemsBox, new Label(" "));

        if (discountAmount > 0) {
            receiptBox.getChildren().add(new Label(String.format("SUBTOTAL:   ₱%.2f", rawSubtotal)));
            receiptBox.getChildren().add(new Label(String.format("DISCOUNT:  -₱%.2f", discountAmount)));
            receiptBox.getChildren().add(new Label(String.format("REASON:     %s", discountReason != null && !discountReason.isEmpty() ? discountReason : "Manual")));
            receiptBox.getChildren().add(new Label("------------------------"));
        }

        Label totalLbl = new Label(String.format("TOTAL:      ₱%.2f", finalTotal));
        totalLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        if (tenderedAmount < finalTotal) tenderedAmount = finalTotal;

        double changeAmount = tenderedAmount - finalTotal;

        Label tenderLbl = new Label(String.format("Tendered:   ₱%.2f", tenderedAmount));
        Label changeLbl = new Label(String.format("Change:     ₱%.2f", changeAmount));

        receiptBox.getChildren().addAll(totalLbl, tenderLbl, changeLbl, new Label("------------------------"));

        // Match the dynamic VAT from POS
        double currentTaxRate = com.kamotomo.pos.utils.SystemSettings.getTaxRate();
        double currentVatableSales = finalTotal / (1 + currentTaxRate);
        double currentVatAmount = finalTotal - currentVatableSales;
        int displayTaxPercent = (int) Math.round(currentTaxRate * 100);

        Label vatableLbl = new Label(String.format("VATable Sales: ₱%.2f", currentVatableSales));
        Label vatLbl = new Label(String.format("VAT (%d%%):     ₱%.2f", displayTaxPercent, currentVatAmount));
        receiptBox.getChildren().addAll(vatableLbl, vatLbl);

        receiptBox.getChildren().add(new Label("\nThank you!\nIngat sa kalsada! 🏍"));

        HBox containerBox = new HBox(receiptBox);
        containerBox.setAlignment(Pos.CENTER);
        containerBox.setPadding(new Insets(20));
        dialogPane.setContent(containerBox);

        ButtonType printType = new ButtonType("🖨 Print Reprint", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(printType, closeType);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == printType) {
                javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(dialog.getOwner())) {
                    boolean success = job.printPage(receiptBox);
                    if (success) job.endJob();

                    com.kamotomo.pos.utils.SystemLogger.logAction("Transaction", "Reprinted receipt for Sale ID: " + txId);
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    protected void onResetFilters() {
        searchField.clear();
        dateFilter.setValue(null);
        if (actionFilter != null) actionFilter.getSelectionModel().select("All Actions");
        if (userFilter != null) userFilter.getSelectionModel().select("All Users");
    }

    @FXML
    protected void onExportLogs() {
        // Only outputs what is currently filtered and visible on screen!
        if (filteredData.isEmpty()) {
            showThemedAlert(Alert.AlertType.WARNING, "No Data", "There are no logs matching your current filters to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export System Logs");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("KaMotoMo_AuditLogs.csv");
        File file = fileChooser.showSaveDialog(logsTable.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("Timestamp,User,Action,Details");
                for (LogEntry log : filteredData) {
                    String cleanDetails = log.getDetails() != null ? log.getDetails().replace("\"", "\"\"") : "";
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            log.getTimestamp(), log.getUsername(), log.getAction(), cleanDetails);
                }
                SystemLogger.logAction("Export", "Exported filtered system logs to CSV.");
                showThemedAlert(Alert.AlertType.INFORMATION, "Export Successful", "Logs have been successfully exported to CSV.");
            } catch (Exception e) {
                e.printStackTrace();
                showThemedAlert(Alert.AlertType.ERROR, "Export Failed", "Could not write to the selected file.");
            }
        }
    }

    @FXML
    protected void onClearLogs() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning: Clear All Logs");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to permanently delete all logs?");

        applyThemeToDialog(alert.getDialogPane());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.prepareStatement("TRUNCATE TABLE `system_log`").executeUpdate();
                SystemLogger.logAction("System", "Administrator truncated all system logs.");
                loadLogs();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void showThemedAlert(Alert.AlertType type, String title, String message) {
        if (isAlertShowing) return;
        isAlertShowing = true;

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
        isAlertShowing = false;
    }

    private void applyThemeToDialog(DialogPane dialogPane) {
        if (logsTable == null || logsTable.getScene() == null) return;

        String activeThemeUrl = "";
        javafx.scene.Parent current = logsTable;

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
            for (String stylesheet : logsTable.getScene().getStylesheets()) {
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

    public static class LogEntry {
        private final String timestamp, username, action, details;
        public LogEntry(String timestamp, String username, String action, String details) {
            this.timestamp = timestamp; this.username = username; this.action = action; this.details = details;
        }
        public String getTimestamp() { return timestamp; }
        public String getUsername() { return username; }
        public String getAction() { return action; }
        public String getDetails() { return details; }
    }
}