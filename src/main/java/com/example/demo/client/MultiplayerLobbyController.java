package com.example.demo.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.json.JSONObject;

import java.io.IOException;

public class MultiplayerLobbyController {

    @FXML
    private ChoiceBox<Integer> playerCountChoiceBox;

    @FXML
    private TextField hostNameField;

    @FXML
    private ListView<String> playersListView;

    @FXML
    private Label currentPlayersLabel;

    @FXML
    private Button startGameButton;
    private String playerName;

    @FXML
    public void initialize() {
        playerName= SessionManager.getInstance().getUsername();
        System.out.println("MultiplayerLobbyController initialized.");
    }

    @FXML
    private void handleCreateGame() {
        Integer selectedPlayers = playerCountChoiceBox.getValue();
        System.out.println("Player " + playerName + " created a new game");
        System.out.println("Create Game button pressed. Selected players: " + selectedPlayers);
        SceneManager.switchTo("multiplayer_lobby_status.fxml");
        JSONObject json = new JSONObject();
        json.put("playerName",  playerName); // Replace with the actual player name
        json.put("numberOfPlayers", selectedPlayers); // The index of the clicked card

        // Send the POST request with the JSON payload
        try {
            String response = ApiClient.post("/game/create", json.toString());
            System.out.println("Response from backend: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleJoinGame() {
        String hostName = hostNameField.getText();
        System.out.println("Join Game button pressed. Host name: " + hostName);
        JSONObject json = new JSONObject();
        json.put("playerName",  playerName); // Replace with the actual player name
        json.put("gameName", hostName); // The index of the clicked card
        try {
            String response = ApiClient.post("/game/enter", json.toString());
            System.out.println("Response from backend: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SceneManager.switchTo("multiplayer_lobby_status.fxml");
    }

    @FXML
    private void handleStartGame() {
        System.out.println("Start Game button pressed.");
    }
}
