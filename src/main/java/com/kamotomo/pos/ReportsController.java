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
    @FXML private Label lblTopSellersTitle;

    // Chart
    @FXML private BarChart<String, Number> topSellersChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // Timeframe
    @FXML private ComboBox<String> timeframeBox;

    @FXML private ComboBox<String> topCountBox;

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

        if (topCountBox != null) {
            topCountBox.getItems().addAll("Top 5", "Top 10", "Top 20");
            topCountBox.getSelectionModel().selectFirst();
            topCountBox.setOnAction(e -> loadReportData());
        }

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
        // FIX: Strictly enforce that we only analyze Completed sales, ignoring Voids.
        // We use t.status so it correctly aliases when joined with TRANSACTION_DETAILS
        String dateCondition = " WHERE (t.status = 'Completed' OR t.status IS NULL) ";

        if (timeFilter.equals("Today")) {
            dateCondition += " AND DATE(t.transactionDate) = CURDATE() ";
        } else if (timeFilter.equals("This Month")) {
            dateCondition += " AND MONTH(t.transactionDate) = MONTH(CURDATE()) AND YEAR(t.transactionDate) = YEAR(CURDATE()) ";
        }

        loadKpis(dateCondition);
        loadTopSellers(dateCondition);

        // --- THIS LINE IS THE FIX ---
        // Now it passes the date filter to the Apriori query!
        loadAprioriInsights(dateCondition);
    }

    private void loadKpis(String dateCondition) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(t.transactionID) as txCount, SUM(t.totalAmount) as revenue, SUM(t.discountAmount) as discounts FROM TRANSACTION t" + dateCondition;            PreparedStatement stmt = conn.prepareStatement(sql);
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

    // --- HELPER: Draws the visual KPI boxes for the printer ---
    private VBox createPrintKpiCard(String title, String value) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(170);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-weight: bold;");
        Label vLbl = new Label(value);
        vLbl.setStyle("-fx-font-size: 18px; -fx-text-fill: #111827; -fx-font-weight: bold;");

        card.getChildren().addAll(tLbl, vLbl);
        return card;
    }

    private void loadTopSellers(String dateCondition) {
        int limit = 5;
        String titleText = "TOP 5";

        if (topCountBox != null && topCountBox.getValue() != null) {
            titleText = topCountBox.getValue().toUpperCase();
            limit = Integer.parseInt(topCountBox.getValue().replace("Top ", ""));
        }

        // UPDATE THE DASHBOARD LABEL DYNAMICALLY
        if (lblTopSellersTitle != null) {
            lblTopSellersTitle.setText(titleText + " BEST SELLING ITEMS");
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.productName, SUM(td.quantity) as totalSold " +
                    "FROM TRANSACTION_DETAILS td " +
                    "JOIN PRODUCT p ON td.productID = p.productID " +
                    "JOIN TRANSACTION t ON td.transactionID = t.transactionID " +
                    dateCondition +
                    "GROUP BY p.productID ORDER BY totalSold DESC LIMIT ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit); // Inject the dynamic limit
            ResultSet rs = stmt.executeQuery();
            // ... rest of the method remains exactly the same

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

    private void loadAprioriInsights(String dateCondition) {
        ObservableList<AprioriRule> rules = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Added explicit spacing around dateCondition to ensure SQL syntax is perfect
            String sql = "SELECT p1.productName AS itemA, p2.productName AS itemB, COUNT(*) AS frequency " +
                    "FROM TRANSACTION_DETAILS td1 " +
                    "JOIN TRANSACTION_DETAILS td2 ON td1.transactionID = td2.transactionID AND td1.productID < td2.productID " +
                    "JOIN PRODUCT p1 ON td1.productID = p1.productID " +
                    "JOIN PRODUCT p2 ON td2.productID = p2.productID " +
                    "JOIN TRANSACTION t ON td1.transactionID = t.transactionID \n" +
                    dateCondition + "\n" +
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
        }

        aprioriTable.setItems(rules); // If there are no group sales today, the table will correctly be empty!
    }

    @FXML
    private void printDetailedReport() {
        // Create the main page layout (Width 800px simulates A4/Letter paper proportions)
        VBox reportPage = new VBox(20);
        reportPage.setPadding(new Insets(40));
        reportPage.setStyle("-fx-background-color: white; -fx-font-family: 'IBM Plex Sans', sans-serif;");
        reportPage.setPrefWidth(800);

        // 1. Header Section
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);

        VBox titleBox = new VBox(5);
        Label companyName = new Label("KaMotoMo Motor Parts");
        companyName.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label reportName = new Label("SALES & ANALYTICS REPORT");
        reportName.setStyle("-fx-font-size: 16px; -fx-text-fill: #4b5563; -fx-font-weight: bold;");
        titleBox.getChildren().addAll(companyName, reportName);

        VBox metaBox = new VBox(2);
        metaBox.setAlignment(Pos.CENTER_RIGHT);
        Label dateLbl = new Label("Generated: " + new SimpleDateFormat("MMM dd, yyyy - HH:mm").format(new Date()));
        Label timeLbl = new Label("Timeframe: " + timeframeBox.getValue());
        Label prepLbl = new Label("Prepared By: " + UserSession.getInstance().getName());
        dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        timeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        prepLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        metaBox.getChildren().addAll(dateLbl, timeLbl, prepLbl);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(titleBox, metaBox);

        // 2. KPIs Section (4 visual boxes side-by-side)
        HBox kpiContainer = new HBox(15);
        kpiContainer.setAlignment(Pos.CENTER);
        kpiContainer.getChildren().addAll(
                createPrintKpiCard("Gross Revenue", lblRevenue.getText()),
                createPrintKpiCard("Total Transactions", lblTransactions.getText()),
                createPrintKpiCard("Items Sold", lblItemsSold.getText()),
                createPrintKpiCard("Discounts Given", lblDiscounts.getText())
        );

        // 3. Visuals & Tables (Split Left & Right)
        HBox contentBox = new HBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);

        // Left Side: The Bar Chart
        VBox chartBox = new VBox(10);
        chartBox.setPrefWidth(380);
        String dynamicTitle = topCountBox.getValue() != null ? topCountBox.getValue() : "Top 5";
        Label chartTitle = new Label(dynamicTitle + " Best Selling Items");
        chartTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        // We clone the chart so animations don't break the printer
        BarChart<String, Number> printChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        printChart.setAnimated(false);
        printChart.setLegendVisible(false);
        printChart.setStyle("-fx-background-color: transparent;");

        if (!topSellersChart.getData().isEmpty()) {
            XYChart.Series<String, Number> printSeries = new XYChart.Series<>();
            for (XYChart.Data<String, Number> d : topSellersChart.getData().get(0).getData()) {
                printSeries.getData().add(new XYChart.Data<>(d.getXValue(), d.getYValue()));
            }
            printChart.getData().add(printSeries);

            // Color the bars to match your theme
            for (XYChart.Data<String, Number> d : printSeries.getData()) {
                if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: #8b5cf6;");
            }
        }
        chartBox.getChildren().addAll(chartTitle, printChart);

        // Right Side: The Apriori Table Grid
        VBox tableBox = new VBox(10);
        tableBox.setPrefWidth(350);
        Label tableTitle = new Label("Market Basket Insights");
        tableTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        // Use a GridPane to create a perfectly aligned visual table
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 1px; -fx-background-color: white; -fx-border-radius: 4;");
        grid.setVgap(10);
        grid.setHgap(15);
        grid.setPadding(new Insets(15));

        Label h1 = new Label("Primary Item"); h1.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111827;");
        Label h2 = new Label("Bought With"); h2.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111827;");
        Label h3 = new Label("Freq."); h3.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111827;");
        grid.addRow(0, h1, h2, h3);

        int row = 1;
        for (AprioriRule rule : aprioriTable.getItems()) {
            Label iA = new Label(rule.getItemA()); iA.setStyle("-fx-font-size: 11px;"); iA.setWrapText(true); iA.setMaxWidth(120);
            Label iB = new Label(rule.getItemB()); iB.setStyle("-fx-font-size: 11px;"); iB.setWrapText(true); iB.setMaxWidth(120);
            Label f = new Label(rule.getFrequency() + "x"); f.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b5cf6; -fx-font-weight: bold;");
            grid.addRow(row++, iA, iB, f);
        }
        tableBox.getChildren().addAll(tableTitle, grid);

        contentBox.getChildren().addAll(chartBox, tableBox);

        // Footer Section
        VBox footer = new VBox(5);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(30, 0, 0, 0));
        Label sigLine = new Label("_____________________________");
        Label sigText = new Label("Authorized Signature");
        sigText.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        footer.getChildren().addAll(sigLine, sigText);

        // Assemble the page
        reportPage.getChildren().addAll(header, new Separator(), kpiContainer, new Separator(), contentBox, footer);

        // --- Display Print Preview Dialog ---
        Dialog<Void> printDialog = new Dialog<>();
        printDialog.setTitle("Report Print Preview");
        DialogPane dialogPane = printDialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        HBox dialogContainer = new HBox(reportPage);
        dialogContainer.setAlignment(Pos.CENTER);
        dialogContainer.setStyle("-fx-background-color: #f3f4f6;"); // Grey background for the preview window so the white paper pops out
        dialogContainer.setPadding(new Insets(20));
        dialogPane.setContent(dialogContainer);

        ButtonType printType = new ButtonType("🖨 Print Report", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(printType, ButtonType.CANCEL);

        printDialog.setResultConverter(dialogButton -> {
            if (dialogButton == printType) {
                PrinterJob job = PrinterJob.createPrinterJob();
                // We print the reportPage node, which JavaFX will automatically convert into a high-quality vector printout!
                if (job != null && job.showPrintDialog(printDialog.getOwner())) {
                    boolean success = job.printPage(reportPage);
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

    @FXML
    protected void onExportReport() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Print Format Selection");
        alert.setHeaderText("Select Report Format");
        alert.setContentText("How would you like to print this report?\n\n" +
                "• Simple: Thermal receipt style (Saves ink/paper)\n" +
                "• Detailed: Full A4 Dashboard style (For presentations)");

        applyThemeToDialog(alert.getDialogPane());

        ButtonType btnSimple = new ButtonType("Simple (Receipt)");
        ButtonType btnDetailed = new ButtonType("Detailed (A4)");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnSimple, btnDetailed, btnCancel);

        alert.showAndWait().ifPresent(type -> {
            if (type == btnSimple) {
                printSimpleReport();
            } else if (type == btnDetailed) {
                printDetailedReport();
            }
        });
    }

    private void printSimpleReport() {
        VBox receiptBox = new VBox(5);
        receiptBox.setPrefWidth(250);
        receiptBox.setMaxWidth(250);
        receiptBox.setPadding(new Insets(10));
        receiptBox.setStyle("-fx-background-color: white; -fx-font-family: 'Monospaced'; -fx-font-size: 11px; -fx-text-fill: black;");

        receiptBox.getChildren().addAll(
                new Label("KaMotoMo Motor Parts"),
                new Label("SALES REPORT (" + timeframeBox.getValue() + ")"),
                new Label("------------------------"),
                new Label(String.format("%-10s %s", "Rev:", lblRevenue.getText())),
                new Label(String.format("%-10s %s", "Txns:", lblTransactions.getText())),
                new Label(String.format("%-10s %s", "Items:", lblItemsSold.getText())),
                new Label("------------------------"),
                new Label("Printed: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())),
                new Label("By: " + UserSession.getInstance().getName())
        );

        // --- NEW: WRAP IN A PREVIEW DIALOG ---
        Dialog<Void> printDialog = new Dialog<>();
        printDialog.setTitle("Receipt Print Preview");

        DialogPane dialogPane = printDialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        HBox dialogContainer = new HBox(receiptBox);
        dialogContainer.setAlignment(Pos.CENTER);
        dialogContainer.setStyle("-fx-background-color: #f3f4f6;"); // Grey backdrop so the white receipt pops
        dialogContainer.setPadding(new Insets(20));
        dialogPane.setContent(dialogContainer);

        ButtonType printType = new ButtonType("🖨 Print Receipt", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(printType, ButtonType.CANCEL);

        printDialog.setResultConverter(dialogButton -> {
            if (dialogButton == printType) {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(printDialog.getOwner())) {
                    boolean success = job.printPage(receiptBox);
                    if (success) job.endJob();
                }
            }
            return null;
        });

        printDialog.showAndWait();
    }

    // --- THE TARGETED THEME HUNTER ---
    private void applyThemeToDialog(DialogPane dialogPane) {
        if (lblRevenue == null || lblRevenue.getScene() == null) return;

        String activeThemeUrl = "";
        javafx.scene.Parent current = lblRevenue.getParent();

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

        if (activeThemeUrl.isEmpty() && lblRevenue.getScene() != null) {
            for (String stylesheet : lblRevenue.getScene().getStylesheets()) {
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