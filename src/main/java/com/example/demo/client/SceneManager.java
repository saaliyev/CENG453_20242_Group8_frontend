package com.example.demo.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    private static Stage stage;

    public static void setStage(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void switchTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("UNO Game");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
