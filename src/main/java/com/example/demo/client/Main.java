package com.example.demo.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.setStage(primaryStage);
        SceneManager.switchTo("register.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
