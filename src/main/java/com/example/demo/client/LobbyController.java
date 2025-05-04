package com.example.demo.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ResourceBundle;

public class LobbyController implements Initializable {
    @FXML private Text messageText;
    @FXML private ListView<String> leaderboardListView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fetchLeaderboard("alltime");
    }

    @FXML private void onPlayClicked() {
        SceneManager.switchTo("mode_selection.fxml");
    }

    @FXML private void onLogout() {
        // existing logout logic...
    }

    @FXML private void onWeeklyClicked() {
        fetchLeaderboard("weekly");
    }

    @FXML private void onMonthlyClicked() {
        fetchLeaderboard("monthly");
    }

    @FXML private void onAllTimeClicked() {
        fetchLeaderboard("alltime");
    }

    private void fetchLeaderboard(String period) {
        // period is "weekly", "monthly" or "alltime"
        new Thread(() -> {
            try {
                String resp = ApiClient.get("/leaderboard/" + period);
                JSONArray arr = new JSONArray(resp);

                ObservableList<String> items = FXCollections.observableArrayList();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String user = o.getString("username");
                    int score = o.getInt("totalScore");
                    items.add((i+1) + ". " + user + " – " + score);
                }

                Platform.runLater(() -> {
                    leaderboardListView.setItems(items);
                    messageText.setText(period.substring(0,1).toUpperCase() + period.substring(1) + " leaderboard");
                });
            } catch (Exception e) {
                Platform.runLater(() -> messageText.setText("Failed to load " + period + " leaderboard."));
            }
        }).start();
    }
}