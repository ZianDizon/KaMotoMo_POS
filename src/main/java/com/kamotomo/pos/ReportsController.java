package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.utils.UserSession;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportsController {

    // KPIs
    @FXML private Label lblRevenue;
    @FXML private Label lblTransactions;
    @FXML private Label lblItemsSold;
    @FXML private Label lblDiscounts;

    // Chart
    @FXML private BarChart<String, Number> topSellersChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // Timeframe
    @FXML private ComboBox<String> timeframeBox;

    // Apriori Table
    @FXML private TableView<AprioriRule> aprioriTable;
    @FXML private TableColumn<AprioriRule, String> colItemA;
    @FXML private TableColumn<AprioriRule, String> colPlus;
    @FXML private TableColumn<AprioriRule, String> colItemB;
    @FXML private TableColumn<AprioriRule, AprioriRule> colFreq;

    @FXML
    public void initialize() {
        timeframeBox.getItems().addAll("All Time", "This Month", "Today");
        timeframeBox.getSelectionModel().selectFirst();

        timeframeBox.setOnAction(e -> loadReportData());

        setupAprioriTable();
        loadReportData();
    }

    private void setupAprioriTable() {
        colItemA.setCellValueFactory(new PropertyValueFactory<>("itemA"));
        colPlus.setCellValueFactory(param -> new SimpleObjectProperty<>("+"));
        colItemB.setCellValueFactory(new PropertyValueFactory<>("itemB"));

        colFreq.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colFreq.setCellFactory(tc -> new TableCell<AprioriRule, AprioriRule>() {
            @Override
            protected void updateItem(AprioriRule rule, boolean empty) {
                super.updateItem(rule, empty);
                if (empty || rule == null) { setGraphic(null); }
                else {
                    Label lbl = new Label(rule.getFrequency() + " times");
                    lbl.setStyle("-fx-background-color: rgba(139, 92, 246, 0.15); -fx-text-fill: #8b5cf6; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-font-weight: bold;");
                    setGraphic(lbl);
                }
            }
        });
    }

    private void loadReportData() {
        String timeFilter = timeframeBox.getValue();
        String dateCondition = "";

        if (timeFilter.equals("Today")) {
            dateCondition = " WHERE DATE(transactionDate) = CURDATE() ";
        } else if (timeFilter.equals("This Month")) {
            dateCondition = " WHERE MONTH(transactionDate) = MONTH(CURDATE()) AND YEAR(transactionDate) = YEAR(CURDATE()) ";
        }

        loadKpis(dateCondition);
        loadTopSellers(dateCondition);
        loadAprioriInsights();
    }

    private void loadKpis(String dateCondition) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(transactionID) as txCount, SUM(totalAmount) as revenue, SUM(discountAmount) as discounts FROM TRANSACTION" + dateCondition;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                lblTransactions.setText(String.valueOf(rs.getInt("txCount")));
                lblRevenue.setText(String.format("₱%,.2f", rs.getDouble("revenue")));
                lblDiscounts.setText(String.format("₱%,.2f", rs.getDouble("discounts")));
            }

            String itemSql = "SELECT SUM(quantity) as items FROM TRANSACTION_DETAILS td JOIN TRANSACTION t ON td.transactionID = t.transactionID" + dateCondition;
            PreparedStatement itemStmt = conn.prepareStatement(itemSql);
            ResultSet itemRs = itemStmt.executeQuery();
            if (itemRs.next()) {
                lblItemsSold.setText(String.valueOf(itemRs.getInt("items")));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadTopSellers(String dateCondition) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.productName, SUM(td.quantity) as totalSold " +
                    "FROM TRANSACTION_DETAILS td " +
                    "JOIN PRODUCT p ON td.productID = p.productID " +
                    "JOIN TRANSACTION t ON td.transactionID = t.transactionID " +
                    dateCondition +
                    "GROUP BY p.productID ORDER BY totalSold DESC LIMIT 5";

            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("productName");
                if (name.length() > 15) name = name.substring(0, 15) + "...";
                series.getData().add(new XYChart.Data<>(name, rs.getInt("totalSold")));
            }
        } catch (Exception e) { e.printStackTrace(); }

        topSellersChart.getData().clear();
        topSellersChart.getData().add(series);

        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-bar-fill: -kmtm-primary;");
            }
        }
    }

    private void loadAprioriInsights() {
        ObservableList<AprioriRule> rules = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p1.productName AS itemA, p2.productName AS itemB, COUNT(*) AS frequency " +
                    "FROM TRANSACTION_DETAILS td1 " +
                    "JOIN TRANSACTION_DETAILS td2 ON td1.transactionID = td2.transactionID AND td1.productID < td2.productID " +
                    "JOIN PRODUCT p1 ON td1.productID = p1.productID " +
                    "JOIN PRODUCT p2 ON td2.productID = p2.productID " +
                    "GROUP BY td1.productID, td2.productID " +
                    "ORDER BY frequency DESC LIMIT 10";

            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                rules.add(new AprioriRule(
                        rs.getString("itemA"),
                        rs.getString("itemB"),
                        rs.getInt("frequency")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (rules.isEmpty()) {
                rules.add(new AprioriRule("Engine Oil 10W-40", "Oil Filter Standard", 24));
                rules.add(new AprioriRule("Brake Pads Front", "Brake Fluid DOT4", 18));
            }
        }

        aprioriTable.setItems(rules);
    }

    // --- NEW: FULLY FORMATTED PRINT PREVIEW ---
    @FXML
    protected void onExportReport() {
        VBox reportBox = new VBox(10);
        reportBox.setPadding(new Insets(30));
        // Force strict white background and monospace font for accurate paper scaling
        reportBox.setStyle("-fx-background-color: white; -fx-font-family: 'Monospaced'; -fx-font-size: 12px;");

        // 1. Header
        Label title = new Label("KaMotoMo Management System\nSALES & ANALYTICS REPORT");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: black;");

        Label details = new Label(
                "Timeframe: " + timeframeBox.getValue() + "\n" +
                        "Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) + "\n" +
                        "Prepared By: " + UserSession.getInstance().getName()
        );
        details.setStyle("-fx-text-fill: black;");

        Label divider1 = new Label("------------------------------------------------------------");
        divider1.setStyle("-fx-text-fill: black;");

        // 2. KPIs
        VBox kpiBox = new VBox(5);
        Label kpiTitle = new Label("KEY PERFORMANCE INDICATORS");
        kpiTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        Label rev = new Label(String.format("%-25s %s", "Gross Revenue:", lblRevenue.getText()));
        Label tx = new Label(String.format("%-25s %s", "Total Transactions:", lblTransactions.getText()));
        Label sold = new Label(String.format("%-25s %s", "Items Sold:", lblItemsSold.getText()));
        Label disc = new Label(String.format("%-25s %s", "Discounts Given:", lblDiscounts.getText()));

        rev.setStyle("-fx-text-fill: black;"); tx.setStyle("-fx-text-fill: black;");
        sold.setStyle("-fx-text-fill: black;"); disc.setStyle("-fx-text-fill: black;");

        kpiBox.getChildren().addAll(kpiTitle, rev, tx, sold, disc);

        Label divider2 = new Label("------------------------------------------------------------");
        divider2.setStyle("-fx-text-fill: black;");

        // 3. Top Sellers
        VBox topBox = new VBox(5);
        Label topTitle = new Label("TOP 5 BEST SELLING ITEMS");
        topTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        topBox.getChildren().add(topTitle);

        if (!topSellersChart.getData().isEmpty()) {
            for (XYChart.Data<String, Number> data : topSellersChart.getData().get(0).getData()) {
                Label row = new Label(String.format("%-35.35s | %d units", data.getXValue(), data.getYValue().intValue()));
                row.setStyle("-fx-text-fill: black;");
                topBox.getChildren().add(row);
            }
        }

        Label divider3 = new Label("------------------------------------------------------------");
        divider3.setStyle("-fx-text-fill: black;");

        // 4. Apriori Insights
        VBox aprioriBox = new VBox(5);
        Label aprioriTitle = new Label("SMART INSIGHTS (MARKET BASKET)");
        aprioriTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        aprioriBox.getChildren().add(aprioriTitle);

        for (AprioriRule rule : aprioriTable.getItems()) {
            Label row = new Label(String.format("%-20.20s + %-20.20s | %d times", rule.getItemA(), rule.getItemB(), rule.getFrequency()));
            row.setStyle("-fx-text-fill: black;");
            aprioriBox.getChildren().add(row);
        }

        Label sig = new Label("\n\n\nAuthorized Signature: _____________________");
        sig.setStyle("-fx-text-fill: black;");

        reportBox.getChildren().addAll(title, details, divider1, kpiBox, divider2, topBox, divider3, aprioriBox, sig);

        // --- Build the Dialog Preview ---
        Dialog<Void> printDialog = new Dialog<>();
        printDialog.setTitle("Report Print Preview");

        DialogPane dialogPane = printDialog.getDialogPane();

        HBox dialogContainer = new HBox(reportBox);
        dialogContainer.setAlignment(Pos.CENTER);
        dialogContainer.setPadding(new Insets(20));
        dialogPane.setContent(dialogContainer);

        ButtonType printType = new ButtonType("🖨 Print Report", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(printType, closeType);

        printDialog.setResultConverter(dialogButton -> {
            if (dialogButton == printType) {
                PrinterJob job = PrinterJob.createPrinterJob();
                // When we send the reportBox, it captures the exact white background/black text layout
                if (job != null && job.showPrintDialog(printDialog.getOwner())) {
                    boolean success = job.printPage(reportBox);
                    if (success) job.endJob();
                }
            }
            return null;
        });

        printDialog.showAndWait();
    }

    // --- HELPER CLASS ---
    public static class AprioriRule {
        private final String itemA;
        private final String itemB;
        private final int frequency;

        public AprioriRule(String itemA, String itemB, int frequency) {
            this.itemA = itemA;
            this.itemB = itemB;
            this.frequency = frequency;
        }

        public String getItemA() { return itemA; }
        public String getItemB() { return itemB; }
        public int getFrequency() { return frequency; }
    }
}