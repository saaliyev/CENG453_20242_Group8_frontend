package com.example.demo.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.json.JSONObject;

public class LobbyController {

    @FXML private Text messageText;

    @FXML
    private void onPlayClicked() {
        SceneManager.switchTo("mode_selection.fxml");
    }

    @FXML
    private void onLogout(ActionEvent actionEvent) {
        String username = SessionManager.getInstance().getUsername();
        String token = SessionManager.getInstance().getToken();

        if (username == null || token == null) {
            messageText.setText("No active session.");
            return;
        }

        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("token", token);

        try {
            String response = ApiClient.post("/auth/logout", json.toString());
            System.out.println("Logout response: " + response);

            SessionManager.getInstance().clearSession();
            SceneManager.switchTo("/login.fxml");
        } catch (Exception e) {
            System.out.println("Logout failed: " + e.getMessage());
            messageText.setText("Logout failed: " + e.getMessage());
        }
    }
}
