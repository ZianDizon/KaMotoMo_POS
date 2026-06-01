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

        usernameField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("^[a-zA-Z0-9]{0,16}$")) {
                return change;
            }
            return null;
        }));

        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("Employee", "Admin"));
        roleBox.setMaxWidth(Double.MAX_VALUE);
        roleBox.setStyle("-fx-font-family: 'IBM Plex Sans'; -fx-background-color: -kmtm-surface2; -fx-border-color: -kmtm-border; -fx-border-radius: 4;");

        PasswordField passField = new PasswordField();
        passField.setPromptText(existingUser == null ? "Strict: Min 8, Upper, Lower, Num, Special" : "Leave blank to keep current");
        passField.setStyle(inputStyle);

        if (existingUser != null) {
            nameField.setText(existingUser.getName());
            contactField.setText(existingUser.getContactInfo());
            usernameField.setText(existingUser.getUsername());
            roleBox.setValue(existingUser.getRole());
        } else {
            roleBox.getSelectionModel().selectFirst();
        }

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(25, 30, 15, 30));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        Label nameLbl = new Label("REAL NAME"); nameLbl.setStyle(labelStyle);
        Label contactLbl = new Label("CONTACT INFO"); contactLbl.setStyle(labelStyle);
        Label userLbl = new Label("USERNAME"); userLbl.setStyle(labelStyle);
        Label roleLbl = new Label("SYSTEM ROLE"); roleLbl.setStyle(labelStyle);
        Label passLbl = new Label(existingUser == null ? "PASSWORD (Strict Format)" : "NEW PASSWORD (OPTIONAL)"); passLbl.setStyle(labelStyle);

        grid.add(new VBox(5, nameLbl, nameField), 0, 0, 2, 1);
        grid.add(new VBox(5, userLbl, usernameField), 0, 1);
        grid.add(new VBox(5, roleLbl, roleBox), 1, 1);
        grid.add(new VBox(5, contactLbl, contactField), 0, 2);
        grid.add(new VBox(5, passLbl, passField), 1, 2);

        dialogPane.setContent(grid);

        ButtonType btnSave = new ButtonType(existingUser == null ? "Save User" : "Update User", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL, btnSave);

        final Button btOk = (Button) dialogPane.lookupButton(btnSave);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (usernameField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()) {
                showThemedAlert(Alert.AlertType.WARNING, "Invalid Input", "Name and Username cannot be empty.");
                event.consume();
                return;
            }

// --- SECURITY FIX: STRICT REGEX COMPLEXITY CHECK ---
            if (existingUser == null && !com.kamotomo.pos.utils.SecurityUtil.isPasswordStrong(passField.getText().trim())) {
                showThemedAlert(Alert.AlertType.WARNING, "Weak Password",
                        "Initial password does not meet security requirements:\n" +
                                "• Minimum of 8 characters\n" +
                                "• At least 1 uppercase letter\n" +
                                "• At least 1 lowercase letter\n" +
                                "• At least 1 number\n" +
                                "• At least 1 special character (@$!%*?&)");
                event.consume();
                return;
            }

            if (existingUser != null && !passField.getText().trim().isEmpty() && !com.kamotomo.pos.utils.SecurityUtil.isPasswordStrong(passField.getText().trim())) {
                showThemedAlert(Alert.AlertType.WARNING, "Weak Password",
                        "New password does not meet security requirements:\n" +
                                "• Minimum of 8 characters\n" +
                                "• At least 1 uppercase letter\n" +
                                "• At least 1 lowercase letter\n" +
                                "• At least 1 number\n" +
                                "• At least 1 special character (@$!%*?&)");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(b -> b == btnSave ? true : null);
        dialog.showAndWait().ifPresent(confirmed -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (existingUser == null) {
                    String sql = "INSERT INTO `user` (username, password, role, name, contactInfo, status) VALUES (?, ?, ?, ?, ?, 'Active')";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, usernameField.getText().trim());
                    // --- SECURITY FIX: HASH NEW PASSWORD ---
                    stmt.setString(2, com.kamotomo.pos.utils.SecurityUtil.hashPassword(passField.getText().trim()));
                    stmt.setString(3, roleBox.getValue());
                    stmt.setString(4, nameField.getText().trim());
                    stmt.setString(5, contactField.getText().trim());
                    stmt.executeUpdate();

                    com.kamotomo.pos.utils.SystemLogger.logAction("User Management", "Created new user: " + usernameField.getText().trim());
                    showThemedAlert(Alert.AlertType.INFORMATION, "Success", "New user created successfully.");
                } else {
                    if (passField.getText().trim().isEmpty()) {
                        String sql = "UPDATE `user` SET username = ?, role = ?, name = ?, contactInfo = ? WHERE userID = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, usernameField.getText().trim());
                        stmt.setString(2, roleBox.getValue());
                        stmt.setString(3, nameField.getText().trim());
                        stmt.setString(4, contactField.getText().trim());
                        stmt.setInt(5, existingUser.getUserID());
                        stmt.executeUpdate();
                    } else {
                        String sql = "UPDATE `user` SET username = ?, password = ?, role = ?, name = ?, contactInfo = ? WHERE userID = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, usernameField.getText().trim());
                        // --- SECURITY FIX: HASH UPDATED PASSWORD ---
                        stmt.setString(2, com.kamotomo.pos.utils.SecurityUtil.hashPassword(passField.getText().trim()));
                        stmt.setString(3, roleBox.getValue());
                        stmt.setString(4, nameField.getText().trim());
                        stmt.setString(5, contactField.getText().trim());
                        stmt.setInt(6, existingUser.getUserID());
                        stmt.executeUpdate();
                    }
                    com.kamotomo.pos.utils.SystemLogger.logAction("User Management", "Updated user: " + usernameField.getText().trim());
                    showThemedAlert(Alert.AlertType.INFORMATION, "Success", "User details updated successfully.");
                }
                loadUserData();
            } catch (Exception e) {
                e.printStackTrace();
                showThemedAlert(Alert.AlertType.ERROR, "Database Error", "Could not save user. Ensure username is unique.");
            }
        });
    }

    private void resetPassword(UserAccount user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Secure Password Reset");
        dialog.setHeaderText("Set temporary password for " + user.getUsername());

        DialogPane dialogPane = dialog.getDialogPane();
        applyThemeToDialog(dialogPane);

        PasswordField tempPasswordField = new PasswordField();
        tempPasswordField.setPromptText("Strict: Min 8, Upper, Lower, Num, Special");
        tempPasswordField.setStyle("-fx-padding: 10; -fx-background-color: -kmtm-surface2; -fx-border-color: -kmtm-border; -fx-border-radius: 4; -fx-text-fill: -kmtm-text;");

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("New Temporary Password:"), tempPasswordField);
        content.setPadding(new Insets(20));

        dialogPane.setContent(content);

        ButtonType btnReset = new ButtonType("Apply Reset", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL, btnReset);

        final Button btOk = (Button) dialogPane.lookupButton(btnReset);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // --- SECURITY FIX: STRICT REGEX COMPLEXITY CHECK ---
            if (!com.kamotomo.pos.utils.SecurityUtil.isPasswordStrong(tempPasswordField.getText())) {
                showThemedAlert(Alert.AlertType.WARNING, "Security Requirement",
                        "Validation Failed. Password must contain:\n\n" +
                                "• Minimum of 8 characters\n" +
                                "• At least 1 uppercase letter\n" +
                                "• At least 1 lowercase letter\n" +
                                "• At least 1 number\n" +
                                "• At least 1 special character (@$!%*?&)");
                event.consume();
            }
        });

        dialog.setResultConverter(b -> b == btnReset ? tempPasswordField.getText() : null);

        dialog.showAndWait().ifPresent(newPass -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE `user` SET password = ? WHERE userID = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                // --- SECURITY FIX: HASH TEMPORARY PASSWORD ---
                stmt.setString(1, com.kamotomo.pos.utils.SecurityUtil.hashPassword(newPass));
                stmt.setInt(2, user.getUserID());
                stmt.executeUpdate();

                com.kamotomo.pos.utils.SystemLogger.logAction("Security", "Reset password for user ID " + user.getUserID());
                showThemedAlert(Alert.AlertType.INFORMATION, "Password Reset", "Password for " + user.getUsername() + " successfully updated. Please provide them with the new temporary credentials.");
            } catch (Exception e) {
                e.printStackTrace();
                showThemedAlert(Alert.AlertType.ERROR, "Database Error", "Failed to reset password.");
            }
        });
    }

    private void toggleUserStatus(UserAccount user) {
        String newStatus = user.getStatus().equals("Active") ? "Archived" : "Active";
        String action = newStatus.equals("Archived") ? "archive (deactivate)" : "restore";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Account Status Change");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to " + action + " the account for " + user.getUsername() + "?");

        applyThemeToDialog(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE `user` SET status = ? WHERE userID = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, newStatus);
                    stmt.setInt(2, user.getUserID());
                    stmt.executeUpdate();

                    com.kamotomo.pos.utils.SystemLogger.logAction("User Management", "Changed status of user " + user.getUsername() + " to " + newStatus);
                    loadUserData();
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
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

    private void applyThemeToDialog(DialogPane dialogPane) {
        if (usersTable == null || usersTable.getScene() == null) return;

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
            current = current.getParent();
        }

        if (activeThemeUrl.isEmpty()) {
            for (String stylesheet : usersTable.getScene().getStylesheets()) {
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