package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.utils.SupplyChainEngine;
import com.kamotomo.pos.utils.UserSession;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StockMonitorController {

    @FXML private TableView<StockItem> stockTable;
    @FXML private TableColumn<StockItem, String> colId;
    @FXML private TableColumn<StockItem, StockItem> colName;
    @FXML private TableColumn<StockItem, String> colCategory;
    @FXML private TableColumn<StockItem, StockItem> colStockLevel;
    @FXML private TableColumn<StockItem, String> colStatus;
    @FXML private TableColumn<StockItem, StockItem> colAction;

    @FXML private ToggleButton btnFilterAll;
    @FXML private ToggleButton btnFilterCritical;
    @FXML private ToggleButton btnFilterLow;
    @FXML private ToggleButton btnFilterOk;
    @FXML private Button btnDraftOrder;
    @FXML private Button btnPendingOrders;

    private ObservableList<StockItem> masterData = FXCollections.observableArrayList();
    private FilteredList<StockItem> filteredData;

    @FXML
    public void initialize() {
        ToggleGroup filterGroup = new ToggleGroup();
        btnFilterAll.setToggleGroup(filterGroup);
        btnFilterCritical.setToggleGroup(filterGroup);
        btnFilterLow.setToggleGroup(filterGroup);
        btnFilterOk.setToggleGroup(filterGroup);

        filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) btnFilterAll.setSelected(true);
            applyFilter();
        });

        setupColorCodedToggles();
        setupTable();
        loadInventoryData();

        if ("Employee".equalsIgnoreCase(UserSession.getInstance().getRole())) {
            if (colAction != null) {
                colAction.setVisible(false);
            }
            if (btnDraftOrder != null) {
                btnDraftOrder.setVisible(false);
                btnDraftOrder.setManaged(false);
            }
            if (btnPendingOrders != null) {
                btnPendingOrders.setVisible(false);
                btnPendingOrders.setManaged(false);
            }
        }
    }

    private void applyThemeToDialog(DialogPane dialogPane) {
        if (stockTable == null || stockTable.getScene() == null) return;

        String activeThemeUrl = "";
        javafx.scene.Parent current = stockTable;

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
            for (String stylesheet : stockTable.getScene().getStylesheets()) {
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

    private void setupColorCodedToggles() {
        String baseBtn = "-fx-padding: 8 16; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-radius: 4; -fx-background-radius: 4; ";

        String critActive = baseBtn + "-fx-background-color: rgba(240,61,61,0.2); -fx-text-fill: #f03d3d; -fx-border-color: #f03d3d; -fx-border-width: 2;";
        String critInactive = baseBtn + "-fx-background-color: transparent; -fx-text-fill: #f03d3d; -fx-border-color: #f03d3d; -fx-border-width: 1;";
        btnFilterCritical.styleProperty().bind(Bindings.when(btnFilterCritical.selectedProperty()).then(critActive).otherwise(critInactive));

        String lowActive = baseBtn + "-fx-background-color: rgba(234,179,8,0.2); -fx-text-fill: #eab308; -fx-border-color: #eab308; -fx-border-width: 2;";
        String lowInactive = baseBtn + "-fx-background-color: transparent; -fx-text-fill: #eab308; -fx-border-color: #eab308; -fx-border-width: 1;";
        btnFilterLow.styleProperty().bind(Bindings.when(btnFilterLow.selectedProperty()).then(lowActive).otherwise(lowInactive));

        String okActive = baseBtn + "-fx-background-color: rgba(58,223,138,0.2); -fx-text-fill: #3adf8a; -fx-border-color: #3adf8a; -fx-border-width: 2;";
        String okInactive = baseBtn + "-fx-background-color: transparent; -fx-text-fill: #3adf8a; -fx-border-color: #3adf8a; -fx-border-width: 1;";
        btnFilterOk.styleProperty().bind(Bindings.when(btnFilterOk.selectedProperty()).then(okActive).otherwise(okInactive));

        String allActive = baseBtn + "-fx-background-color: rgba(245,98,15,0.2); -fx-text-fill: #f5620f; -fx-border-color: #f5620f; -fx-border-width: 2;";
        String allInactive = baseBtn + "-fx-background-color: transparent; -fx-text-fill: #f5620f; -fx-border-color: #f5620f; -fx-border-width: 1;";
        btnFilterAll.styleProperty().bind(Bindings.when(btnFilterAll.selectedProperty()).then(allActive).otherwise(allInactive));
    }

    private void showThemedAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        applyThemeToDialog(alert.getDialogPane());
        alert.getDialogPane().lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: -kmtm-text;"));

        alert.showAndWait();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("sku"));

        colName.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colName.setCellFactory(tc -> new TableCell<StockItem, StockItem>() {
            @Override
            protected void updateItem(StockItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    HBox box = new HBox(5);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(item.getName());
                    nameLbl.setStyle("-fx-text-fill: -kmtm-text;");
                    box.getChildren().add(nameLbl);

                    if (item.getPendingQty() > 0) {
                        Label pendingLbl = new Label("(⏳ " + item.getPendingQty() + " on order)");
                        pendingLbl.setStyle("-fx-text-fill: #eab308; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold;");
                        box.getChildren().add(pendingLbl);
                    }
                    setGraphic(box);
                }
            }
        });

        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCategory.setCellFactory(tc -> new TableCell<StockItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.setStyle("-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text-dim; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold;");
                    setGraphic(lbl);
                }
            }
        });

        colStockLevel.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colStockLevel.setCellFactory(tc -> new TableCell<StockItem, StockItem>() {
            @Override
            protected void updateItem(StockItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    VBox box = new VBox(4);
                    Label lbl = new Label(item.getStock() + " units");
                    lbl.setStyle("-fx-text-fill: -kmtm-text; -fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold; -fx-font-size: 13px;");

                    Rectangle track = new Rectangle(120, 3);
                    track.setStyle("-fx-fill: -kmtm-surface2;");
                    double fillRatio = Math.min(1.0, (double) item.getStock() / (item.getRop() * 2));
                    Rectangle fill = new Rectangle(120 * fillRatio, 3);

                    if (item.getStatus().equals("CRITICAL")) fill.setStyle("-fx-fill: #f03d3d;");
                    else if (item.getStatus().equals("LOW")) fill.setStyle("-fx-fill: #eab308;");
                    else fill.setStyle("-fx-fill: #3adf8a;");

                    StackPane barPane = new StackPane(track, fill);
                    barPane.setAlignment(Pos.CENTER_LEFT);
                    box.getChildren().addAll(lbl, barPane);
                    setGraphic(box);
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(tc -> new TableCell<StockItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add("badge");
                    if (item.equals("CRITICAL")) lbl.getStyleClass().add("badge-critical");
                    else if (item.equals("LOW")) lbl.getStyleClass().add("badge-low");
                    else lbl.getStyleClass().add("badge-ok");
                    setGraphic(lbl);
                }
            }
        });

        colAction.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colAction.setCellFactory(tc -> new TableCell<StockItem, StockItem>() {
            @Override
            protected void updateItem(StockItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    Button btn = new Button("+ Restock");
                    btn.getStyleClass().add("action-btn");
                    btn.setOnAction(e -> showRestockDialog(item));
                    setGraphic(btn);
                }
            }
        });
    }

    private void loadInventoryData() {
        masterData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            java.util.Map<Integer, Integer> pendingMap = new java.util.HashMap<>();
            String pendingSql = "SELECT pi.productID, SUM(pi.orderQty) as totalPending " +
                    "FROM PO_ITEM pi " +
                    "JOIN PURCHASE_ORDER po ON pi.poId = po.poId " +
                    "WHERE po.status = 'Pending' " +
                    "GROUP BY pi.productID";
            try (PreparedStatement pendStmt = conn.prepareStatement(pendingSql);
                 ResultSet rsPend = pendStmt.executeQuery()) {
                while (rsPend.next()) {
                    pendingMap.put(rsPend.getInt("productID"), rsPend.getInt("totalPending"));
                }
            } catch (Exception e) {
                System.out.println("Pending orders safely bypassed.");
            }

            String sql = "SELECT * FROM PRODUCT WHERE status = 'Active'";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("productID");
                    String name = rs.getString("productName");
                    String category = rs.getString("category");
                    int stock = rs.getInt("stockQuantity");
                    int pendingQty = pendingMap.getOrDefault(id, 0);

                    // Safely extract supplier Name
                    String supplier = "General Supplier";
                    try {
                        supplier = rs.getString("supplierName");
                        if (supplier == null || supplier.isEmpty()) supplier = "General Supplier";
                    } catch (Exception ignored) {} // Fallback if column isn't properly created yet

                    String prefix = (category != null && category.length() >= 3) ? category.substring(0, 3).toUpperCase() : "ITM";
                    String sku = String.format("%s-%04d", prefix, id);

                    int[] recs = SupplyChainEngine.getRestockRecommendations(id);
                    int dynamicRop = recs[0] > 0 ? recs[0] : 5;
                    int recommendedEoq = recs[1] > 0 ? recs[1] : 10;

                    // Inject supplier into the StockItem
                    masterData.add(new StockItem(id, sku, name, category, stock, pendingQty, dynamicRop, recommendedEoq, supplier));
                }
            }
        } catch (Exception e) {
            javafx.application.Platform.runLater(() -> showThemedAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage()));
            e.printStackTrace();
        }

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<StockItem> sortedData = new SortedList<>(filteredData);

        sortedData.setComparator((p1, p2) -> {
            int severity1 = p1.getStatus().equals("CRITICAL") ? 0 : p1.getStatus().equals("LOW") ? 1 : 2;
            int severity2 = p2.getStatus().equals("CRITICAL") ? 0 : p2.getStatus().equals("LOW") ? 1 : 2;
            if (severity1 != severity2) return Integer.compare(severity1, severity2);
            return Integer.compare(p1.getStock(), p2.getStock());
        });

        sortedData.comparatorProperty().bind(stockTable.comparatorProperty());
        stockTable.setItems(sortedData);
    }

    private void applyFilter() {
        filteredData.setPredicate(item -> {
            if (btnFilterAll.isSelected()) return true;
            if (btnFilterCritical.isSelected() && item.getStatus().equals("CRITICAL")) return true;
            if (btnFilterLow.isSelected() && item.getStatus().equals("LOW")) return true;
            if (btnFilterOk.isSelected() && item.getStatus().equals("OK")) return true;
            return false;
        });
    }

    private void showRestockDialog(StockItem item) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Restock Item");

        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label infoLbl = new Label(item.getSku() + " — " + item.getName() + "\n(Current: " + item.getStock() + " units)");
        infoLbl.setStyle("-fx-font-family: 'IBM Plex Sans'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -kmtm-text;");

        TextField qtyField = new TextField();
        qtyField.setPromptText("Quantity to add");
        qtyField.setText(String.valueOf(item.getEoq()));
        qtyField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null));

        content.getChildren().addAll(infoLbl, qtyField);

        ButtonType btnAdd = new ButtonType("Add Stock", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL, btnAdd);
        dialogPane.setContent(content);

        dialog.setResultConverter(b -> b == btnAdd && !qtyField.getText().isEmpty() ? Integer.parseInt(qtyField.getText()) : null);
        dialog.showAndWait().ifPresent(qtyToAdd -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("UPDATE PRODUCT SET stockQuantity = stockQuantity + ? WHERE productID = ?");
                stmt.setInt(1, qtyToAdd);
                stmt.setInt(2, item.getDbId());
                stmt.executeUpdate();
                loadInventoryData();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @FXML
    protected void onDraftSupplierOrder() {
        ObservableList<PoItem> draftOrder = FXCollections.observableArrayList();
        ObservableList<String> uniqueSuppliers = FXCollections.observableArrayList();

        // Dynamically build list of active suppliers from inventory
        for (StockItem item : masterData) {
            if (!uniqueSuppliers.contains(item.getSupplier())) {
                uniqueSuppliers.add(item.getSupplier());
            }
        }
        if (uniqueSuppliers.isEmpty()) uniqueSuppliers.add("General Supplier");

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Draft Purchase Order");

        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        HBox topBox = new HBox(10);
        topBox.setAlignment(Pos.CENTER_LEFT);
        Label suppLbl = new Label("Supplier:");
        suppLbl.setStyle("-fx-text-fill: -kmtm-text;");

        ComboBox<String> supplierBox = new ComboBox<>(uniqueSuppliers);
        supplierBox.setPrefWidth(350);
        HBox.setHgrow(supplierBox, Priority.ALWAYS);

        topBox.getChildren().addAll(suppLbl, supplierBox);

        TableView<PoItem> draftTable = new TableView<>(draftOrder);
        draftTable.setPrefHeight(250);

        TableColumn<PoItem, String> colSku = new TableColumn<>("SKU");
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<PoItem, String> colItemName = new TableColumn<>("ITEM NAME");
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colItemName.setPrefWidth(200);

        TableColumn<PoItem, PoItem> colQty = new TableColumn<>("QTY");
        colQty.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colQty.setCellFactory(tc -> new TableCell<PoItem, PoItem>() {
            @Override
            protected void updateItem(PoItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    TextField qtyInput = new TextField(String.valueOf(item.getQty()));
                    qtyInput.setPrefWidth(60);
                    qtyInput.textProperty().addListener((obs, oldV, newV) -> {
                        if (newV.matches("\\d+")) item.setQty(Integer.parseInt(newV));
                    });
                    setGraphic(qtyInput);
                }
            }
        });

        TableColumn<PoItem, PoItem> colRm = new TableColumn<>("X");
        colRm.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colRm.setCellFactory(tc -> new TableCell<PoItem, PoItem>() {
            @Override
            protected void updateItem(PoItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    Button btnRm = new Button("✕");
                    btnRm.setStyle("-fx-background-color: transparent; -fx-text-fill: #f03d3d; -fx-cursor: hand;");
                    btnRm.setOnAction(e -> draftOrder.remove(item));
                    setGraphic(btnRm);
                }
            }
        });

        draftTable.getColumns().addAll(colSku, colItemName, colQty, colRm);

        HBox addBox = new HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> allItemsBox = new ComboBox<>();
        allItemsBox.setPromptText("Select any item from this supplier...");
        allItemsBox.setPrefWidth(250);
        Button btnAddManual = new Button("Add to Order");

        // --- NEW: DYNAMIC SUPPLIER FILTERING LOGIC ---
        supplierBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                draftOrder.clear();
                allItemsBox.getItems().clear();

                for (StockItem item : masterData) {
                    if (item.getSupplier().equalsIgnoreCase(newVal)) {
                        // Populate manual addition combobox
                        allItemsBox.getItems().add(item.getSku() + " - " + item.getName());

                        // Auto-add critical/low items to the PO draft
                        if ((item.getStatus().equals("CRITICAL") || item.getStatus().equals("LOW")) && item.getPendingQty() == 0) {
                            draftOrder.add(new PoItem(item.getDbId(), item.getSku(), item.getName(), item.getEoq()));
                        }
                    }
                }
            }
        });

        btnAddManual.setOnAction(e -> {
            String selection = allItemsBox.getValue();
            if (selection == null) return;
            String selectedSku = selection.split(" - ")[0];

            for (StockItem si : masterData) {
                if (si.getSku().equals(selectedSku)) {
                    if (si.getPendingQty() > 0) {
                        showThemedAlert(Alert.AlertType.CONFIRMATION, "Warning: Double Order", "There are already " + si.getPendingQty() + " units on order! Are you sure you want to order more?");
                    }
                    draftOrder.add(new PoItem(si.getDbId(), si.getSku(), si.getName(), si.getEoq()));
                    allItemsBox.setValue(null);
                    break;
                }
            }
        });
        addBox.getChildren().addAll(allItemsBox, btnAddManual);

        VBox content = new VBox(15, topBox, draftTable, addBox);
        content.setPadding(new Insets(20));
        dialogPane.setContent(content);

        ButtonType btnSavePrint = new ButtonType("Save & Print PO", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL, btnSavePrint);

        // Force selection to trigger the population of tables
        supplierBox.getSelectionModel().selectFirst();

        dialog.setResultConverter(b -> b == btnSavePrint ? true : null);
        dialog.showAndWait().ifPresent(confirmed -> {
            if (draftOrder.isEmpty()) return;

            String selectedSupplier = supplierBox.getValue() != null ? supplierBox.getValue() : "Unknown Supplier";

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                String poSql = "INSERT INTO PURCHASE_ORDER (supplierName, dateCreated, status, createdBy) VALUES (?, ?, 'Pending', ?)";
                PreparedStatement poStmt = conn.prepareStatement(poSql, Statement.RETURN_GENERATED_KEYS);
                poStmt.setString(1, selectedSupplier);
                poStmt.setString(2, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                poStmt.setString(3, UserSession.getInstance().getName());
                poStmt.executeUpdate();

                ResultSet rs = poStmt.getGeneratedKeys();
                int newPoId = 0;
                if (rs.next()) newPoId = rs.getInt(1);

                String itemSql = "INSERT INTO PO_ITEM (poId, productID, orderQty) VALUES (?, ?, ?)";
                PreparedStatement itemStmt = conn.prepareStatement(itemSql);
                for (PoItem item : draftOrder) {
                    itemStmt.setInt(1, newPoId);
                    itemStmt.setInt(2, item.getDbId());
                    itemStmt.setInt(3, item.getQty());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
                conn.commit();

                executeFinalPrintout(draftOrder, selectedSupplier, newPoId);
                loadInventoryData();

            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @FXML
    protected void onViewPendingOrders() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Pending Purchase Orders");

        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        ListView<HBox> poListView = new ListView<>();
        poListView.setPrefSize(480, 300);
        poListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-background: transparent;");

        poListView.setCellFactory(lv -> new ListCell<HBox>() {
            @Override
            protected void updateItem(HBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(item);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                }
            }
        });

        refreshPendingOrdersList(poListView);

        dialogPane.setContent(poListView);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private void refreshPendingOrdersList(ListView<HBox> poListView) {
        poListView.getItems().clear();

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM PURCHASE_ORDER WHERE status = 'Pending'");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int poId = rs.getInt("poId");
                String supp = rs.getString("supplierName");
                String date = rs.getString("dateCreated");

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 10; -fx-border-color: transparent transparent -kmtm-border transparent; -fx-border-width: 0 0 1 0;");

                VBox info = new VBox();
                Label lblPO = new Label("PO #" + poId + " - " + supp);
                lblPO.setStyle("-fx-font-weight: bold; -fx-text-fill: -kmtm-text;");
                Label lblDate = new Label("Ordered: " + date);
                lblDate.setStyle("-fx-font-size: 10px; -fx-text-fill: -kmtm-text-dim;");
                info.getChildren().addAll(lblPO, lblDate);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button btnReceive = new Button("Receive");
                btnReceive.setStyle("-fx-background-color: #3adf8a; -fx-text-fill: #151819; -fx-font-weight: bold; -fx-cursor: hand;");
                btnReceive.setOnAction(e -> {
                    processReceivedOrder(poId);
                    refreshPendingOrdersList(poListView);
                });

                Button btnCancel = new Button("Cancel");
                btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #f03d3d; -fx-border-color: #f03d3d; -fx-border-radius: 4; -fx-cursor: hand;");
                btnCancel.setOnAction(e -> {
                    cancelPendingOrder(poId);
                    refreshPendingOrdersList(poListView);
                });

                row.getChildren().addAll(info, spacer, btnCancel, btnReceive);
                poListView.getItems().add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (poListView.getItems().isEmpty()) {
            Label emptyLbl = new Label("No pending orders.");
            emptyLbl.setStyle("-fx-padding: 10; -fx-text-fill: -kmtm-text;");
            poListView.getItems().add(new HBox(emptyLbl));
        }
    }

    private void processReceivedOrder(int poId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement updatePo = conn.prepareStatement("UPDATE PURCHASE_ORDER SET status = 'Received' WHERE poId = ?");
            updatePo.setInt(1, poId);
            updatePo.executeUpdate();

            PreparedStatement getItems = conn.prepareStatement("SELECT productID, orderQty FROM PO_ITEM WHERE poId = ?");
            getItems.setInt(1, poId);
            ResultSet rsItems = getItems.executeQuery();

            PreparedStatement updateStock = conn.prepareStatement("UPDATE PRODUCT SET stockQuantity = stockQuantity + ? WHERE productID = ?");
            while (rsItems.next()) {
                updateStock.setInt(1, rsItems.getInt("orderQty"));
                updateStock.setInt(2, rsItems.getInt("productID"));
                updateStock.addBatch();
            }
            updateStock.executeBatch();
            conn.commit();

            loadInventoryData();
            showThemedAlert(Alert.AlertType.INFORMATION, "Order Received", "Inventory stock has been automatically updated.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cancelPendingOrder(int poId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement updatePo = conn.prepareStatement("UPDATE PURCHASE_ORDER SET status = 'Cancelled' WHERE poId = ?");
            updatePo.setInt(1, poId);
            updatePo.executeUpdate();

            loadInventoryData();
            showThemedAlert(Alert.AlertType.INFORMATION, "Order Cancelled", "PO #" + poId + " has been cancelled. Quantities removed from pending status.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void executeFinalPrintout(ObservableList<PoItem> finalOrder, String supplier, int poId) {
        VBox receiptBox = new VBox(10);
        receiptBox.setPadding(new Insets(30));
        receiptBox.setStyle("-fx-background-color: white; -fx-font-family: 'Monospaced'; -fx-font-size: 12px;");

        Label title = new Label("KaMotoMo Management System\nOFFICIAL PURCHASE ORDER");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: black;");

        Label details = new Label("PO Number: #" + poId + "\nSupplier: " + supplier + "\nDate: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
        details.setStyle("-fx-text-fill: black;");

        Label divider = new Label("------------------------------------------------------------");
        divider.setStyle("-fx-text-fill: black;");

        VBox itemsBox = new VBox(5);
        Label headerRow = new Label(String.format("%-12s | %-30s | %-12s", "SKU", "ITEM NAME", "ORDER QTY"));
        headerRow.setStyle("-fx-text-fill: black;");
        itemsBox.getChildren().add(headerRow);

        Label headerDiv = new Label("------------------------------------------------------------");
        headerDiv.setStyle("-fx-text-fill: black;");
        itemsBox.getChildren().add(headerDiv);

        for (PoItem item : finalOrder) {
            Label row = new Label(String.format("%-12s | %-30.30s | %-12d", item.getSku(), item.getName(), item.getQty()));
            row.setStyle("-fx-text-fill: black;");
            itemsBox.getChildren().add(row);
        }

        Label sig = new Label("\n\n\nAuthorized Signature: _____________________");
        sig.setStyle("-fx-text-fill: black;");

        receiptBox.getChildren().addAll(title, details, divider, itemsBox, sig);

        Dialog<Void> receiptDialog = new Dialog<>();
        receiptDialog.setTitle("Purchase Order Preview");

        DialogPane dialogPane = receiptDialog.getDialogPane();

        HBox dialogContainer = new HBox(receiptBox);
        dialogContainer.setAlignment(Pos.CENTER);
        dialogContainer.setPadding(new Insets(20));
        dialogPane.setContent(dialogContainer);

        ButtonType printType = new ButtonType("🖨 Print Order", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(printType, closeType);

        receiptDialog.setResultConverter(dialogButton -> {
            if (dialogButton == printType) {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(receiptDialog.getOwner())) {
                    boolean success = job.printPage(receiptBox);
                    if (success) job.endJob();
                }
            }
            return null;
        });

        receiptDialog.showAndWait();
    }

    public static class StockItem {
        private final int dbId;
        private final String sku;
        private final String name;
        private final String category;
        private final int stock;
        private final String status;
        private final int rop;
        private final int pendingQty;
        private final int eoq;
        private final String supplier;

        public StockItem(int dbId, String sku, String name, String category, int stock, int pendingQty, int rop, int eoq, String supplier) {
            this.dbId = dbId;
            this.sku = sku;
            this.name = name;
            this.category = category == null ? "Uncategorized" : category;
            this.stock = stock;
            this.pendingQty = pendingQty;
            this.rop = rop;
            this.eoq = eoq;
            this.supplier = supplier == null ? "General Supplier" : supplier;

            if (this.stock <= (this.rop / 2)) this.status = "CRITICAL";
            else if (this.stock <= this.rop) this.status = "LOW";
            else this.status = "OK";
        }
        public int getDbId() { return dbId; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public int getStock() { return stock; }
        public String getStatus() { return status; }
        public int getRop() { return rop; }
        public int getEoq() { return eoq; }
        public int getPendingQty() { return pendingQty; }
        public String getSupplier() { return supplier; }
    }

    public static class PoItem {
        private final int dbId;
        private final String sku;
        private final String name;
        private int qty;

        public PoItem(int dbId, String sku, String name, int qty) {
            this.dbId = dbId;
            this.sku = sku;
            this.name = name;
            this.qty = qty;
        }
        public int getDbId() { return dbId; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }
    }
}