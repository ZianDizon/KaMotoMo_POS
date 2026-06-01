package com.kamotomo.pos;

import com.kamotomo.pos.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;

public class HelpController {

    // These specific panels will be injected from the FXML file
    @FXML private TitledPane adminPaneInventory;
    @FXML private TitledPane adminPaneUsers;
    @FXML private TitledPane adminPaneLogs;
    @FXML private TitledPane adminPaneMaintenance;
    @FXML private TitledPane adminPaneVoids;
    @FXML private TitledPane adminPaneReports;
    @FXML private TitledPane adminPaneRecovery;

    @FXML
    public void initialize() {
        // --- SECURE THE HELP MODULE ---
        // If the user is an Employee, completely hide and collapse the Admin guides
        if ("Employee".equalsIgnoreCase(UserSession.getInstance().getRole())) {

            if (adminPaneInventory != null) {
                adminPaneInventory.setVisible(false);
                adminPaneInventory.setManaged(false);
            }
            if (adminPaneUsers != null) {
                adminPaneUsers.setVisible(false);
                adminPaneUsers.setManaged(false);
            }
            if (adminPaneLogs != null) {
                adminPaneLogs.setVisible(false);
                adminPaneLogs.setManaged(false);
            }
            if (adminPaneMaintenance != null) {
                adminPaneMaintenance.setVisible(false);
                adminPaneMaintenance.setManaged(false);
            }

            if (adminPaneVoids != null) {
                adminPaneVoids.setVisible(false);
                adminPaneVoids.setManaged(false);
            }

            if (adminPaneReports != null) {
                adminPaneReports.setVisible(false);
                adminPaneReports.setManaged(false);
            }

            if (adminPaneRecovery != null) {
                adminPaneRecovery.setVisible(false);
                adminPaneRecovery.setManaged(false);
            }
        }
    }
}