package com.example.demo.client;

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
        fetchAllTimeLeaderboard();
    }

    @FXML
    private void onPlayClicked() {
        SceneManager.switchTo("mode_selection.fxml");
    }

    @FXML
    private void onLogout() {
        String username = SessionManager.getInstance().getUsername();
        String token    = SessionManager.getInstance().getToken();

        if (username == null || token == null) {
            messageText.setText("No active session.");
            return;
        }

        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("token", token);

        try {
            String response = ApiClient.post("/auth/logout", json.toString());
            SessionManager.getInstance().clearSession();
            SceneManager.switchTo("login.fxml");
        } catch (Exception e) {
            messageText.setText("Logout failed: " + e.getMessage());
        }
    }

    private void fetchAllTimeLeaderboard() {
        new Thread(() -> {
            try {
                // call your backend
                String resp = ApiClient.get("/leaderboard/alltime");
                JSONArray arr = new JSONArray(resp);

                // build a simple list of "username – score" strings
                javafx.collections.ObservableList<String> items =
                        javafx.collections.FXCollections.observableArrayList();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String user = o.getString("username");
                    int    score = o.getInt("totalScore");
                    items.add((i+1) + ". " + user + " – " + score);
                }

                // update the ListView on the JavaFX thread
                javafx.application.Platform.runLater(() ->
                        leaderboardListView.setItems(items)
                );
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        messageText.setText("Failed to load leaderboard.")
                );
            }
        }).start();
    }
}
