package com.example.demo.client;

import javafx.fxml.FXML;

public class LobbyController {

    @FXML
    private void onPlayClicked() {
        SceneManager.switchTo("mode_selection.fxml");
    }

}
