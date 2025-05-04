package com.example.demo.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class LobbyController {

    @FXML
    private Button playButton;

    @FXML
    private Button leaderboardButton;

    @FXML
    private Button logoutButton;

    @FXML
    private void initialize() {
        playButton.setOnAction(e -> handlePlay());
        leaderboardButton.setOnAction(e -> handleLeaderboard());
        logoutButton.setOnAction(e -> handleLogout());
    }

    private void handlePlay() {
        System.out.println("Play clicked");
        SceneManager.switchTo("game.fxml"); // Make sure game.fxml exists
    }

    private void handleLeaderboard() {
        System.out.println("Leaderboard clicked");
        SceneManager.switchTo("leaderboard.fxml"); // Make sure leaderboard.fxml exists
    }

    private void handleLogout() {
        System.out.println("Logout clicked");
        SceneManager.switchTo("login.fxml");
    }
}
