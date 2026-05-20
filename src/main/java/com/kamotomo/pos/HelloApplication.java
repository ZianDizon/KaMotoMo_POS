package com.kamotomo.pos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}