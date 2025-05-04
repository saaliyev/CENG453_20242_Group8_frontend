package com.example.demo.client;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class GameController implements Initializable {
    String playerName = "YourPlayerName4";
    @FXML private BorderPane rootPane;
    @FXML private VBox topContainer;
    @FXML private HBox bottomContainer;
    @FXML private HBox playerHand;
    @FXML private VBox leftHand;
    @FXML private VBox rightHand;
    @FXML private HBox topHand;
    @FXML private ImageView pileImage;
    @FXML private ImageView backCardImage;
    @FXML private ToggleButton unoTogglePlayer;
    @FXML private ToggleButton unoToggleLeft;
    @FXML private ToggleButton unoToggleTop;
    @FXML private ToggleButton unoToggleRight;
    @FXML private ImageView directionImage;
    @FXML private Region gameColorIndicator;
    @FXML private VBox colorPicker;
    private boolean gameEnded= false;


    private int gameColor = -1;
    private final List<ImageView> allHandCards = new ArrayList<>();

    private static final double CARD_WIDTH = 140;
    private static final double CARD_HEIGHT = CARD_WIDTH * 1.4;
    private static final double MAX_HAND_WIDTH = 580;

    private List<String> playerCards = List.of();
    private List<String> leftOpponentCards = List.of();
    private List<String> topOpponentCards = List.of();
    private List<String> rightOpponentCards = List.of();
    private int Index=0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        topContainer.setMaxHeight(CARD_HEIGHT + 20);
        bottomContainer.setMaxHeight(CARD_HEIGHT + 20);
        backCardImage.setImage(new Image(getClass().getResourceAsStream("/images/uno_card-back.png")));
        backCardImage.setFitWidth(CARD_WIDTH);
        backCardImage.setFitHeight(CARD_HEIGHT);
        backCardImage.setStyle("-fx-cursor: hand;");
        backCardImage.setOnMouseClicked(event -> {
            System.out.println("Deck clicked!");

            // Create the JSON payload
            JSONObject json = new JSONObject();
            json.put("playerName",  playerName); // Replace with the actual player name
            json.put("actionType", -5); // The index of the clicked card

            // Send the POST request with the JSON payload
            try {
                String response = ApiClient.post("/game/match/makeMove", json.toString());
                System.out.println("Response from backend: " + response);
            } catch (IOException e) {
                e.printStackTrace();
            }

            event.consume(); // Prevent further propagation
        });

        Boolean isMultiplayer = false;

        JSONObject json = new JSONObject();
        json.put("playerName", playerName);
        json.put("isMultiplayer", isMultiplayer);
        try {
            String response = ApiClient.post("/game/create", String.valueOf(json));
            System.out.println("Game Creation Successful: " + response);
        } catch (Exception e) {
            System.out.println("Game Creation failed: " + e.getMessage());
        }

        // Initial game state
        try {
            String response = ApiClient.get("/game/state");
            System.out.println("Initial game state: " + response);
            updateGameState(new JSONObject(response));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ChangeListener<Number> resizeListener = (obs, oldVal, newVal) -> applySpacing();
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.widthProperty().addListener(resizeListener);
                newScene.heightProperty().addListener(resizeListener);
                applySpacing();
            }
        });

        // Start polling game state in a background thread
        Thread gameStatePollingThread = new Thread(() -> {

                String lastResponse = null;
                while (!gameEnded) {
                    try {
                        String newResponse = ApiClient.get("/game/state");
                        System.out.println(newResponse);
                        if (!newResponse.equals(lastResponse)) {
                            lastResponse = newResponse;
                            JSONObject jsonState = new JSONObject(newResponse);
                            Platform.runLater(() -> {
                                try {
                                    updateGameState(jsonState);
                                } catch (Exception e) {
                                    System.out.println("Failed to update game state: " + e.getMessage());
                                }
                            });
                        }
                        Thread.sleep(1000); // 1 second polling interval
                    } catch (IOException | InterruptedException e) {
                        System.out.println("Polling error: " + e.getMessage());
                        break;
                    }
                }
        });
        gameStatePollingThread.setDaemon(true);
        gameStatePollingThread.start();
    }

    private void updateGameState(JSONObject json2) {
        gameEnded = json2.getBoolean("gameEnded");
        this.playerCards = jsonArrayToList(json2.getJSONArray("player0CardPaths"));
        this.leftOpponentCards = jsonArrayToList(json2.getJSONArray("player3CardPaths"));
        this.topOpponentCards = jsonArrayToList(json2.getJSONArray("player2CardPaths"));
        this.rightOpponentCards = jsonArrayToList(json2.getJSONArray("player1CardPaths"));

        setDirection(json2.getInt("direction") == 1);
        gameColor= json2.getInt("gameColor");
        updateGameColorIndicator(gameColor);
        System.out.println("Game Color " + gameColor);
        pileImage.setImage(new Image(getClass().getResourceAsStream(json2.getString("pileTopImagePath"))));
        pileImage.setFitWidth(CARD_WIDTH);
        pileImage.setFitHeight(CARD_HEIGHT);

        playerHand.getChildren().clear();
        leftHand.getChildren().clear();
        topHand.getChildren().clear();
        rightHand.getChildren().clear();

        for (int i = 0; i < playerCards.size(); i++) {
            String path = playerCards.get(i);
            ImageView cardView = createCardImageView(path, 0, true, i);  // Pass the index
            playerHand.getChildren().add(cardView);
        }

        leftOpponentCards.forEach(path -> leftHand.getChildren().add(createCardImageView(path, 90, false, 0)));
        topOpponentCards.forEach(path -> topHand.getChildren().add(createCardImageView(path, 180, false, 0 )));
        rightOpponentCards.forEach(path -> rightHand.getChildren().add(createCardImageView(path, -90, false, 0)));

        applySpacing();
    }

    private ImageView createCardImageView(String path, double rotation, boolean isClickable, int index) {
        ImageView iv = new ImageView(new Image(getClass().getResourceAsStream(path)));
        iv.setFitWidth(CARD_WIDTH);
        iv.setFitHeight(CARD_HEIGHT);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setRotate(rotation);
        iv.setPickOnBounds(true);
        iv.setMouseTransparent(false);
        iv.toFront();
        iv.setStyle(isClickable ? "-fx-cursor: hand;" : "-fx-cursor: default;");

        iv.setOnMouseClicked(event -> {
            System.out.println("Card clicked! Index: " + index + ", Path: " + path);

            JSONObject json = new JSONObject();
            json.put("playerName", playerName);
            json.put("actionType", index);

                if (path.endsWith("uno_card-wildchange.png") || path.endsWith("uno_card-wilddraw4.png")) {
                    Index= index;
                    colorPicker.setVisible(true);
                    colorPicker.setManaged(true);
                }
                else {
                    new Thread(() -> {
                        try {
                            ApiClient.post("/game/match/makeMove", json.toString()); // no need to read response
                        } catch (Exception e) {
                            System.out.println("Error making move: " + e.getMessage());
                        }
                    }).start();
                }




                // Check if the card is a wild card

            event.consume(); // Prevent further propagation
        });

        allHandCards.add(iv);
        return iv;
    }


    private void applySpacing() {
        adjustSpacingHBox(playerHand, playerCards.size());
        adjustSpacingHBox(topHand, topOpponentCards.size());
        adjustSpacingVBox(leftHand, leftOpponentCards.size());
        adjustSpacingVBox(rightHand, rightOpponentCards.size());
    }

    private void adjustSpacingHBox(HBox box, int count) {
        if (count < 2) return;
        double regionWidth = box.getWidth();
        double totalCardWidth = count * CARD_WIDTH;
        double spacing = (totalCardWidth <= Math.min(regionWidth, MAX_HAND_WIDTH))
                ? 10
                : (Math.min(regionWidth, MAX_HAND_WIDTH) - CARD_WIDTH) / (count - 1) - CARD_WIDTH;
        box.setSpacing(spacing);
    }

    private void adjustSpacingVBox(VBox box, int count) {
        if (count < 2) return;
        double regionHeight = box.getHeight();
        double totalCardHeight = count * CARD_HEIGHT;
        double spacing = (totalCardHeight <= regionHeight)
                ? 10
                : (regionHeight - CARD_HEIGHT) * 1.3 / (count - 1) - CARD_HEIGHT;
        box.setSpacing(spacing);
    }

    private void setDirection(boolean counterclockwise) {
        String imagePath = counterclockwise ? "/images/arrow_ccw.png" : "/images/arrow_cw.png";
        directionImage.setImage(new Image(getClass().getResourceAsStream(imagePath)));
        directionImage.setPreserveRatio(true);
        directionImage.setSmooth(true);
    }

    @FXML public void onSkipClicked() {
        System.out.println("Skip button clicked");
    }

    @FXML public void onWildClicked() {
        System.out.println("Wild button clicked");
    }

    @FXML public void onReverseClicked() {
        System.out.println("Reverse button clicked");
    }

    @FXML public void onDraw2Clicked() {
        System.out.println("Draw 2 button clicked");
    }

    @FXML public void onWildDraw4Clicked() {
        System.out.println("Wild Draw 4 button clicked");
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }
    private void updateGameColorIndicator(int color) {
        String fxColor;
        switch (color) {
            case 0 -> fxColor = "red";
            case 1 -> fxColor = "yellow";
            case 2 -> fxColor = "green";
            case 3 -> fxColor = "blue";
            default -> fxColor = "gray";
        }
        gameColorIndicator.setStyle("-fx-background-color: " + fxColor + "; -fx-border-color: black; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    @FXML private void onColorRed() throws IOException {
        sendColorChoice(0);  // Red
    }
    @FXML private void onColorYellow() throws IOException {
        sendColorChoice(1);  // Yellow
    }
    @FXML private void onColorGreen() throws IOException {
        sendColorChoice(2);  // Green
    }
    @FXML private void onColorBlue() throws IOException {
        sendColorChoice(3);  // Blue
    }

    private void sendColorChoice(int color) throws IOException {
        System.out.println("girdi bura" );
        JSONObject json = new JSONObject();
        json.put("playerName", playerName);
        json.put("actionType", Index);

        String response = ApiClient.post("/game/match/makeMove", json.toString());
        System.out.println("Response from backend: " + response);
        JSONObject colorJson = new JSONObject();
        colorJson.put("playerName", playerName);
        colorJson.put("actionType", color - 4);  // Adjust as your backend expects

        try {
            String colorResponse = ApiClient.post("/game/match/makeMove", colorJson.toString());
            System.out.println("Color chosen response: " + colorResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }

        colorPicker.setVisible(false);
        colorPicker.setManaged(false);
    }


}
