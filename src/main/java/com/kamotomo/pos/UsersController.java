package com.kamotomo.pos;

import com.kamotomo.pos.database.DatabaseConnection;
import com.kamotomo.pos.utils.UserSession;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class UsersController {

    @FXML private TableView<UserAccount> usersTable;
    @FXML private TableColumn<UserAccount, Integer> colId;
    @FXML private TableColumn<UserAccount, String> colUsername;
    @FXML private TableColumn<UserAccount, String> colRole;
    @FXML private TableColumn<UserAccount, String> colStatus;
    @FXML private TableColumn<UserAccount, UserAccount> colAction;

    private TableColumn<UserAccount, String> colName;
    private TableColumn<UserAccount, String> colContact;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ToggleButton toggleArchiveBtn;

    private ObservableList<UserAccount> masterData = FXCollections.observableArrayList();
    private FilteredList<UserAccount> filteredData;

    @FXML
    public void initialize() {
        roleFilter.getItems().addAll("All Roles", "Admin", "Employee");
        roleFilter.getSelectionModel().selectFirst();

        // --- FORCES THE INITIAL STYLE ---
        if (toggleArchiveBtn != null) {
            toggleArchiveBtn.setStyle("-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;");
        }

        setupTable();
        loadUserData();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        roleFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void showThemedAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        applyThemeToDialog(dialogPane);

        alert.showAndWait();
    }

    private void setupTable() {
        colName = new TableColumn<>("REAL NAME");
        colName.setPrefWidth(200);
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setCellFactory(tc -> new TableCell<UserAccount, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); } else {
                    setText(item);
                    setStyle("-fx-font-family: 'IBM Plex Sans'; -fx-text-fill: -kmtm-text;");
                }
            }
        });

        colContact = new TableColumn<>("CONTACT INFO");
        colContact.setPrefWidth(180);
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactInfo"));
        colContact.setCellFactory(tc -> new TableCell<UserAccount, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); } else {
                    setText(item);
                    setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-text-fill: -kmtm-text-dim;");
                }
            }
        });

        usersTable.getColumns().add(1, colName);
        usersTable.getColumns().add(2, colContact);

        colId.setCellValueFactory(new PropertyValueFactory<>("userID"));
        colId.setCellFactory(tc -> new TableCell<UserAccount, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    setText(String.format("USR-%03d", item));
                    setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-text-fill: -kmtm-text-dim;");
                }
            }
        });

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setCellFactory(tc -> new TableCell<UserAccount, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    setText(item);
                    setStyle("-fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold; -fx-text-fill: -kmtm-text;");
                }
            }
        });

        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(tc -> new TableCell<UserAccount, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    Label lbl = new Label(item.toUpperCase());
                    if (item.equalsIgnoreCase("Admin")) {
                        lbl.setStyle("-fx-background-color: rgba(245, 98, 15, 0.15); -fx-text-fill: #f5620f; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold;");
                    } else {
                        lbl.setStyle("-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text-dim; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold;");
                    }
                    setGraphic(lbl);
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(tc -> new TableCell<UserAccount, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    Label lbl = new Label(item.toUpperCase());
                    if (item.equalsIgnoreCase("Active")) {
                        lbl.setStyle("-fx-background-color: rgba(58, 223, 138, 0.15); -fx-text-fill: #3adf8a; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold;");
                    } else {
                        lbl.setStyle("-fx-background-color: rgba(240, 61, 61, 0.15); -fx-text-fill: #f03d3d; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold;");
                    }
                    setGraphic(lbl);
                }
            }
        });

        colAction.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colAction.setCellFactory(tc -> new TableCell<UserAccount, UserAccount>() {
            @Override
            protected void updateItem(UserAccount user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) { setGraphic(null); }
                else {
                    HBox actionBox = new HBox(8);
                    actionBox.setAlignment(Pos.CENTER_LEFT);

                    String btnStyle = "-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-cursor: hand;";

                    Button btnEdit = new Button("Edit");
                    btnEdit.setStyle(btnStyle);
                    btnEdit.setOnAction(e -> showUserDialog(user));

                    Button btnReset = new Button("Reset Pass");
                    btnReset.setStyle(btnStyle);
                    btnReset.setOnAction(e -> resetPassword(user));

                    Button btnToggle = new Button(user.getStatus().equals("Active") ? "Archive" : "Restore");
                    btnToggle.setStyle(btnStyle + (user.getStatus().equals("Active") ? "-fx-text-fill: #f03d3d;" : "-fx-text-fill: #3adf8a;"));
                    btnToggle.setOnAction(e -> toggleUserStatus(user));

                    if (user.getUserID() == UserSession.getInstance().getUserId()) {
                        btnToggle.setDisable(true);
                        btnToggle.setTooltip(new Tooltip("You cannot archive your own active session."));
                    }

                    actionBox.getChildren().addAll(btnEdit, btnReset, btnToggle);
                    setGraphic(actionBox);
                }
            }
        });
    }

    private void loadUserData() {
        masterData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM `user`";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                masterData.add(new UserAccount(
                        rs.getInt("userID"),
                        rs.getString("username"),
                        rs.getString("name"),
                        rs.getString("contactInfo"),
                        rs.getString("role"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<UserAccount> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sortedData);
        applyFilters();
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();
        String role = roleFilter.getValue();
        boolean showArchived = toggleArchiveBtn.isSelected();

        filteredData.setPredicate(user -> {
            if (!showArchived && user.getStatus().equals("Archived")) return false;
            if (showArchived && user.getStatus().equals("Active")) return false;

            if (!role.equals("All Roles") && !user.getRole().equalsIgnoreCase(role)) return false;

            if (search.isEmpty()) return true;
            return user.getUsername().toLowerCase().contains(search) ||
                    user.getName().toLowerCase().contains(search) ||
                    String.format("usr-%03d", user.getUserID()).contains(search);
        });
    }

    @FXML
    protected void onToggleArchive() {
        if (toggleArchiveBtn.isSelected()) {
            toggleArchiveBtn.setText("Show Active");
            toggleArchiveBtn.setStyle("-fx-background-color: -kmtm-primary-glow; -fx-text-fill: -kmtm-primary; -fx-border-color: -kmtm-primary; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;");
        } else {
            toggleArchiveBtn.setText("Show Archived");
            toggleArchiveBtn.setStyle("-fx-background-color: -kmtm-surface2; -fx-text-fill: -kmtm-text; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-family: 'IBM Plex Sans'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;");
        }
        applyFilters();
    }

    @FXML
    protected void onAddUserClick() {
        showUserDialog(null);
    }

    private void showUserDialog(UserAccount existingUser) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(existingUser == null ? "Create New User" : "Edit User Details");

        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        // --- UPGRADED INPUT STYLING ---
        String inputStyle = "-fx-padding: 10; -fx-background-color: -kmtm-surface2; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: -kmtm-text; -fx-font-family: 'IBM Plex Sans';";
        String labelStyle = "-fx-text-fill: -kmtm-text-dim; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-font-weight: bold;";

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Juan Dela Cruz");
        nameField.setStyle(inputStyle);

        TextField contactField = new TextField();
        contactField.setPromptText("Phone or Email");
        contactField.setStyle(inputStyle);

        TextField usernameField = new TextField();
        usernameField.setPromptText("System Username");
        usernameField.setStyle(inputStyle);

        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("Employee", "Admin"));
        roleBox.setMaxWidth(Double.MAX_VALUE);
        roleBox.setStyle("-fx-font-family: 'IBM Plex Sans'; -fx-background-color: -kmtm-surface2; -fx-border-color: -kmtm-border; -fx-border-radius: 4;");

        PasswordField passField = new PasswordField();
        passField.setPromptText(existingUser == null ? "Initial Password" : "Leave blank to keep current");
        passField.setStyle(inputStyle);

        if (existingUser != null) {
            nameField.setText(existingUser.getName());
            contactField.setText(existingUser.getContactInfo());
            usernameField.setText(existingUser.getUsername());
            roleBox.setValue(existingUser.getRole());
        } else {
            roleBox.getSelectionModel().selectFirst();
        }

        // --- UPGRADED GRID LAYOUT ---
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(25, 30, 15, 30));

        // Force a perfect 50/50 split
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        Label nameLbl = new Label("REAL NAME"); nameLbl.setStyle(labelStyle);
        Label contactLbl = new Label("CONTACT INFO"); contactLbl.setStyle(labelStyle);
        Label userLbl = new Label("USERNAME"); userLbl.setStyle(labelStyle);
        Label roleLbl = new Label("SYSTEM ROLE"); roleLbl.setStyle(labelStyle);
        Label passLbl = new Label(existingUser == null ? "PASSWORD" : "NEW PASSWORD (OPTIONAL)"); passLbl.setStyle(labelStyle);

        // Stack labels on top of fields
        grid.add(new VBox(5, nameLbl, nameField), 0, 0, 2, 1);
        grid.add(new VBox(5, userLbl, usernameField), 0, 1);
        grid.add(new VBox(5, roleLbl, roleBox), 1, 1);
        grid.add(new VBox(5, contactLbl, contactField), 0, 2);

        if (existingUser == null) {
            grid.add(new VBox(5, passLbl, passField), 1, 2);
        } else {
            grid.add(new VBox(5, passLbl, passField), 1, 2);
        }

        dialogPane.setContent(grid);

        ButtonType btnSave = new ButtonType(existingUser == null ? "Save User" : "Update User", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL, btnSave);

        dialog.setResultConverter(b -> b == btnSave ? true : null);
        dialog.showAndWait().ifPresent(confirmed -> {
            if (usernameField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()) {
                showThemedAlert(Alert.AlertType.WARNING, "Invalid Input", "Name and Username cannot be empty.");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                if (existingUser == null) {
                    String sql = "INSERT INTO `user` (username, password, role, name, contactInfo, status) VALUES (?, ?, ?, ?, ?, 'Active')";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, usernameField.getText().trim());
                    stmt.setString(2, passField.getText().trim());
                    stmt.setString(3, roleBox.getValue());
                    stmt.setString(4, nameField.getText().trim());
                    stmt.setString(5, contactField.getText().trim());
                    stmt.executeUpdate();

                    com.kamotomo.pos.utils.SystemLogger.logAction("User Management", "Created new user: " + usernameField.getText().trim());
                    showThemedAlert(Alert.AlertType.INFORMATION, "Success", "New user created successfully.");
                } else {
                    String sql = "UPDATE `user` SET username = ?, role = ?, name = ?, contactInfo = ? WHERE userID = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, usernameField.getText().trim());
                    stmt.setString(2, roleBox.getValue());
                    stmt.setString(3, nameField.getText().trim());
                    stmt.setString(4, contactField.getText().trim());
                    stmt.setInt(5, existingUser.getUserID());
                    stmt.executeUpdate();

                    com.kamotomo.pos.utils.SystemLogger.logAction("User Management", "Updated user: " + usernameField.getText().trim());
                }
                loadUserData();
            } catch (Exception e) {
                e.printStackTrace();
                showThemedAlert(Alert.AlertType.ERROR, "Database Error", "Could not save user. Ensure username is unique.");
            }
        });
    }

    private void resetPassword(UserAccount user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Password");
        confirm.setHeaderText("Reset password for " + user.getUsername() + "?");
        confirm.setContentText("This will change their password to '1234'.");

        DialogPane dialogPane = confirm.getDialogPane();
        applyThemeToDialog(dialogPane);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE `user` SET password = ? WHERE userID = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, "1234");
                stmt.setInt(2, user.getUserID());
                stmt.executeUpdate();

                // --- LOGGING ADDED HERE ---
                com.kamotomo.pos.utils.SystemLogger.logAction("Security", "Reset password for user ID " + user.getUserID());

                showThemedAlert(Alert.AlertType.INFORMATION, "Password Reset", "Password successfully reset to '1234'.");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void toggleUserStatus(UserAccount user) {
        String newStatus = user.getStatus().equals("Active") ? "Archived" : "Active";
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE `user` SET status = ? WHERE userID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newStatus);
            stmt.setInt(2, user.getUserID());
            stmt.executeUpdate();
            loadUserData();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static class UserAccount {
        private final int userID;
        private final String username;
        private final String name;
        private final String contactInfo;
        private final String role;
        private final String status;

        public UserAccount(int userID, String username, String name, String contactInfo, String role, String status) {
            this.userID = userID;
            this.username = username;
            this.name = name;
            this.contactInfo = contactInfo;
            this.role = role;
            this.status = status;
        }

        public int getUserID() { return userID; }
        public String getUsername() { return username; }
        public String getName() { return name; }
        public String getContactInfo() { return contactInfo; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
    }

    // --- THE TARGETED THEME HUNTER ---
    private void applyThemeToDialog(DialogPane dialogPane) {
        if (usersTable == null || usersTable.getScene() == null) return;

        // 1. Hunt down the exact active theme URL by walking up the application tree
        String activeThemeUrl = "";
        javafx.scene.Parent current = usersTable;

        while (current != null) {
            for (String stylesheet : current.getStylesheets()) {
                if (stylesheet.contains("dark-theme.css") || stylesheet.contains("light-theme.css")) {
                    activeThemeUrl = stylesheet;
                    break;
                }
            }
            if (!activeThemeUrl.isEmpty()) break;
            current = current.getParent(); // Move up to the next wrapper
        }

        // 2. If it wasn't on the nodes, check the Scene itself
        if (activeThemeUrl.isEmpty()) {
            for (String stylesheet : usersTable.getScene().getStylesheets()) {
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