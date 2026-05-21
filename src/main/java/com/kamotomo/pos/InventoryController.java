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

        // --- ROLE-BASED INVENTORY LOCKDOWN ---
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

            String prefix = product.getCategory().length() >= 3 ? product.getCategory().substring(0, 3).toUpperCase() : "ITM";
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
                    String prefix = p.getCategory().length() >= 3 ? p.getCategory().substring(0, 3).toUpperCase() : "ITM";
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

                    int rop = (2 * 2) + 5;

                    if (p.getStatus().equals("Archived")) {
                        badge.setText("ARCHIVED");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text-dim;");
                    } else if (p.getStock() <= (rop / 2)) {
                        badge.setText("CRITICAL");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(240, 61, 61, 0.1); -fx-text-fill: #f03d3d;");
                    } else if (p.getStock() <= rop) {
                        badge.setText("LOW");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(234, 179, 8, 0.1); -fx-text-fill: #eab308;");
                    } else {
                        badge.setText("IN STOCK");
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

            // --- LOGGING ADDED HERE ---
            com.kamotomo.pos.utils.SystemLogger.logAction("Inventory", "Changed status of product ID " + p.getId() + " to " + newStatus);

            loadDataFromDatabase();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showProductDialog(Product existingProduct) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.initOwner(inventoryTable.getScene().getWindow());
        dialog.initStyle(StageStyle.UNDECORATED);

        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);
        dialogPane.getStyleClass().add("custom-dialog");

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

        if (existingProduct != null) {
            nameField.setText(existingProduct.getName());
            catBox.setValue(existingProduct.getCategory());
            priceField.setText(String.valueOf(existingProduct.getPrice()));
            stockField.setText(String.valueOf(existingProduct.getStock()));
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
        Label nameLbl = new Label("PRODUCT NAME");
        Label priceLbl = new Label("PRICE (₱)");
        Label stockLbl = new Label("STOCK QUANTITY");

        VBox catBoxContainer = new VBox(5, catLbl, catBox);
        grid.add(catBoxContainer, 0, 0, 2, 1);

        VBox nameContainer = new VBox(5, nameLbl, nameField);
        grid.add(nameContainer, 0, 1, 2, 1);

        VBox priceContainer = new VBox(5, priceLbl, priceField);
        VBox stockContainer = new VBox(5, stockLbl, stockField);
        grid.add(priceContainer, 0, 2);
        grid.add(stockContainer, 1, 2);

        dialogPane.setContent(grid);

        ButtonType saveButtonType = new ButtonType(existingProduct == null ? "Save Product" : "Update Product", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String name = nameField.getText();
                    String cat = catBox.getValue();
                    double price = Double.parseDouble(priceField.getText());
                    int stock = Integer.parseInt(stockField.getText());

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        if (existingProduct == null) {
                            String sql = "INSERT INTO PRODUCT (productName, category, price, stockQuantity, status) VALUES (?, ?, ?, ?, 'Active')";
                            com.kamotomo.pos.utils.SystemLogger.logAction("Inventory", "Added new product: " + name);
                            PreparedStatement stmt = conn.prepareStatement(sql);
                            stmt.setString(1, name); stmt.setString(2, cat); stmt.setDouble(3, price); stmt.setInt(4, stock);
                            stmt.executeUpdate();
                        } else {
                            String sql = "UPDATE PRODUCT SET productName=?, category=?, price=?, stockQuantity=? WHERE productID=?";
                            com.kamotomo.pos.utils.SystemLogger.logAction("Inventory", "Updated product ID: " + existingProduct.getId());
                            PreparedStatement stmt = conn.prepareStatement(sql);
                            stmt.setString(1, name); stmt.setString(2, cat); stmt.setDouble(3, price); stmt.setInt(4, stock); stmt.setInt(5, existingProduct.getId());
                            stmt.executeUpdate();
                        }
                    }
                    loadDataFromDatabase();
                } catch (Exception e) {
                    System.out.println("Input Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }


    // --- THE TARGETED THEME HUNTER ---
    private void applyThemeToDialog(DialogPane dialogPane) {
        if (inventoryTable == null || inventoryTable.getScene() == null) return;

        // 1. Hunt down the exact active theme URL by walking up the application tree
        String activeThemeUrl = "";
        javafx.scene.Parent current = inventoryTable;

        while (current != null) {
            for (String stylesheet : current.getStylesheets()) {
                if (stylesheet.contains("dark-theme.css") || stylesheet.contains("light-theme.css")) {
                    activeThemeUrl = stylesheet;
                    break;
                }
            }
            if (!activeThemeUrl.isEmpty()) break;
            current = current.getParent(); // Move up to the next wrapper (e.g., VBox -> BorderPane)
        }

        // 2. If it wasn't on the nodes, check the Scene itself
        if (activeThemeUrl.isEmpty()) {
            for (String stylesheet : inventoryTable.getScene().getStylesheets()) {
                if (stylesheet.contains("dark-theme.css") || stylesheet.contains("light-theme.css")) {
                    activeThemeUrl = stylesheet;
                    break;
                }
            }
        }

        // 3. Clear any old/default styles and apply ONLY the correct theme file
        dialogPane.getStylesheets().clear();
        if (!activeThemeUrl.isEmpty()) {
            dialogPane.getStylesheets().add(activeThemeUrl);
        }

        // 4. Ensure the CSS custom variables (-kmtm) are activated via the root class
        if (!dialogPane.getStyleClass().contains("custom-dialog")) {
            dialogPane.getStyleClass().addAll("custom-dialog", "root");
        }
    }
}