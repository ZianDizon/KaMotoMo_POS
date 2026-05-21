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

    // Add this column to your logs-view.fxml!
    @FXML private TableColumn<LogEntry, LogEntry> colActionBtn;

    @FXML private TextField searchField;
    @FXML private DatePicker dateFilter;

    private ObservableList<LogEntry> masterData = FXCollections.observableArrayList();
    private FilteredList<LogEntry> filteredData;

    @FXML
    public void initialize() {
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));

        setupActionColumn();
        loadLogs();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupActionColumn() {
        if (colActionBtn == null) return; // Safety check if FXML isn't updated yet

        colActionBtn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colActionBtn.setCellFactory(tc -> new TableCell<LogEntry, LogEntry>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else if ("Transaction".equalsIgnoreCase(item.getAction())) {
                    Button viewBtn = new Button("📄 Receipt");
                    viewBtn.setStyle("-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-primary; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-cursor: hand; -fx-font-size: 11px;");
                    viewBtn.setOnAction(e -> parseAndShowReceipt(item)); // Pass the whole 'item' object
                    setGraphic(viewBtn);
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private void loadLogs() {
        masterData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM `system_log` ORDER BY timestamp DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                masterData.add(new LogEntry(
                        rs.getString("timestamp"), rs.getString("username"),
                        rs.getString("action"), rs.getString("details")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }

        filteredData = new FilteredList<>(masterData, p -> true);
        logsTable.setItems(filteredData);
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();
        filteredData.setPredicate(log -> {
            boolean matchesSearch = search.isEmpty() ||
                    log.getUsername().toLowerCase().contains(search) ||
                    log.getAction().toLowerCase().contains(search) ||
                    log.getDetails().toLowerCase().contains(search);

            boolean matchesDate = true;
            if (dateFilter.getValue() != null) {
                matchesDate = log.getTimestamp().startsWith(dateFilter.getValue().toString());
            }
            return matchesSearch && matchesDate;
        });
    }

    private void parseAndShowReceipt(LogEntry log) {
        try {
            // Extract the Transaction ID from the log details
            String cleanDetails = log.getDetails().replace("Sale ID: ", "");
            String idString = cleanDetails.split(" ")[0].trim();
            int txId = Integer.parseInt(idString);

            // Pass the ID, plus the date and cashier from the log entry
            renderReceiptPopup(txId, log.getTimestamp(), log.getUsername());
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not process receipt structure.");
            alert.showAndWait();
        }
    }

    private void renderReceiptPopup(int txId, String timestamp, String cashier) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transaction Archive — Receipt Reprint");
        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);
        dialogPane.getStyleClass().add("custom-dialog");

        // --- FETCH TRANSACTION METADATA ---
        String paymentMethod = "Unknown";
        double discountAmount = 0.0;
        String discountReason = "";
        double finalTotal = 0.0;
        double tenderedAmount = 0.0; // New variable

        try (Connection conn = DatabaseConnection.getConnection()) {
            // UPGRADED: Added amountTendered to the SELECT query
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

        // --- RECONSTRUCT THERMAL RECEIPT ---
        VBox receiptBox = new VBox(8);
        receiptBox.setAlignment(Pos.TOP_CENTER);
        receiptBox.setPrefWidth(250);
        receiptBox.setMaxWidth(250);
        receiptBox.setPadding(new Insets(10));
        receiptBox.setStyle("-fx-background-color: white; -fx-font-family: 'Monospaced'; -fx-font-size: 11px; -fx-text-fill: black;");

        Label title = new Label("KaMotoMo\nMotor Parts");
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label details = new Label("TXN: " + txId + "\nDate: " + timestamp + "\nCashier: " + cashier + "\nPay: " + paymentMethod);
        details.setAlignment(Pos.CENTER_LEFT);
        details.setMaxWidth(Double.MAX_VALUE);

        Label divider1 = new Label("------------------------");
        Label divider2 = new Label("------------------------");

        VBox itemsBox = new VBox(2);
        double rawSubtotal = 0.0;

        // Fetch Line Items
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
                double unitPrice = subTotal / qty; // Reverse-engineer unit price for display

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

        // Reconstruct Discounts (if applicable)
        if (discountAmount > 0) {
            receiptBox.getChildren().add(new Label(String.format("SUBTOTAL:   ₱%.2f", rawSubtotal)));
            receiptBox.getChildren().add(new Label(String.format("DISCOUNT:  -₱%.2f", discountAmount)));
            receiptBox.getChildren().add(new Label(String.format("REASON:     %s", discountReason != null ? discountReason : "Manual")));
            receiptBox.getChildren().add(new Label("------------------------"));
        }

        Label totalLbl = new Label(String.format("TOTAL:      ₱%.2f", finalTotal));
        totalLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        // Safety check: If it's an old transaction from before we added the database column,
        // assume they paid the exact amount to prevent negative change.
        if (tenderedAmount < finalTotal) {
            tenderedAmount = finalTotal;
        }

        double changeAmount = tenderedAmount - finalTotal;

        Label tenderLbl = new Label(String.format("Tendered:   ₱%.2f", tenderedAmount));
        Label changeLbl = new Label(String.format("Change:     ₱%.2f", changeAmount));

        // UPGRADED: Now identical to the POS receipt
        receiptBox.getChildren().addAll(totalLbl, tenderLbl, changeLbl, new Label("\nThank you!\nIngat sa kalsada! 🏍"));

        // Wrap in a container for the popup UI
        HBox containerBox = new HBox(receiptBox);
        containerBox.setAlignment(Pos.CENTER);
        containerBox.setPadding(new Insets(20));
        dialogPane.setContent(containerBox);

        ButtonType printType = new ButtonType("🖨 Print Reprint", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(printType, closeType);

        // Add Print Functionality
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == printType) {
                javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(dialog.getOwner())) {
                    boolean success = job.printPage(receiptBox);
                    if (success) job.endJob();

                    // Log that a reprint was issued
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
    }

    @FXML
    protected void onExportLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export System Logs");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("KaMotoMo_AuditLogs.csv");
        File file = fileChooser.showSaveDialog(logsTable.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // CSV structural headers
                writer.println("Timestamp,User,Action,Details");
                for (LogEntry log : filteredData) {
                    // Escape details containing quotes or internal commas
                    String cleanDetails = log.getDetails().replace("\"", "\"\"");
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            log.getTimestamp(), log.getUsername(), log.getAction(), cleanDetails);
                }
                SystemLogger.logAction("Export", "Exported system logs to CSV.");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    protected void onClearLogs() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning: Clear All Logs");

        // CLEAN HEADER FIX: Delete the white box and move text to the body
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to permanently delete all logs?");

        // USE LOCAL THEME INJECTOR
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

    // --- THE TARGETED THEME HUNTER ---
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
}