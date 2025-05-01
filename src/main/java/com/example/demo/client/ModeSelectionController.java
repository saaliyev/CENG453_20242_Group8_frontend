package com.example.demo.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.net.URL;

public class ModeSelectionController {

    @FXML
    public void handleSinglePlayer(ActionEvent event) {
        // Switch to game screen
        SceneManager.switchTo("game.fxml");
        SceneManager.getStage().setFullScreen(true);

    }

    @FXML
    public void handleMultiplayer(ActionEvent event) {
        // Placeholder: Navigate to multiplayer lobby
        SceneManager.switchTo("multiplayer_lobby.fxml");
    }
}
