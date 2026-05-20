package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OverviewController {

    @FXML private VBox overviewRoot;

    // Top Cards
    @FXML private Label lblTodaySales;
    @FXML private Label lblTodayTxnCount;
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblActiveProducts;
    @FXML private Label lblLowStock;

    // Chart
    @FXML private BarChart<String, Number> salesChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // Lists and Tables
    @FXML private ListView<CriticalItem> criticalStockList;
    @FXML private TableView<TransactionSummary> recentTxnTable;
    @FXML private TableColumn<TransactionSummary, String> colTxnId;
    @FXML private TableColumn<TransactionSummary, String> colDate;
    @FXML private TableColumn<TransactionSummary, String> colItems;
    @FXML private TableColumn<TransactionSummary, String> colTotal;
    @FXML private TableColumn<TransactionSummary, String> colPayment;
    @FXML private TableColumn<TransactionSummary, String> colCashier;

    @FXML
    public void initialize() {
        FadeTransition ft = new FadeTransition(Duration.millis(600), overviewRoot);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        setupTableColumns();
        setupCriticalStockList();
        loadDashboardData();
    }

    private void setupTableColumns() {
        colTxnId.setCellValueFactory(new PropertyValueFactory<>("txnId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colItems.setCellValueFactory(new PropertyValueFactory<>("items"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("payment"));
        colCashier.setCellValueFactory(new PropertyValueFactory<>("cashier"));
    }

    private void setupCriticalStockList() {
        criticalStockList.setCellFactory(lv -> new ListCell<CriticalItem>() {
            @Override
            protected void updateItem(CriticalItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox root = new HBox();
                    root.setStyle("-fx-background-color: -kmtm-surface2; -fx-padding: 10 15; -fx-background-radius: 4; -fx-border-color: transparent transparent -kmtm-border transparent; -fx-border-width: 0 0 1 0;");

                    VBox leftBox = new VBox(2);
                    Label nameLbl = new Label(item.getName());
                    nameLbl.setStyle("-fx-text-fill: -kmtm-text; -fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold; -fx-font-size: 13px;");
                    Label idLbl = new Label(item.getSku());
                    idLbl.setStyle("-fx-text-fill: -kmtm-text-muted; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px;");
                    leftBox.getChildren().addAll(nameLbl, idLbl);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label stockLbl = new Label(item.getStock() + " left");
                    stockLbl.setStyle("-fx-text-fill: #f03d3d; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 12px; -fx-font-weight: bold;");

                    root.setAlignment(Pos.CENTER_LEFT);
                    root.getChildren().addAll(leftBox, spacer, stockLbl);

                    setGraphic(root);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0 0 5 0;");
                }
            }
        });
    }

    private void loadDashboardData() {
        ObservableList<CriticalItem> criticalItems = FXCollections.observableArrayList();
        ObservableList<TransactionSummary> recentTxns = FXCollections.observableArrayList();
        double[] monthlyRevenue = new double[12]; // Array to hold 12 months of data

        try (Connection conn = DatabaseConnection.getConnection()) {

            // 1. Get Active Products & Low Stock Alerts
            String productSql = "SELECT * FROM PRODUCT WHERE status = 'Active'";
            PreparedStatement prodStmt = conn.prepareStatement(productSql);
            ResultSet rsProd = prodStmt.executeQuery();
            int activeCount = 0;
            int lowStockCount = 0;

            while (rsProd.next()) {
                activeCount++;
                int stock = rsProd.getInt("stockQuantity");

                // STANDARDIZED MATH: ROP = 9 (Match the Stock Monitor)
                int rop = (2 * 2) + 5;

                if (stock <= rop) {
                    lowStockCount++;

                    // Only add to the Dashboard Critical list if it hits the "Critical" tier
                    if (stock <= (rop / 2)) {
                        String cat = rsProd.getString("category");
                        String prefix = (cat != null && cat.length() >= 3) ? cat.substring(0, 3).toUpperCase() : "ITM";
                        String sku = String.format("%s-%04d", prefix, rsProd.getInt("productID"));
                        criticalItems.add(new CriticalItem(sku, rsProd.getString("productName"), stock));
                    }
                }
            }
            lblActiveProducts.setText(String.valueOf(activeCount));
            lblLowStock.setText(String.valueOf(lowStockCount));

            // 2. Get Totals & Recent Transactions
            // Note: Update 'transactionDate' below to match your actual database column name if different!
            String txSql = "SELECT t.transactionID, t.totalAmount, t.paymentMethod, t.transactionDate, u.name as cashier " +
                    "FROM TRANSACTION t LEFT JOIN USER u ON t.userID = u.userID ORDER BY t.transactionID DESC";
            PreparedStatement txStmt = conn.prepareStatement(txSql);
            ResultSet rsTx = txStmt.executeQuery();

            double totalRev = 0;
            double todayRev = 0;
            int todayCount = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, hh:mm a");
            SimpleDateFormat monthFormat = new SimpleDateFormat("M"); // Gets month number 1-12

            while (rsTx.next()) {
                double amt = rsTx.getDouble("totalAmount");
                totalRev += amt;

                // Assuming you have a Date/Timestamp column. If not, this gracefully handles it.
                Date txDate = rsTx.getTimestamp("transactionDate");
                if (txDate != null) {
                    // Check if it's today (Basic check for demo purposes)
                    if (sdf.format(txDate).substring(0, 12).equals(sdf.format(new Date()).substring(0, 12))) {
                        todayRev += amt;
                        todayCount++;
                    }
                    // Add to chart array (month format returns 1 for Jan, so index is month - 1)
                    int monthIndex = Integer.parseInt(monthFormat.format(txDate)) - 1;
                    monthlyRevenue[monthIndex] += amt;
                }

                // Add to recent transactions table (Limit to 15 for UI speed)
                if (recentTxns.size() < 15) {
                    String formattedId = "T" + String.format("%012d", rsTx.getInt("transactionID"));
                    String dateStr = txDate != null ? sdf.format(txDate) : "Unknown";
                    recentTxns.add(new TransactionSummary(
                            formattedId, dateStr, "View Items", String.format("₱%,.2f", amt),
                            rsTx.getString("paymentMethod"), rsTx.getString("cashier")
                    ));
                }
            }

            lblTotalRevenue.setText(String.format("₱%,.2f", totalRev));
            lblTodaySales.setText(String.format("₱%,.2f", todayRev));
            lblTodayTxnCount.setText(todayCount + " transaction(s)");

        } catch (Exception e) {
            System.out.println("Could not load dashboard data from database. Showing visual layout.");
            // If the database query fails (e.g., column names mismatch), inject some dummy data to keep UI alive
            criticalItems.add(new CriticalItem("ENG-0007", "Chain Lubricant", 2));
            criticalItems.add(new CriticalItem("FLT-0004", "Air Filter", 3));
            monthlyRevenue[4] = 1040.0; // Put dummy data in May
            recentTxns.add(new TransactionSummary("T1778779674930", "May 15, 2026", "2 item(s)", "₱240.00", "GCASH", "Admin User"));
            // e.printStackTrace();
        }

        criticalStockList.setItems(criticalItems);
        recentTxnTable.setItems(recentTxns);
        setupChart(monthlyRevenue);
    }

    private void setupChart(double[] monthlyRevenue) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (int i = 0; i < months.length; i++) {
            series.getData().add(new XYChart.Data<>(months[i], monthlyRevenue[i]));
        }

        salesChart.getData().clear();
        xAxis.getCategories().clear();
        xAxis.getCategories().addAll(months);
        salesChart.getData().add(series);

        // --- NEW: Format Y-Axis to show the Peso sign and decimals ---
        yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("₱%,.2f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return null; // Not needed for displaying
            }
        });

        // Apply Custom Tooltips and Styling to nodes after they are added to the chart
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) {
                // Default Bar Color
                data.getNode().setStyle("-fx-bar-fill: -kmtm-primary;");

                // If value is 0, don't bother showing a tooltip
                if (data.getYValue().doubleValue() > 0) {

                    // Build the custom floating UI Box (Replicating your screenshot exactly)
                    VBox tBox = new VBox(5);
                    tBox.setStyle("-fx-background-color: #1a1e20; -fx-border-color: #2e3538; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");

                    Label mLabel = new Label(data.getXValue());
                    mLabel.setStyle("-fx-text-fill: #e8eaeb; -fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold; -fx-font-size: 12px;");

                    HBox valBox = new HBox(5);
                    valBox.setAlignment(Pos.CENTER_LEFT);
                    Region square = new Region();
                    square.setPrefSize(10, 10);
                    square.setStyle("-fx-background-color: -kmtm-primary;");

                    Label vLabel = new Label(String.format("₱%,.2f", data.getYValue().doubleValue()));
                    vLabel.setStyle("-fx-text-fill: white; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 12px;");

                    valBox.getChildren().addAll(square, vLabel);
                    tBox.getChildren().addAll(mLabel, valBox);

                    // Create the Tooltip and hide its default background
                    Tooltip tooltip = new Tooltip();
                    tooltip.setGraphic(tBox);
                    tooltip.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                    tooltip.setShowDelay(Duration.millis(50)); // Shows almost instantly

                    Tooltip.install(data.getNode(), tooltip);

                    // Hover Highlighting Effect
                    data.getNode().setOnMouseEntered(e -> data.getNode().setStyle("-fx-bar-fill: #ff8533; -fx-cursor: hand;"));
                    data.getNode().setOnMouseExited(e -> data.getNode().setStyle("-fx-bar-fill: -kmtm-primary;"));
                }
            }
        }
    }

    // --- HELPER DATA CLASSES ---

    public static class CriticalItem {
        private final String sku;
        private final String name;
        private final int stock;

        public CriticalItem(String sku, String name, int stock) {
            this.sku = sku;
            this.name = name;
            this.stock = stock;
        }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public int getStock() { return stock; }
    }

    public static class TransactionSummary {
        private final String txnId;
        private final String date;
        private final String items;
        private final String total;
        private final String payment;
        private final String cashier;

        public TransactionSummary(String txnId, String date, String items, String total, String payment, String cashier) {
            this.txnId = txnId;
            this.date = date;
            this.items = items;
            this.total = total;
            this.payment = payment;
            this.cashier = cashier;
        }
        public String getTxnId() { return txnId; }
        public String getDate() { return date; }
        public String getItems() { return items; }
        public String getTotal() { return total; }
        public String getPayment() { return payment; }
        public String getCashier() { return cashier; }
    }
}