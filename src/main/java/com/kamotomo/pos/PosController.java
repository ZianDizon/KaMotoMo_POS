package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.models.CartItem;
import com.kamotomo.pos.models.Product;
import com.kamotomo.pos.utils.CartSession;
import com.kamotomo.pos.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PosController {

    @FXML private TilePane productGrid;
    @FXML private VBox cartContainer;
    @FXML private TextField searchField;

    @FXML private Label originalTotalLabel;
    @FXML private Label totalLabel;
    @FXML private TextField discountField;
    @FXML private ComboBox<String> discountTypeBox;
    @FXML private ComboBox<String> discountReasonBox;

    @FXML private Button btnCash;
    @FXML private Button btnGCash;
    @FXML private VBox cashInputContainer;
    @FXML private VBox gcashInputContainer;
    @FXML private TextField cashTenderedField;
    @FXML private TextField gcashRefField;
    @FXML private Label changeLabel;
    @FXML private Button btnTransactionHistory;

    private List<CartItem> cart;
    private List<Product> allProducts = new ArrayList<>();

    private static List<Product> offlineProductCache = new ArrayList<>();

    private String currentCategory = "All";
    private String selectedPaymentMethod = "Cash";

    private double currentRawTotal = 0.0;
    private double currentDiscountAmount = 0.0;
    private double currentFinalTotal = 0.0;
    private double currentVatAmount = 0.0;
    private double currentVatableSales = 0.0;
    private boolean isAlertShowing = false;

    @FXML
    public void initialize() {
        if ("Employee".equalsIgnoreCase(UserSession.getInstance().getRole())) {
            btnTransactionHistory.setVisible(false);
            btnTransactionHistory.setManaged(false);
        }

        cart = CartSession.getInstance().getCart();

        discountReasonBox.setMinWidth(130);
        discountReasonBox.setPrefWidth(130);
        discountReasonBox.setMaxWidth(130);

        discountTypeBox.getItems().clear();
        discountTypeBox.getItems().addAll("%", "₱");

        discountTypeBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: -kmtm-text; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 13px;");
                }
            }
        });
        discountTypeBox.getSelectionModel().selectFirst();

        discountReasonBox.getItems().setAll("None", "Promo Code", "Senior Citizen", "PWD", "Employee Discount", "Damaged Box", "Wholesale");

        // --- FIXED: DYNAMIC ROUTING FOR ALL PRESETS ---
        discountReasonBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            double rate = 0;
            boolean isPreset = false;

            if ("Senior Citizen".equals(newVal) || "PWD".equals(newVal)) {
                rate = com.kamotomo.pos.utils.SystemSettings.getScPwdDiscountRate();
                isPreset = true;
            } else if ("Wholesale".equals(newVal)) {
                rate = com.kamotomo.pos.utils.SystemSettings.getWholesaleDiscountRate();
                isPreset = true;
            } else if ("Employee Discount".equals(newVal)) {
                rate = com.kamotomo.pos.utils.SystemSettings.getEmployeeDiscountRate();
                isPreset = true;
            }

            if (isPreset) {
                discountTypeBox.setValue("%");
                discountField.setText(String.format("%.0f", rate * 100));
                discountField.setDisable(true);
                discountTypeBox.setDisable(true);
            } else {
                discountField.setDisable(false);
                discountTypeBox.setDisable(false);
                if ("None".equals(newVal)) {
                    discountField.clear();
                }
            }
            refreshCartUI();
        });

        loadProductsFromDatabase();
        refreshCartUI();
        updatePaymentUI();

        discountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) { discountField.setText(oldVal); }
            refreshCartUI();
        });

        discountTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> refreshCartUI());

        cashTenderedField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) { cashTenderedField.setText(oldVal); }
            updateChangeDisplay();
        });

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        gcashRefField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d{0,13}")) { return change; }
            return null;
        }));
    }

    @FXML
    protected void onCashToggleClick() {
        selectedPaymentMethod = "Cash";
        updatePaymentUI();
    }

    @FXML
    protected void onGCashToggleClick() {
        selectedPaymentMethod = "GCash";
        updatePaymentUI();
    }

    private void updatePaymentUI() {
        boolean isGCash = selectedPaymentMethod.equals("GCash");

        String activeStyle = "-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-primary; -fx-border-color: -kmtm-primary; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 10; -fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold;";
        String inactiveStyle = "-fx-background-color: -kmtm-surface; -fx-text-fill: -kmtm-text-dim; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 10; -fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold;";

        btnCash.setStyle(isGCash ? inactiveStyle : activeStyle);
        btnGCash.setStyle(isGCash ? activeStyle : inactiveStyle);

        cashInputContainer.setVisible(!isGCash);
        cashInputContainer.setManaged(!isGCash);

        gcashInputContainer.setVisible(isGCash);
        gcashInputContainer.setManaged(isGCash);

        if (isGCash) {
            cashTenderedField.setText(String.valueOf(currentFinalTotal));
            changeLabel.setText("");
        } else {
            cashTenderedField.clear();
            gcashRefField.clear();
        }
        updateChangeDisplay();
    }

    private void loadProductsFromDatabase() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null || !conn.isValid(2)) {
                throw new Exception("Database is unreachable.");
            }

            allProducts.clear();
            String sql = "SELECT * FROM PRODUCT WHERE status = 'Active'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                allProducts.add(new Product(
                        rs.getInt("productID"),
                        rs.getString("productName"),
                        rs.getDouble("price"),
                        rs.getInt("stockQuantity"),
                        rs.getString("category"),
                        rs.getString("status")
                ));
            }

            offlineProductCache.clear();
            offlineProductCache.addAll(allProducts);

        } catch (Exception e) {
            allProducts.clear();
            allProducts.addAll(offlineProductCache);
            System.out.println("Database offline: Loaded " + allProducts.size() + " items from local cache.");
        }
        applyFilters();
    }

    private void applyFilters() {
        productGrid.getChildren().clear();
        String searchText = searchField != null ? searchField.getText().toLowerCase() : "";

        for (Product product : allProducts) {
            if (product.getStock() <= 0) {
                continue;
            }

            boolean matchesCategory = currentCategory.equals("All") || product.getCategory().equalsIgnoreCase(currentCategory);
            String prefix = product.getCategory().length() >= 3 ? product.getCategory().substring(0, 3).toUpperCase() : "ITM";
            String sku = String.format("%s-%04d", prefix, product.getId()).toLowerCase();

            boolean matchesSearch = searchText.isEmpty() || product.getName().toLowerCase().contains(searchText) || sku.contains(searchText) || product.getCategory().toLowerCase().contains(searchText);

            if (matchesCategory && matchesSearch) {
                productGrid.getChildren().add(createProductCard(product));
            }
        }
    }

    @FXML
    protected void onCategoryClick(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        currentCategory = clickedBtn.getText().replaceAll("[^a-zA-Z &]", "").trim();
        if (currentCategory.isEmpty() || currentCategory.equalsIgnoreCase("All")) {
            currentCategory = "All";
        }

        if (clickedBtn.getParent() instanceof Pane) {
            Pane parent = (Pane) clickedBtn.getParent();
            for (javafx.scene.Node node : parent.getChildren()) {
                if (node instanceof Button) {
                    node.setStyle("");
                }
            }
            clickedBtn.setStyle("-fx-background-color: -kmtm-primary-glow; -fx-text-fill: -kmtm-primary; -fx-border-color: -kmtm-primary; -fx-border-radius: 4; -fx-background-radius: 4;");
        }

        applyFilters();
    }

    @FXML
    protected void onClearCartClick() {
        if (cart.isEmpty()) {
            showThemedAlert(Alert.AlertType.INFORMATION, "Empty Cart", "The cart is already empty.");
            return;
        }
        cart.clear();
        discountField.clear();
        discountReasonBox.getEditor().clear();
        discountReasonBox.setValue(null);
        cashTenderedField.clear();
        gcashRefField.clear();
        selectedPaymentMethod = "Cash";
        updatePaymentUI();
        refreshCartUI();
    }

    private void showThemedAlert(Alert.AlertType type, String title, String message) {
        if (isAlertShowing) return;
        isAlertShowing = true;

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // Prevents the white-box glitch
        alert.setContentText(message);

        if (productGrid.getScene() != null && productGrid.getScene().getWindow() != null) {
            alert.initOwner(productGrid.getScene().getWindow());
        }

        applyThemeToDialog(alert.getDialogPane());

        alert.showAndWait();
        isAlertShowing = false;
    }

    // --- THE TARGETED THEME HUNTER ---
    private void applyThemeToDialog(DialogPane dialogPane) {
        if (productGrid == null || productGrid.getScene() == null) return;

        String activeThemeUrl = "";
        javafx.scene.Parent current = productGrid;

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
            for (String stylesheet : productGrid.getScene().getStylesheets()) {
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

    private String getCategoryIcon(String category) {
        if (category == null) return "📦";
        switch (category.toLowerCase()) {
            case "engine parts": return "⚙";
            case "brakes": return "🔴";
            case "tires & wheels": return "🛞";
            case "electrical": return "⚡";
            case "filters": return "🔵";
            case "suspension": return "🔩";
            case "exhaust": return "💨";
            case "lubricants & fluids": return "🛢";
            case "body parts": return "🏍";
            case "accessories": return "✨";
            default: return "📦";
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(5);
        card.setPrefWidth(160);
        card.setStyle("-fx-background-color: -kmtm-surface; -fx-border-color: -kmtm-border; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16 14; -fx-cursor: hand;");

        Label iconLabel = new Label(getCategoryIcon(product.getCategory()));
        iconLabel.setStyle("-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', sans-serif; -fx-font-size: 24px; -fx-padding: 0 0 5 0; -fx-text-fill: -kmtm-text;");

        String prefix = product.getCategory().length() >= 3 ? product.getCategory().substring(0, 3).toUpperCase() : "ITM";
        Label idLabel = new Label(String.format("%s-%04d", prefix, product.getId()));
        idLabel.setStyle("-fx-text-fill: -kmtm-text-dim; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px;");

        Label nameLabel = new Label(product.getName());
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-text-fill: -kmtm-text; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label priceLabel = new Label(String.format("₱%.2f", product.getPrice()));
        priceLabel.setStyle("-fx-text-fill: -kmtm-primary; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label stockLabel = new Label(product.getStock() + " in stock");
        stockLabel.setStyle("-fx-text-fill: -kmtm-text; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-font-weight: bold;");

        card.getChildren().addAll(iconLabel, idLabel, nameLabel, priceLabel, stockLabel);

        card.setOnMouseClicked(event -> {
            if (product.getStock() > 0) addToCart(product);
        });

        card.setOnMouseEntered(e -> {
            if (product.getStock() > 0) {
                card.setStyle("-fx-background-color: -kmtm-surface2; -fx-border-color: -kmtm-primary; -fx-border-radius: 8; -background-radius: 8; -fx-padding: 16 14; -fx-cursor: hand;");
                card.setTranslateY(-3);
            }
        });
        card.setOnMouseExited(e -> {
            if (product.getStock() > 0) {
                card.setStyle("-fx-background-color: -kmtm-surface; -fx-border-color: -kmtm-border; -fx-border-radius: 8; -background-radius: 8; -fx-padding: 16 14; -fx-cursor: hand;");
                card.setTranslateY(0);
            }
        });

        return card;
    }

    private void addToCart(Product product) {
        for (CartItem item : cart) {
            if (item.getProduct().getId() == product.getId()) {
                if (item.getQuantity() < product.getStock()) {
                    item.setQuantity(item.getQuantity() + 1);
                    refreshCartUI();
                } else {
                    showThemedAlert(Alert.AlertType.WARNING, "Stock Limit Reached", "Cannot add more " + product.getName() + ".\nOnly " + product.getStock() + " units available.");
                }
                return;
            }
        }
        cart.add(new CartItem(product, 1));
        refreshCartUI();
    }

    private void updateCartQuantity(Product product, int delta) {
        for (int i = 0; i < cart.size(); i++) {
            CartItem item = cart.get(i);
            if (item.getProduct().getId() == product.getId()) {
                int newQty = item.getQuantity() + delta;
                if (newQty <= 0) {
                    cart.remove(i);
                } else if (newQty <= product.getStock()) {
                    item.setQuantity(newQty);
                } else {
                    showThemedAlert(Alert.AlertType.WARNING, "Stock Limit Reached", "Cannot increase quantity.\nOnly " + product.getStock() + " units available.");
                }
                break;
            }
        }
        refreshCartUI();
    }

    private void applyManualQuantity(CartItem item, String newQtyStr) {
        try {
            int newQty = Integer.parseInt(newQtyStr.trim());
            if (newQty == item.getQuantity()) return;

            if (newQty <= 0) {
                updateCartQuantity(item.getProduct(), -item.getQuantity());
            } else if (newQty > item.getProduct().getStock()) {
                showThemedAlert(Alert.AlertType.WARNING, "Stock Limit Reached", "Only " + item.getProduct().getStock() + " units available. Quantity adjusted to maximum.");
                int delta = item.getProduct().getStock() - item.getQuantity();
                updateCartQuantity(item.getProduct(), delta);
            } else {
                int delta = newQty - item.getQuantity();
                updateCartQuantity(item.getProduct(), delta);
            }
        } catch (NumberFormatException e) {
            refreshCartUI();
        }
    }

    private void refreshCartUI() {
        cartContainer.getChildren().clear();
        cartContainer.setSpacing(8);
        cartContainer.setAlignment(Pos.TOP_CENTER);

        if (cart.isEmpty()) {
            cartContainer.setAlignment(Pos.CENTER);
            Label emptyLabel = new Label("Cart is empty.\nTap items to add.");
            emptyLabel.setTextAlignment(TextAlignment.CENTER);
            emptyLabel.setStyle("-fx-text-fill: -kmtm-text-muted; -fx-font-size: 13px;");
            cartContainer.getChildren().add(emptyLabel);
            originalTotalLabel.setText("₱0.00");
            totalLabel.setText("₱0.00");
            currentRawTotal = 0.0;
            currentFinalTotal = 0.0;
            currentDiscountAmount = 0.0;
            currentVatAmount = 0.0;
            currentVatableSales = 0.0;
            updateChangeDisplay();
            return;
        }

        currentRawTotal = 0;

        for (CartItem item : cart) {
            currentRawTotal += item.getSubtotal();

            HBox cartRow = new HBox(10);
            cartRow.setAlignment(Pos.CENTER_LEFT);
            cartRow.setStyle("-fx-background-color: -kmtm-surface2; -fx-background-radius: 4; -fx-padding: 10;");
            cartRow.setMinHeight(Region.USE_PREF_SIZE);

            VBox nameBox = new VBox();
            Label nameLbl = new Label(item.getProduct().getName());
            nameLbl.setStyle("-fx-text-fill: -kmtm-text; -fx-font-size: 13px; -fx-font-weight: bold;");
            nameLbl.setWrapText(true);
            nameLbl.setMinHeight(Region.USE_PREF_SIZE);

            Label unitPriceLbl = new Label(String.format("₱%.2f each", item.getProduct().getPrice()));
            unitPriceLbl.setStyle("-fx-text-fill: -kmtm-text-muted; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px;");
            nameBox.getChildren().addAll(nameLbl, unitPriceLbl);
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            HBox qtyBox = new HBox(5);
            qtyBox.setAlignment(Pos.CENTER);

            Button minusBtn = new Button("−");
            minusBtn.setMinWidth(28);
            minusBtn.setMinHeight(28);
            minusBtn.setStyle("-fx-background-color: -kmtm-surface; -fx-text-fill: -kmtm-text; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-cursor: hand;");
            minusBtn.setOnAction(e -> updateCartQuantity(item.getProduct(), -1));

            TextField qtyField = new TextField(String.valueOf(item.getQuantity()));
            qtyField.setStyle("-fx-background-color: -kmtm-surface; -fx-text-fill: -kmtm-text; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 14px; -fx-alignment: center; -fx-border-color: -kmtm-border; -fx-border-radius: 4;");
            qtyField.setPrefWidth(45);
            qtyField.setMinWidth(45);
            qtyField.setOnAction(e -> cartContainer.requestFocus());
            qtyField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) applyManualQuantity(item, qtyField.getText());
            });

            Button plusBtn = new Button("+");
            plusBtn.setMinWidth(28);
            plusBtn.setMinHeight(28);
            plusBtn.setStyle("-fx-background-color: -kmtm-surface; -fx-text-fill: -kmtm-text; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-cursor: hand;");
            plusBtn.setOnAction(e -> updateCartQuantity(item.getProduct(), 1));

            qtyBox.getChildren().addAll(minusBtn, qtyField, plusBtn);

            Label subLbl = new Label(String.format("₱%.2f", item.getSubtotal()));
            subLbl.setStyle("-fx-text-fill: -kmtm-primary; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 13px; -fx-alignment: center-right;");
            subLbl.setMinWidth(70);

            Button removeBtn = new Button("✕");
            removeBtn.setMinWidth(28);
            removeBtn.setMinHeight(28);
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -kmtm-text-muted; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> updateCartQuantity(item.getProduct(), -item.getQuantity()));

            cartRow.getChildren().addAll(nameBox, qtyBox, subLbl, removeBtn);
            cartContainer.getChildren().add(cartRow);
        }

        // --- CLEANED: DISCOUNT LOGIC (NO VAT EXEMPTION) ---
        double discountInputValue = 0;
        try {
            if (!discountField.getText().isEmpty()) discountInputValue = Double.parseDouble(discountField.getText());
        } catch (Exception ignored) {}

        if (discountTypeBox.getValue() != null && discountTypeBox.getValue().equals("%")) {
            currentDiscountAmount = currentRawTotal * (discountInputValue / 100.0);
        } else {
            currentDiscountAmount = discountInputValue;
        }

        if (currentDiscountAmount > currentRawTotal) {
            currentDiscountAmount = currentRawTotal;
        }

        currentFinalTotal = currentRawTotal - currentDiscountAmount;

        // --- FIXED: EVERYONE PAYS VAT FOR MOTOR PARTS ---
        double currentTaxRate = com.kamotomo.pos.utils.SystemSettings.getTaxRate();
        currentVatableSales = currentFinalTotal / (1 + currentTaxRate);
        currentVatAmount = currentFinalTotal - currentVatableSales;

        originalTotalLabel.setText(String.format("₱%.2f", currentRawTotal));
        if (currentDiscountAmount > 0) {
            originalTotalLabel.setStyle("-fx-text-fill: -kmtm-text-muted; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 13px; -fx-strikethrough: true;");
        } else {
            originalTotalLabel.setStyle("-fx-text-fill: -kmtm-text-muted; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 13px; -fx-strikethrough: false;");
        }

        totalLabel.setText(String.format("₱%.2f", currentFinalTotal));

        if (selectedPaymentMethod.equals("GCash")) {
            cashTenderedField.setText(String.valueOf(currentFinalTotal));
        }

        updateChangeDisplay();
    }

    private void updateChangeDisplay() {
        if (currentFinalTotal == 0 || selectedPaymentMethod.equals("GCash")) {
            changeLabel.setText("");
            return;
        }

        try {
            double tendered = Double.parseDouble(cashTenderedField.getText());
            double diff = tendered - currentFinalTotal;

            if (diff >= 0) {
                changeLabel.setText(String.format("Change: ₱%.2f", diff));
                changeLabel.setStyle("-fx-text-fill: #3adf8a;");
            } else {
                changeLabel.setText(String.format("Short: ₱%.2f", Math.abs(diff)));
                changeLabel.setStyle("-fx-text-fill: #f03d3d;");
            }
        } catch (NumberFormatException e) {
            changeLabel.setText("");
        }
    }

    @FXML
    protected void onSearchEnterPressed() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) return;

        for (Product product : allProducts) {
            String prefix = product.getCategory().length() >= 3 ? product.getCategory().substring(0, 3).toUpperCase() : "ITM";
            String sku = String.format("%s-%04d", prefix, product.getId()).toLowerCase();

            if (sku.equals(query) || product.getName().toLowerCase().equals(query)) {
                if (product.getStock() > 0) {
                    addToCart(product);
                    searchField.clear();
                    searchField.requestFocus();
                } else {
                    showThemedAlert(Alert.AlertType.WARNING, "Out of Stock", product.getName() + " is currently out of stock.");
                }
                return;
            }
        }
        showThemedAlert(Alert.AlertType.ERROR, "Not Found", "No product matches the scanned code: " + query);
        searchField.clear();
    }

    private void saveTransactionOffline(double total, double tendered, double discount, String discountReason, String paymentMethod, String refNumber) {
        String filename = "offline_sales.csv";
        String delimiter = "|";

        try (java.io.FileWriter fw = new java.io.FileWriter(filename, true);
             java.io.BufferedWriter bw = new java.io.BufferedWriter(fw);
             java.io.PrintWriter out = new java.io.PrintWriter(bw)) {

            StringBuilder sb = new StringBuilder();
            sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append(delimiter);
            sb.append(UserSession.getInstance().getUserId()).append(delimiter);
            sb.append(total).append(delimiter);
            sb.append(tendered).append(delimiter);
            sb.append(discount).append(delimiter);
            sb.append(discountReason != null && !discountReason.isEmpty() ? discountReason : "None").append(delimiter);
            sb.append(paymentMethod).append(delimiter);
            sb.append(refNumber != null && !refNumber.isEmpty() ? refNumber : "N/A").append(delimiter);

            StringBuilder itemsStr = new StringBuilder();
            for (CartItem item : cart) {
                itemsStr.append(item.getProduct().getId()).append(",")
                        .append(item.getQuantity()).append(",")
                        .append(item.getSubtotal()).append(";");
            }
            sb.append(itemsStr.toString());
            out.println(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            showThemedAlert(Alert.AlertType.ERROR, "Critical Error", "Failed to save offline transaction to local disk!");
        }
    }

    @FXML
    protected void onCompleteSaleClick() {
        if (cart.isEmpty()) {
            showThemedAlert(Alert.AlertType.WARNING, "Cart is Empty", "Add items to the cart before completing the sale.");
            return;
        }

        String refNumber = null;
        double tendered = 0;

        if (selectedPaymentMethod.equals("GCash")) {
            refNumber = gcashRefField.getText();
            if (refNumber == null || refNumber.length() < 13) {
                showThemedAlert(Alert.AlertType.WARNING, "Invalid Reference", "Please enter the full 13-digit GCash Reference Number.");
                return;
            }
            tendered = currentFinalTotal;
        } else {
            try { tendered = Double.parseDouble(cashTenderedField.getText()); } catch (Exception ignored) {}
            if (tendered < currentFinalTotal) {
                showThemedAlert(Alert.AlertType.WARNING, "Insufficient Cash", "You are short by ₱" + String.format("%.2f", (currentFinalTotal - tendered)));
                return;
            }
        }

        String discountReason = discountReasonBox.getEditor().getText();
        if (currentDiscountAmount > 0 && (discountReason == null || discountReason.trim().isEmpty())) {
            discountReason = "Manual Discount";
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Transaction");
        confirm.setHeaderText(null);

        String confirmMessage = String.format(
                "Items: %d\nTotal Due: ₱%.2f\nAmount Tendered: ₱%.2f\nChange: ₱%.2f\nPayment Method: %s\n\nDo you want to proceed with this transaction?",
                cart.size(), currentFinalTotal, tendered, (tendered - currentFinalTotal), selectedPaymentMethod
        );
        confirm.setContentText(confirmMessage);

        applyThemeToDialog(confirm.getDialogPane());

        java.util.Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        int userId = UserSession.getInstance().getUserId();
        int transactionId = 0;

        boolean isDatabaseDown = false;
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null || !conn.isValid(2)) {
                isDatabaseDown = true;
            }
        } catch (Exception dbError) {
            isDatabaseDown = true;
        }

        if (isDatabaseDown) {
            showThemedAlert(Alert.AlertType.WARNING, "Offline Mode Active",
                    "Database connection lost. Transaction will be saved locally to the offline queue.");

            saveTransactionOffline(currentFinalTotal, tendered, currentDiscountAmount, discountReason, selectedPaymentMethod, refNumber);

            int offlineId = (int) (System.currentTimeMillis() % 100000);
            showReceipt(offlineId, tendered, tendered - currentFinalTotal, discountReason, selectedPaymentMethod, refNumber);

            for(CartItem cartItem : cart) {
                for(Product p : allProducts) {
                    if(p.getId() == cartItem.getProduct().getId()) {
                        p.setStock(p.getStock() - cartItem.getQuantity());
                    }
                }
            }

            onClearCartClick();
            applyFilters();
            return;
        }

        try {
            conn.setAutoCommit(false);
            try {
                String txSql = "INSERT INTO TRANSACTION (userID, totalAmount, paymentMethod, discountAmount, discountReason, amountTendered) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement txStmt = conn.prepareStatement(txSql, java.sql.Statement.RETURN_GENERATED_KEYS);
                txStmt.setInt(1, userId);
                txStmt.setDouble(2, currentFinalTotal);
                txStmt.setString(3, selectedPaymentMethod);
                txStmt.setDouble(4, currentDiscountAmount);
                txStmt.setString(5, discountReason);
                txStmt.setDouble(6, tendered);
                txStmt.executeUpdate();

                ResultSet rs = txStmt.getGeneratedKeys();
                if (rs.next()) transactionId = rs.getInt(1);

                String detailsSql = "INSERT INTO TRANSACTION_DETAILS (transactionID, productID, quantity, subtotal) VALUES (?, ?, ?, ?)";
                String stockSql = "UPDATE PRODUCT SET stockQuantity = stockQuantity - ? WHERE productID = ?";
                PreparedStatement detailsStmt = conn.prepareStatement(detailsSql);
                PreparedStatement stockStmt = conn.prepareStatement(stockSql);

                for (CartItem item : cart) {
                    detailsStmt.setInt(1, transactionId);
                    detailsStmt.setInt(2, item.getProduct().getId());
                    detailsStmt.setInt(3, item.getQuantity());
                    detailsStmt.setDouble(4, item.getSubtotal());
                    detailsStmt.addBatch();

                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, item.getProduct().getId());
                    stockStmt.addBatch();
                }

                detailsStmt.executeBatch();
                stockStmt.executeBatch();
                conn.commit();

                com.kamotomo.pos.utils.SystemLogger.logAction("Transaction", "Sale ID: " + transactionId + " | Total: ₱" + String.format("%.2f", currentFinalTotal));

                showReceipt(transactionId, tendered, tendered - currentFinalTotal, discountReason, selectedPaymentMethod, refNumber);

                onClearCartClick();
                loadProductsFromDatabase();

            } catch (Exception ex) {
                conn.rollback();
                showThemedAlert(Alert.AlertType.ERROR, "Database Error", "Sale failed due to a database error. Check logs.");
                ex.printStackTrace();
            } finally {
                if(conn != null) conn.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showReceipt(int transactionId, double tendered, double change, String discountReason, String paymentMethod, String refNumber) {
        VBox receiptBox = new VBox(8);
        receiptBox.setAlignment(Pos.TOP_CENTER);

        receiptBox.setPrefWidth(250);
        receiptBox.setMaxWidth(250);
        receiptBox.setPadding(new Insets(10));
        receiptBox.setStyle("-fx-background-color: white; -fx-font-family: 'Monospaced'; -fx-font-size: 11px; -fx-text-fill: black;");

        Label title = new Label("KaMotoMo\nMotor Parts");
        title.setTextAlignment(TextAlignment.CENTER);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label details = new Label("TXN: " + transactionId + "\nDate: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) + "\nCashier: " + UserSession.getInstance().getName() + "\nPay: " + paymentMethod);
        if (paymentMethod.equals("GCash")) {
            details.setText(details.getText() + "\nRef: " + refNumber);
        }
        details.setAlignment(Pos.CENTER_LEFT);
        details.setMaxWidth(Double.MAX_VALUE);

        Label divider1 = new Label("------------------------");
        Label divider2 = new Label("------------------------");

        VBox itemsBox = new VBox(2);
        for (CartItem item : cart) {
            Label nameRow = new Label(item.getProduct().getName());
            nameRow.setWrapText(true);
            nameRow.setMaxWidth(230);

            Label priceRow = new Label(String.format("  %dx @ ₱%.2f = ₱%.2f", item.getQuantity(), item.getProduct().getPrice(), item.getSubtotal()));

            itemsBox.getChildren().addAll(nameRow, priceRow);
        }

        receiptBox.getChildren().addAll(title, divider1, details, divider2, itemsBox, new Label(" "));

        if (currentDiscountAmount > 0) {
            receiptBox.getChildren().add(new Label(String.format("SUBTOTAL:   ₱%.2f", currentRawTotal)));
            receiptBox.getChildren().add(new Label(String.format("DISCOUNT:  -₱%.2f", currentDiscountAmount)));
            receiptBox.getChildren().add(new Label(String.format("REASON:     %s", discountReason)));
            receiptBox.getChildren().add(new Label("------------------------"));
        }

        Label totalLbl = new Label(String.format("TOTAL:      ₱%.2f", currentFinalTotal));
        totalLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label tenderLbl = new Label(String.format("Tendered:   ₱%.2f", tendered));
        Label changeLbl = new Label(String.format("Change:     ₱%.2f", change));

        receiptBox.getChildren().addAll(totalLbl, tenderLbl, changeLbl, new Label("------------------------"));

        // --- FIXED: Standard VAT Breakdown for Receipt ---
        int displayTaxPercent = (int) Math.round(com.kamotomo.pos.utils.SystemSettings.getTaxRate() * 100);

        Label vatableLbl = new Label(String.format("VATable Sales: ₱%.2f", currentVatableSales));
        Label vatLbl = new Label(String.format("VAT (%d%%):     ₱%.2f", displayTaxPercent, currentVatAmount));
        receiptBox.getChildren().addAll(vatableLbl, vatLbl);

        receiptBox.getChildren().add(new Label("\nThank you!\nIngat sa kalsada! 🏍"));

        Dialog<Void> receiptDialog = new Dialog<>();
        receiptDialog.setTitle("Transaction Complete");

        DialogPane dialogPane = receiptDialog.getDialogPane();
        if (productGrid.getScene() != null && productGrid.getScene().getRoot() != null) {
            dialogPane.getStylesheets().addAll(productGrid.getScene().getRoot().getStylesheets());
        }
        dialogPane.getStyleClass().addAll("custom-dialog", "root");

        HBox dialogContainer = new HBox(receiptBox);
        dialogContainer.setAlignment(Pos.CENTER);
        dialogContainer.setPadding(new Insets(20));
        dialogPane.setContent(dialogContainer);

        ButtonType printType = new ButtonType("🖨 Print", ButtonBar.ButtonData.OK_DONE);
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

    @FXML
    protected void onTransactionHistoryClick() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("transaction-ledger-view.fxml"));
            Parent root = fxmlLoader.load();

            Stage ledgerStage = new Stage();
            ledgerStage.setTitle("Transaction Ledger & Void Protocol");
            ledgerStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            ledgerStage.setResizable(false);

            javafx.scene.Scene ledgerScene = new javafx.scene.Scene(root, 900, 600);

            String activeTheme = com.kamotomo.pos.utils.UserSession.getInstance().getThemePreference();
            if (activeTheme == null || activeTheme.isEmpty()) activeTheme = "light";
            ledgerScene.getStylesheets().add(getClass().getResource("/" + activeTheme + "-theme.css").toExternalForm());

            ledgerStage.setScene(ledgerScene);
            ledgerStage.showAndWait();

            loadProductsFromDatabase();

        } catch (Exception e) {
            e.printStackTrace();
            showThemedAlert(Alert.AlertType.ERROR, "System Error", "Failed to open the Transaction Ledger.");
        }
    }
}