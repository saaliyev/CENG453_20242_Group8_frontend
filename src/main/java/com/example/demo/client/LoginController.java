package com.example.demo.client;

import com.example.demo.client.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.json.JSONObject;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Text messageText;

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);

        try {
            String response = ApiClient.post("/auth/login", json.toString());
            JSONObject obj = new JSONObject(response);
            String token = obj.getString("token");
            String user = obj.getString("username");

            // Save session token globally
            SessionManager.getInstance().saveSession(user, token);

            System.out.println("Login successful. Token: " + token);
            messageText.setText("Login successful!");

            SceneManager.switchTo("/lobby.fxml");

        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            messageText.setText("Login failed: " + e.getMessage());
        }
    }

    @FXML
    private void goToRegister() {
        SceneManager.switchTo("register.fxml");
    }
}
