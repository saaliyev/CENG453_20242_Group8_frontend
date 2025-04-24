package com.example.demo.client;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
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
            System.out.println("Login response: " + response); // debug
            messageText.setText("Login successful!");
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage()); // debug
            messageText.setText("Login failed: " + e.getMessage());
        }
    }


    @FXML
    private void goToRegister() {
        SceneManager.switchTo("register.fxml");
    }

}
