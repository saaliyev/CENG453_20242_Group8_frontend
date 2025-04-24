package com.example.demo.client;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.json.JSONObject;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Text messageText;

    @FXML
    private void onRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!password.equals(confirmPassword)) {
            messageText.setText("Passwords do not match.");
            return;
        }

        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);

        try {
            String response = ApiClient.post("/auth/register", json.toString());
            messageText.setText("Registration successful!");
            SceneManager.switchTo("login.fxml");
        } catch (Exception e) {
            messageText.setText("Registration failed: " + e.getMessage());
        }
    }

    @FXML
    private void goToLogin() {
        SceneManager.switchTo("login.fxml");
    }
}
