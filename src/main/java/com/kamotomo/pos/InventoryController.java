package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.models.Product;
import com.kamotomo.pos.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class InventoryController {

    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, String> colStatus;
    @FXML private TableColumn<Product, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Button toggleArchiveBtn;
    @FXML private Button btnAddProduct;

    private ObservableList<Product> masterData = FXCollections.observableArrayList();
    private FilteredList<Product> filteredData;
    private boolean showingArchived = false;
    private boolean isAlertShowing = false;

    @FXML
    public void initialize() {
        categoryFilter.getItems().addAll("All Categories", "Engine Parts", "Brakes", "Tires & Wheels", "Electrical", "Filters", "Suspension", "Exhaust", "Lubricants & Fluids", "Body Parts", "Accessories");
        categoryFilter.setValue("All Categories");

        setupCustomColumns();

        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        inventoryTable.getColumns().forEach(col -> col.setReorderable(false));

        filteredData = new FilteredList<>(masterData, p -> true);
        loadDataFromDatabase();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        inventoryTable.setItems(filteredData);

        if ("Employee".equalsIgnoreCase(UserSession.getInstance().getRole())) {
            if (btnAddProduct != null) {
                btnAddProduct.setVisible(false);
                btnAddProduct.setManaged(false);
            }
            if (colActions != null) {
                colActions.setVisible(false);
            }
        }
    }

    private void loadDataFromDatabase() {
        masterData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM PRODUCT";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                masterData.add(new Product(
                        rs.getInt("productID"), rs.getString("productName"), rs.getDouble("price"),
                        rs.getInt("stockQuantity"), rs.getString("category"), rs.getString("status")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String cat = categoryFilter.getValue();

        filteredData.setPredicate(product -> {
            if (!showingArchived && product.getStatus().equals("Archived")) return false;
            if (showingArchived && product.getStatus().equals("Active")) return false;
            if (cat != null && !cat.equals("All Categories") && !product.getCategory().equals(cat)) return false;

            if (searchText == null || searchText.isEmpty()) return true;

            String prefix = product.getCategory() != null && product.getCategory().length() >= 3 ? product.getCategory().substring(0, 3).toUpperCase() : "ITM";
            String displayId = String.format("%s-%04d", prefix, product.getId()).toLowerCase();

            return product.getName().toLowerCase().contains(searchText) ||
                    displayId.contains(searchText) ||
                    String.valueOf(product.getId()).contains(searchText);
        });
    }

    @FXML
    protected void onToggleArchiveClick() {
        showingArchived = !showingArchived;
        toggleArchiveBtn.setText(showingArchived ? "Show Active" : "Show Archived");

        if (showingArchived) {
            toggleArchiveBtn.setStyle("-fx-background-color: -kmtm-primary-glow; -fx-text-fill: -kmtm-primary; -fx-border-color: -kmtm-primary; -fx-border-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 13px; -fx-padding: 9 14; -fx-cursor: hand;");
        } else {
            toggleArchiveBtn.setStyle("-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 13px; -fx-padding: 9 14; -fx-cursor: hand;");
        }
        applyFilters();
    }

    @FXML
    protected void onAddProductClick() {
        showProductDialog(null);
    }

    private void setupCustomColumns() {
        colId.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Product p = getTableRow().getItem();
                    String cat = p.getCategory();
                    String prefix = (cat != null && cat.length() >= 3) ? cat.substring(0, 3).toUpperCase() : "ITM";
                    setText(String.format("%s-%04d", prefix, item));
                    setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-text-fill: -kmtm-text-dim;");
                }
            }
        });

        colCategory.setCellFactory(column -> new TableCell<Product, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    Label badge = new Label(item.toUpperCase());
                    badge.setStyle("-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text-muted; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
                    setGraphic(badge);
                }
            }
        });

        colPrice.setCellFactory(column -> new TableCell<Product, Double>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(String.format("₱%.2f", item));
                    setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-text-fill: -kmtm-text;");
                }
            }
        });

        colStock.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(String.valueOf(item));
                    setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-text-fill: -kmtm-text;");
                }
            }
        });

        colStatus.setCellFactory(column -> new TableCell<Product, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) setGraphic(null);
                else {
                    Product p = getTableRow().getItem();
                    Label badge = new Label();
                    badge.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");

                    if (p.getStatus().equals("Archived")) {
                        badge.setText("ARCHIVED");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text-dim;");
                    } else if (p.getStock() <= 0) {
                        badge.setText("OUT OF STOCK");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(240, 61, 61, 0.1); -fx-text-fill: #f03d3d;");
                    } else {
                        badge.setText("ACTIVE");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(58, 223, 138, 0.1); -fx-text-fill: #3adf8a;");
                    }
                    setGraphic(badge);
                }
            }
        });

        colActions.setCellFactory(new Callback<TableColumn<Product, Void>, TableCell<Product, Void>>() {
            @Override
            public TableCell<Product, Void> call(final TableColumn<Product, Void> param) {
                return new TableCell<Product, Void>() {
                    private final Button editBtn = new Button("Edit");
                    private final Button archiveBtn = new Button("Archive");
                    private final HBox pane = new HBox(8, editBtn, archiveBtn);

                    {
                        pane.setAlignment(Pos.CENTER_LEFT);
                        String btnStyle = "-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand;";
                        editBtn.setStyle(btnStyle);
                        archiveBtn.setStyle(btnStyle);

                        editBtn.setOnAction(event -> {
                            Product data = getTableView().getItems().get(getIndex());
                            showProductDialog(data);
                        });

                        archiveBtn.setOnAction(event -> {
                            Product data = getTableView().getItems().get(getIndex());
                            toggleProductStatus(data);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) setGraphic(null);
                        else {
                            Product p = getTableView().getItems().get(getIndex());
                            archiveBtn.setText(p.getStatus().equals("Archived") ? "Restore" : "Archive");
                            setGraphic(pane);
                        }
                    }
                };
            }
        });
    }

    private void toggleProductStatus(Product p) {
        String newStatus = p.getStatus().equals("Archived") ? "Active" : "Archived";
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE PRODUCT SET status = ? WHERE productID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newStatus);
            stmt.setInt(2, p.getId());
            stmt.executeUpdate();

            com.kamotomo.pos.utils.SystemLogger.logAction("Inventory", "Changed status of product ID " + p.getId() + " to " + newStatus);

            String actionWord = newStatus.equals("Archived") ? "archived." : "restored and made active.";
            showSuccessPopup("Status Updated", "The product '" + p.getName() + "' has been successfully " + actionWord);

            loadDataFromDatabase();
        } catch (Exception e) {
            showErrorPopup("Database Error", "We encountered a problem changing the status. Please contact your administrator.");
        }
    }

    private void showProductDialog(Product existingProduct) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.initOwner(inventoryTable.getScene().getWindow());
        dialog.initStyle(StageStyle.UNDECORATED);

        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        HBox header = new HBox();
        header.getStyleClass().add("dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(existingProduct == null ? "ADD PRODUCT" : "EDIT PRODUCT");
        titleLabel.getStyleClass().add("dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("dialog-close-btn");
        closeBtn.setOnAction(e -> {
            Window window = dialogPane.getScene().getWindow();
            window.hide();
        });

        header.getChildren().addAll(titleLabel, spacer, closeBtn);

        final double[] xOffset = {0};
        final double[] yOffset = {0};
        header.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        header.setOnMouseDragged(event -> {
            Window window = dialogPane.getScene().getWindow();
            window.setX(event.getScreenX() - xOffset[0]);
            window.setY(event.getScreenY() - yOffset[0]);
        });

        dialogPane.setHeader(header);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Engine Oil 10W-40");
        nameField.setStyle("-fx-padding: 8;");

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("Engine Parts", "Brakes", "Tires & Wheels", "Electrical", "Filters", "Suspension", "Exhaust", "Lubricants & Fluids", "Body Parts", "Accessories");
        catBox.setMaxWidth(Double.MAX_VALUE);

        TextField priceField = new TextField();
        priceField.setPromptText("0.00");
        priceField.setStyle("-fx-padding: 8;");

        TextField stockField = new TextField();
        stockField.setPromptText("0");
        stockField.setStyle("-fx-padding: 8;");

        ComboBox<String> supplierBox = new ComboBox<>();
        supplierBox.setEditable(true);
        supplierBox.setMaxWidth(Double.MAX_VALUE);
        supplierBox.setPromptText("Select or type new...");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT supplierName FROM PRODUCT WHERE supplierName IS NOT NULL AND supplierName != ''");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                supplierBox.getItems().add(rs.getString("supplierName"));
            }
        } catch (Exception e) {
            supplierBox.getItems().add("General Supplier");
        }

        TextField leadTimeField = new TextField("7");
        leadTimeField.setPromptText("Days to arrive");
        leadTimeField.setStyle("-fx-padding: 8;");

        TextField safetyStockField = new TextField("5");
        safetyStockField.setPromptText("Emergency buffer");
        safetyStockField.setStyle("-fx-padding: 8;");

        TextField orderCostField = new TextField("150.00");
        orderCostField.setPromptText("0.00");
        orderCostField.setStyle("-fx-padding: 8;");

        TextField holdingCostField = new TextField("15.00");
        holdingCostField.setPromptText("0.00");
        holdingCostField.setStyle("-fx-padding: 8;");

        TextField editReasonField = new TextField();

        if (existingProduct != null) {
            nameField.setText(existingProduct.getName());
            catBox.setValue(existingProduct.getCategory());
            priceField.setText(String.valueOf(existingProduct.getPrice()));
            stockField.setText(String.valueOf(existingProduct.getStock()));

            try (Connection conn = DatabaseConnection.getConnection()) {
                String extendedSql = "SELECT supplierName, supplierLeadTimeDays, safetyStock, orderCost, holdingCost FROM PRODUCT WHERE productID = ?";
                PreparedStatement extStmt = conn.prepareStatement(extendedSql);
                extStmt.setInt(1, existingProduct.getId());
                ResultSet rs = extStmt.executeQuery();
                if (rs.next()) {
                    supplierBox.setValue(rs.getString("supplierName"));
                    leadTimeField.setText(String.valueOf(rs.getInt("supplierLeadTimeDays")));
                    safetyStockField.setText(String.valueOf(rs.getInt("safetyStock")));
                    orderCostField.setText(String.valueOf(rs.getDouble("orderCost")));
                    holdingCostField.setText(String.valueOf(rs.getDouble("holdingCost")));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 20, 10, 20));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        Label catLbl = new Label("CATEGORY");
        Label suppLbl = new Label("SUPPLIER");
        Label nameLbl = new Label("PRODUCT NAME");
        Label priceLbl = new Label("PRICE (₱)");
        Label stockLbl = new Label("STOCK QUANTITY");

        Label scHeaderLbl = new Label("SUPPLY CHAIN (EOQ / ROP VARIABLES)");
        scHeaderLbl.setStyle("-fx-text-fill: -kmtm-primary; -fx-font-weight: bold; -fx-font-size: 11px;");

        Label leadLbl = new Label("LEAD TIME (DAYS)");
        Label safetyLbl = new Label("SAFETY STOCK");
        Label orderCostLbl = new Label("ORDERING COST (₱)");
        Label holdingCostLbl = new Label("HOLDING COST (₱)");

        grid.add(new VBox(5, catLbl, catBox), 0, 0);
        grid.add(new VBox(5, suppLbl, supplierBox), 1, 0);

        grid.add(new VBox(5, nameLbl, nameField), 0, 1, 2, 1);

        grid.add(new VBox(5, priceLbl, priceField), 0, 2);
        grid.add(new VBox(5, stockLbl, stockField), 1, 2);

        Separator sep = new Separator();
        sep.setPadding(new Insets(10, 0, 5, 0));
        grid.add(sep, 0, 3, 2, 1);
        grid.add(scHeaderLbl, 0, 4, 2, 1);

        grid.add(new VBox(5, leadLbl, leadTimeField), 0, 5);
        grid.add(new VBox(5, safetyLbl, safetyStockField), 1, 5);
        grid.add(new VBox(5, orderCostLbl, orderCostField), 0, 6);
        grid.add(new VBox(5, holdingCostLbl, holdingCostField), 1, 6);

        if (existingProduct != null) {
            Separator sep2 = new Separator();
            sep2.setPadding(new Insets(10, 0, 5, 0));
            grid.add(sep2, 0, 7, 2, 1);

            Label editReasonLbl = new Label("REASON FOR EDIT (REQUIRED)");
            editReasonLbl.setStyle("-fx-text-fill: #f03d3d; -fx-font-weight: bold;");
            editReasonField.setPromptText("e.g. Price update from supplier, correcting typo...");
            editReasonField.setStyle("-fx-padding: 8;");
            grid.add(new VBox(5, editReasonLbl, editReasonField), 0, 8, 2, 1);
        }

        dialogPane.setContent(grid);

        ButtonType saveButtonType = new ButtonType(existingProduct == null ? "Save Product" : "Update Product", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        final Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String name = nameField.getText().trim();
            String priceStr = priceField.getText().trim().replace(",", "");
            String stockStr = stockField.getText().trim();
            String leadStr = leadTimeField.getText().trim();
            String safetyStr = safetyStockField.getText().trim();
            String ordCostStr = orderCostField.getText().trim().replace(",", "");
            String holdCostStr = holdingCostField.getText().trim().replace(",", "");
            String reason = editReasonField.getText().trim();

            if (name.isEmpty() || name.length() > 50) {
                showErrorPopup("Invalid Name", "Please enter a valid product name. It cannot be empty or longer than 50 characters.");
                event.consume();
                return;
            }

            if (catBox.getValue() == null) {
                showErrorPopup("Missing Category", "Please click the dropdown menu and select a Category for this product.");
                event.consume();
                return;
            }

            if (supplierBox.getValue() == null || supplierBox.getValue().trim().isEmpty()) {
                showErrorPopup("Missing Supplier", "Please select or type a Supplier for this product.");
                event.consume();
                return;
            }

            try {
                if (Double.parseDouble(priceStr) < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showErrorPopup("Invalid Price", "The price you entered is not recognized. Please type a valid amount.");
                event.consume();
                return;
            }

            try {
                if (Integer.parseInt(stockStr) < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showErrorPopup("Invalid Stock", "The stock quantity must be a whole positive number.");
                event.consume();
                return;
            }

            try {
                if (Integer.parseInt(leadStr) < 0 || Integer.parseInt(safetyStr) < 0) throw new NumberFormatException();
                if (Double.parseDouble(ordCostStr) < 0 || Double.parseDouble(holdCostStr) < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showErrorPopup("Invalid Supply Chain Data", "All supply chain fields must be valid positive numbers. Remove any letters.");
                event.consume();
                return;
            }

            if (existingProduct != null && reason.isEmpty()) {
                showErrorPopup("Reason Required", "You must provide a clear reason for modifying this product to maintain the system audit trail.");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String name = nameField.getText().trim();
                    String cat = catBox.getValue();
                    String supp = supplierBox.getValue().trim();
                    double price = Double.parseDouble(priceField.getText().trim().replace(",", ""));
                    int stock = Integer.parseInt(stockField.getText().trim());

                    int leadTime = Integer.parseInt(leadTimeField.getText().trim());
                    int safetyStock = Integer.parseInt(safetyStockField.getText().trim());
                    double orderCost = Double.parseDouble(orderCostField.getText().trim().replace(",", ""));
                    double holdingCost = Double.parseDouble(holdingCostField.getText().trim().replace(",", ""));
                    String reason = editReasonField.getText().trim();

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        if (existingProduct == null) {
                            String sql = "INSERT INTO PRODUCT (productName, category, price, stockQuantity, status, supplierLeadTimeDays, safetyStock, orderCost, holdingCost, supplierName) VALUES (?, ?, ?, ?, 'Active', ?, ?, ?, ?, ?)";
                            com.kamotomo.pos.utils.SystemLogger.logAction("Inventory", "Added new product: " + name);
                            PreparedStatement stmt = conn.prepareStatement(sql);
                            stmt.setString(1, name); stmt.setString(2, cat); stmt.setDouble(3, price); stmt.setInt(4, stock);
                            stmt.setInt(5, leadTime); stmt.setInt(6, safetyStock); stmt.setDouble(7, orderCost); stmt.setDouble(8, holdingCost);
                            stmt.setString(9, supp);
                            stmt.executeUpdate();

                            showSuccessPopup("Product Added", "You have successfully added '" + name + "' to the active inventory.");
                        } else {
                            String sql = "UPDATE PRODUCT SET productName=?, category=?, price=?, stockQuantity=?, supplierLeadTimeDays=?, safetyStock=?, orderCost=?, holdingCost=?, supplierName=? WHERE productID=?";
                            com.kamotomo.pos.utils.SystemLogger.logAction("Inventory", "Updated product ID " + existingProduct.getId() + " - Reason: " + reason);
                            PreparedStatement stmt = conn.prepareStatement(sql);
                            stmt.setString(1, name); stmt.setString(2, cat); stmt.setDouble(3, price); stmt.setInt(4, stock);
                            stmt.setInt(5, leadTime); stmt.setInt(6, safetyStock); stmt.setDouble(7, orderCost); stmt.setDouble(8, holdingCost);
                            stmt.setString(9, supp);
                            stmt.setInt(10, existingProduct.getId());
                            stmt.executeUpdate();

                            showSuccessPopup("Product Updated", "You have successfully updated the details for '" + name + "'.");
                        }
                    }
                    loadDataFromDatabase();
                } catch (Exception e) {
                    showErrorPopup("Database Error", "We encountered a problem saving this product. Please check if the product name already exists.");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void applyThemeToDialog(DialogPane dialogPane) {
        if (inventoryTable == null || inventoryTable.getScene() == null) return;

        Scene mainScene = inventoryTable.getScene();

        dialogPane.getStylesheets().setAll(mainScene.getStylesheets());
        if (mainScene.getRoot() != null) {
            dialogPane.getStylesheets().addAll(mainScene.getRoot().getStylesheets());
        }

        if (!dialogPane.getStyleClass().contains("custom-dialog")) {
            dialogPane.getStyleClass().addAll("custom-dialog", "root");
        }
    }

    private void showErrorPopup(String title, String message) {
        if (isAlertShowing) return;
        isAlertShowing = true;

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        applyThemeToDialog(dialogPane);

        alert.showAndWait();
        isAlertShowing = false;
    }

    private void showSuccessPopup(String title, String message) {
        if (isAlertShowing) return;
        isAlertShowing = true;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        applyThemeToDialog(dialogPane);

        alert.showAndWait();
        isAlertShowing = false;
    }
}