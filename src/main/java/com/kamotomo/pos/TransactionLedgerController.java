package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.models.TransactionRecord;
import com.kamotomo.pos.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TransactionLedgerController {

    @FXML private TableView<TransactionRecord> transactionTable;
    @FXML private TableColumn<TransactionRecord, Integer> colId;
    @FXML private TableColumn<TransactionRecord, String> colDate;
    @FXML private TableColumn<TransactionRecord, Double> colTotal;
    @FXML private TableColumn<TransactionRecord, String> colMethod;
    @FXML private TableColumn<TransactionRecord, String> colStatus;

    @FXML private TextField searchField;
    @FXML private Button btnVoid;

    private ObservableList<TransactionRecord> transactionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Link TableColumns to the TransactionRecord model
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadTransactions();

        // Search Bar Logic
        FilteredList<TransactionRecord> filteredData = new FilteredList<>(transactionList, b -> true);
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(txn -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    String searchKeyword = newValue.toLowerCase();
                    return String.valueOf(txn.getId()).contains(searchKeyword) ||
                            txn.getDate().toLowerCase().contains(searchKeyword);
                });
            });
        }
        transactionTable.setItems(filteredData);
    }

    private void loadTransactions() {
        transactionList.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Ensure you added the 'status' column to your database as discussed!
            String sql = "SELECT transactionID, transactionDate as date, totalAmount, paymentMethod, status FROM TRANSACTION ORDER BY transactionID DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                transactionList.add(new TransactionRecord(
                        rs.getInt("transactionID"),
                        rs.getString("date"),
                        rs.getDouble("totalAmount"),
                        rs.getString("paymentMethod"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onVoidTransactionClick() {
        TransactionRecord selectedTxn = transactionTable.getSelectionModel().getSelectedItem();

        if (selectedTxn == null) {
            showThemedAlert(Alert.AlertType.WARNING, "No Selection", "Please select a transaction from the table to void.");
            return;
        }

        if (selectedTxn.getStatus().equalsIgnoreCase("Voided")) {
            showThemedAlert(Alert.AlertType.WARNING, "Already Voided", "Transaction ID " + selectedTxn.getId() + " has already been voided.");
            return;
        }

        // Secure Authorization Prompt
        Dialog<String> passDialog = new Dialog<>();
        passDialog.setTitle("Void Authorization");
        passDialog.setHeaderText("Confirm Admin Password to Void TXN-" + selectedTxn.getId());
        applyThemeToDialog(passDialog.getDialogPane());

        PasswordField adminPassField = new PasswordField();
        adminPassField.setPromptText("Enter your Admin Password");
        adminPassField.setStyle("-fx-padding: 10; -fx-background-color: -kmtm-surface; -fx-border-color: -kmtm-border; -fx-border-radius: 4;");

        VBox content = new VBox(10, new Label("Secure Authorization required to reverse stock:"), adminPassField);
        content.setPadding(new Insets(20));
        passDialog.getDialogPane().setContent(content);

        ButtonType voidButtonType = new ButtonType("Authorize Void", ButtonBar.ButtonData.OK_DONE);
        passDialog.getDialogPane().getButtonTypes().addAll(voidButtonType, ButtonType.CANCEL);

        passDialog.setResultConverter(dialogButton -> {
            if (dialogButton == voidButtonType) {
                return adminPassField.getText().trim();
            }
            return null;
        });

        passDialog.showAndWait().ifPresent(password -> {
            executeBackendVoidProtocol(selectedTxn.getId(), password);
        });
    }

    private void executeBackendVoidProtocol(int transactionId, String passwordInput) {
        int currentUserId = UserSession.getInstance().getUserId();
        String adminName = UserSession.getInstance().getName();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start Strict ACID Transaction

            try {
                // 1. Verify Admin Password
                String authSql = "SELECT password FROM `user` WHERE userID = ?";
                PreparedStatement authStmt = conn.prepareStatement(authSql);
                authStmt.setInt(1, currentUserId);
                ResultSet authRs = authStmt.executeQuery();

                if (!authRs.next() || !authRs.getString("password").equals(com.kamotomo.pos.utils.SecurityUtil.hashPassword(passwordInput))) {
                    showThemedAlert(Alert.AlertType.ERROR, "Access Denied", "Incorrect Administrator password.");
                    com.kamotomo.pos.utils.SystemLogger.logAction("Security", "Failed void authorization attempt by Admin ID: " + currentUserId);
                    return;
                }

                // 2. Fetch items and return stock
                String getItemsSql = "SELECT productID, quantity FROM TRANSACTION_DETAILS WHERE transactionID = ?";
                PreparedStatement getItemsStmt = conn.prepareStatement(getItemsSql);
                getItemsStmt.setInt(1, transactionId);
                ResultSet itemsRs = getItemsStmt.executeQuery();

                String returnStockSql = "UPDATE PRODUCT SET stockQuantity = stockQuantity + ? WHERE productID = ?";
                PreparedStatement returnStockStmt = conn.prepareStatement(returnStockSql);

                while (itemsRs.next()) {
                    returnStockStmt.setInt(1, itemsRs.getInt("quantity"));
                    returnStockStmt.setInt(2, itemsRs.getInt("productID"));
                    returnStockStmt.addBatch();
                }

                // 3. Mark Transaction as Voided
                String voidSql = "UPDATE TRANSACTION SET status = 'Voided' WHERE transactionID = ?";
                PreparedStatement voidStmt = conn.prepareStatement(voidSql);
                voidStmt.setInt(1, transactionId);

                // Execute safely
                returnStockStmt.executeBatch();
                voidStmt.executeUpdate();
                conn.commit();

                com.kamotomo.pos.utils.SystemLogger.logAction("Transaction Void", "TXN-" + transactionId + " was securely voided and stock returned by Admin: " + adminName);
                showThemedAlert(Alert.AlertType.INFORMATION, "Void Successful", "Transaction " + transactionId + " has been officially voided. Inventory has been restored.");

                loadTransactions(); // Refresh the table to show the new 'Voided' status

            } catch (Exception ex) {
                conn.rollback();
                ex.printStackTrace();
                showThemedAlert(Alert.AlertType.ERROR, "System Error", "Database error occurred. No stock was moved.");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showThemedAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyThemeToDialog(DialogPane dialogPane) {
        dialogPane.getStylesheets().clear();

        // Strictly read the theme from the session to prevent light-mode defaults
        String activeTheme = UserSession.getInstance().getThemePreference();
        if (activeTheme == null || activeTheme.isEmpty()) activeTheme = "light";

        dialogPane.getStylesheets().add(getClass().getResource("/" + activeTheme + "-theme.css").toExternalForm());
        dialogPane.getStyleClass().addAll("custom-dialog", "root");
    }
}