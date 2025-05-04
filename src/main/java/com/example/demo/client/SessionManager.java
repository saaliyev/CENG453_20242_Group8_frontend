package com.example.demo.client;

public class SessionManager {
    private static SessionManager instance;
    private String username;
    private String token;
    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void saveSession(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public void clearSession() {
        this.token = null;
        this.username = null;
    }

    public boolean isLoggedIn() {
        return token != null;
    }
}
