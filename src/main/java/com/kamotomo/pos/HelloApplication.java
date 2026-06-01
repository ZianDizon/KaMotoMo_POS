package com.kamotomo.pos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.text.Font; // Make sure to import this!
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // --- LOAD CUSTOM FONTS INTO MEMORY ---
        Font.loadFont(getClass().getResourceAsStream("/fonts/BebasNeue-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/IBMPlexMono-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/IBMPlexMono-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/IBMPlexSans-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/IBMPlexSans-Bold.ttf"), 14);

        // Continue loading the app normally...
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 500);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/motorcycle.png")));
        stage.setTitle("KaMotoMo - Login");
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/light-theme.css").toExternalForm());
        // --- NEW: APPLICATION EXIT CONFIRMATION ---
        stage.setOnCloseRequest(event -> {
            // 1. Consume the event to stop the window from instantly closing
            event.consume();

            // 2. Create the themed confirmation dialog
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Exit Application");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to exit the POS system? Any unsaved transaction data will be lost.");

            // 3. Inject our active custom theme into the dialog
            DialogPane dialogPane = alert.getDialogPane();
            if (stage.getScene() != null) {
                dialogPane.getStylesheets().setAll(stage.getScene().getStylesheets());
                if (stage.getScene().getRoot() != null) {
                    dialogPane.getStylesheets().addAll(stage.getScene().getRoot().getStylesheets());
                }
            }
            dialogPane.getStyleClass().addAll("custom-dialog", "root");

            // 4. Wait for the user's decision
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Forcefully terminate the JVM if they click OK
                    javafx.application.Platform.exit();
                    System.exit(0);
                }
            });
        });

        // stage.show(); // (Your existing code)
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}