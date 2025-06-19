package com.example.demo.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MultiplayerLobbyStatusController {

    @FXML
    private Label playerCountLabel;

    @FXML
    private ListView<String> playerListView;

    @FXML
    private Button startGameButton;

    private final Timer pollTimer = new Timer();

    private String playerName;

    public void initialize() {
        startPollingLobbyStatus();
        playerName = SessionManager.getInstance().getUsername();
    }

    private void startPollingLobbyStatus() {
        pollTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    String response = ApiClient.get("/game/getStatus");
                    JSONObject json = new JSONObject(response);

                    int currentPlayers = json.getInt("curNumberOfPlayers");
                    int maxPlayers = json.getInt("numberOfPlayers");
                    String gameStatus = json.getString("gameStatus");
                    System.out.println(gameStatus);
                    if(gameStatus.equals("started")) {
                        stopPolling();
                        if(maxPlayers==2) {
                            Platform.runLater(() -> {
                                SceneManager.switchTo("game_new" +
                                        ".fxml");
                            });
                        }
                        else if(maxPlayers==4) {
                            Platform.runLater(() -> {
                                SceneManager.switchTo("game_4m" +
                                        ".fxml");
                            });
                        }
                        else if(maxPlayers==3) {
                            Platform.runLater(() -> {
                                SceneManager.switchTo("game_3m" +
                                        ".fxml");
                            });
                        }
                    }
                    JSONArray namesArray = json.getJSONArray("playerNames");

                    ArrayList<String> players = new ArrayList<>();
                    for (int i = 0; i < namesArray.length(); i++) {
                        players.add(namesArray.getString(i));
                    }

                    Platform.runLater(() -> updateLobbyStatus(currentPlayers, maxPlayers, players));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 2000); // poll every 2 seconds
    }

    public void updateLobbyStatus(int currentPlayers, int maxPlayers, ArrayList<String> playerNames) {
        playerCountLabel.setText("Current Players: " + currentPlayers + " / " + maxPlayers);
        playerListView.getItems().setAll(playerNames);
        startGameButton.setVisible(currentPlayers == maxPlayers);

        // Print all player names to the console
        System.out.println("Players in the lobby:");
        for (int i = 0; i < playerNames.size(); i++) {
            String name = playerNames.get(i);
            System.out.println("- " + name);

            if (name.equals(playerName)) {
                SessionManager.getInstance().saveTurn(i);
                System.out.println("Player with name "+ playerName + " and index " + i + " has joined the lobby");
            }
        }
    }

    @FXML
    public void handleStartGame() {
        System.out.println("Starting the game...");
        JSONObject json = new JSONObject();
        json.put("playerName",  playerName); // Replace with the actual player name
        try {
            String response = ApiClient.post("/game/start", json.toString());
            System.out.println("Response from backend: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Optional cleanup
    public void stopPolling() {
        pollTimer.cancel();
    }
}
