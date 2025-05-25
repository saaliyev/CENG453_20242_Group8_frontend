package com.example.demo.client;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class GameController implements Initializable {
    String playerName = "";
    @FXML private BorderPane rootPane;
    @FXML private VBox topContainer;
    @FXML private HBox bottomContainer;
    @FXML private HBox playerHand;
    @FXML private VBox leftHand;
    @FXML private VBox rightHand;
    @FXML private HBox topHand;
    @FXML private ImageView pileImage;
    @FXML private ImageView backCardImage;
    @FXML private Circle unoIndicatorTop;
    @FXML private Text unoIndicatorTopText;
    @FXML private Circle unoIndicatorRight;
    @FXML private Text unoIndicatorRightText;
    @FXML private Circle unoIndicatorBottom;
    @FXML private Text unoIndicatorBottomText;
    @FXML private Circle unoIndicatorLeft;
    @FXML private Text unoIndicatorLeftText;
    @FXML private ImageView directionImage;
    @FXML private Region gameColorIndicator;
    @FXML private GridPane colorPicker;
    private boolean gameEnded= false;
    private int turn=0;
    private String winner= "";
    private int score=0;

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
    @FXML private Circle dotTop;
    @FXML private Circle dotRight;
    @FXML private Circle dotBottom;
    @FXML private Circle dotLeft;

    @FXML private VBox gameOverOverlay;
    @FXML private Text winnerText;
    @FXML private Text scoreText;

    private static final Map<String, Image> imageCache = new HashMap<>();

    private Image getCachedImage(String path) {
        return imageCache.computeIfAbsent(path, p -> new Image(getClass().getResourceAsStream(p)));
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        playerName= SessionManager.getInstance().getUsername();
        System.out.println(playerName);
        topContainer.setMaxHeight(CARD_HEIGHT + 20);
        bottomContainer.setMaxHeight(CARD_HEIGHT + 20);
        backCardImage.setImage(getCachedImage("/images/uno_card-back.png"));
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
//                gameEnded = true;
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
            Platform.runLater(() -> {
                winnerText.setText("Winner is: " + winner); // implement getWinnerName()
                String newResponse = null;
                try {
                    newResponse = ApiClient.get("/game/getScore");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                int score = Integer.parseInt(newResponse.trim());
                scoreText.setText("Score is: " + score);  // implement getWinnerScore()


                gameOverOverlay.setVisible(true);

                PauseTransition pause = new PauseTransition(Duration.seconds(4));
                pause.setOnFinished(event -> SceneManager.switchTo("/lobby.fxml"));
                pause.play();
            });

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
        winner= json2.getString("winner");
        updateGameColorIndicator(gameColor);
        System.out.println("Game Color " + gameColor);
        pileImage.setImage(getCachedImage(json2.getString("pileTopImagePath")));
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
        turn= json2.getInt("turn");
        updateTurnIndicators(turn);
        updateUnoIndicators(json2.getInt("size3"), json2.getInt("size2"), json2.getInt("size1"), json2.getInt("size4"));
        applySpacing();
    }

    private ImageView createCardImageView(String path, double rotation, boolean isClickable, int index) {
        ImageView iv = new ImageView(getCachedImage(path));
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
                colorPicker.setVisible(false);
                colorPicker.setManaged(false);
                if (path.endsWith("uno_card-wildchange.png")) {

                    Index= index;
                    colorPicker.setVisible(true);
                    colorPicker.setManaged(true);
                }
                else if(path.endsWith("uno_card-wilddraw4.png")) {
                    json.put("playerName", playerName);
                    json.put("actionType", -11);
                    String response = null;
                    try {
                        response = ApiClient.post("/game/match/isPlayable", json.toString());

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if ("1".equals(response)) {
                        Index= index;
                        colorPicker.setVisible(true);
                        colorPicker.setManaged(true);

                    } else if ("0".equals(response)) {
                        System.out.println("Move is NOT playable");
                    }
                }
                else {
                    new Thread(() -> {
                        try {
                            ApiClient.post("/game/match/makeMove", json.toString()); // no need to read response
                        } catch (Exception e) {
                            System.out.println("Error making move in normal: " + e.getMessage());
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
        // Use Platform.runLater to ensure layout is updated before calculating spacing
        Platform.runLater(() -> {
            adjustSpacingHBox(playerHand, playerCards.size());
            adjustSpacingHBox(topHand, topOpponentCards.size());
            adjustSpacingVBox(leftHand, leftOpponentCards.size());
            adjustSpacingVBox(rightHand, rightOpponentCards.size());
        });
    }

    private void adjustSpacingHBox(HBox box, int count) {
        if (count < 2) {
            box.setSpacing(10); // Default spacing for single cards
            return;
        }

        // Use a fixed reference width instead of relying on dynamic width
        double availableWidth = Math.min(MAX_HAND_WIDTH, 580); // Use consistent reference width
        double totalCardWidth = count * CARD_WIDTH;

        double spacing;
        if (totalCardWidth <= availableWidth) {
            // Cards fit comfortably - use default spacing
            spacing = 10;
        } else {
            // Cards need to overlap - calculate negative spacing
            spacing = (availableWidth - CARD_WIDTH) / (count - 1) - CARD_WIDTH;
            // Ensure minimum spacing to prevent cards from overlapping too much
            spacing = Math.max(spacing, -CARD_WIDTH * 0.8); // Allow max 80% overlap
        }

        box.setSpacing(spacing);
    }

    private void adjustSpacingVBox(VBox box, int count) {
        if (count < 2) {
            box.setSpacing(10); // Default spacing for single cards
            return;
        }

        // Use a fixed reference height instead of relying on dynamic height
        double availableHeight = 400; // Fixed reference height for vertical hands
        double totalCardHeight = count * CARD_HEIGHT;

        double spacing;
        if (totalCardHeight <= availableHeight) {
            // Cards fit comfortably - use default spacing
            spacing = 10;
        } else {
            // Cards need to overlap - calculate negative spacing
            spacing = (availableHeight - CARD_HEIGHT) / (count - 1) - CARD_HEIGHT;
            // Ensure minimum spacing to prevent cards from overlapping too much
            spacing = Math.max(spacing, -CARD_HEIGHT * 0.8); // Allow max 80% overlap
        }

        box.setSpacing(spacing);
    }

    // Also update your updateGameState method to call applySpacing at the end:
// In updateGameState(), after adding all cards, make sure to call:
// applySpacing(); // This should be the last line in updateGameState()
    private void setDirection(boolean counterclockwise) {
        String imagePath = counterclockwise ? "/images/arrow_ccw.png" : "/images/arrow_cw.png";
        directionImage.setImage(getCachedImage(imagePath));
        directionImage.setPreserveRatio(true);
        directionImage.setSmooth(true);
    }

    @FXML public void onSkipClicked() {
        System.out.println("Skip button clicked");
        JSONObject playJson = new JSONObject();
        playJson.put("playerName", playerName);
        playJson.put("actionType", -6);

        new Thread(() -> {
            try {
                ApiClient.post("/game/match/makeMove", playJson.toString());
            } catch (IOException e) {
                System.err.println("Error sending wild play:");
                e.printStackTrace();
            }
        }).start();
    }

    @FXML public void onWildClicked() {
        JSONObject playJson = new JSONObject();
        playJson.put("playerName", playerName);
        playJson.put("actionType", -9);

        new Thread(() -> {
            try {
                ApiClient.post("/game/match/makeMove", playJson.toString());
            } catch (IOException e) {
                System.err.println("Error sending wild play:");
                e.printStackTrace();
            }
        }).start();
        System.out.println("Wild button clicked");
    }

    @FXML public void onReverseClicked() {
        JSONObject playJson = new JSONObject();
        playJson.put("playerName", playerName);
        playJson.put("actionType", -7);

        new Thread(() -> {
            try {
                ApiClient.post("/game/match/makeMove", playJson.toString());
            } catch (IOException e) {
                System.err.println("Error sending wild play:");
                e.printStackTrace();
            }
        }).start();
        System.out.println("Reverse button clicked");
    }

    @FXML public void onDraw2Clicked() {
        JSONObject playJson = new JSONObject();
        playJson.put("playerName", playerName);
        playJson.put("actionType", -8);

        new Thread(() -> {
            try {
                ApiClient.post("/game/match/makeMove", playJson.toString());
            } catch (IOException e) {
                System.err.println("Error sending wild play:");
                e.printStackTrace();
            }
        }).start();
        System.out.println("Draw 2 button clicked");
    }

    @FXML public void onWildDraw4Clicked() {
        JSONObject playJson = new JSONObject();
        playJson.put("playerName", playerName);
        playJson.put("actionType", -10);

        new Thread(() -> {
            try {
                ApiClient.post("/game/match/makeMove", playJson.toString());
            } catch (IOException e) {
                System.err.println("Error sending wild play:");
                e.printStackTrace();
            }
        }).start();
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

    @FXML private void onColorRed() throws IOException, InterruptedException {
        sendColorChoice(0);  // Red
    }
    @FXML private void onColorYellow() throws IOException, InterruptedException {
        sendColorChoice(1);  // Yellow
    }
    @FXML private void onColorGreen() throws IOException, InterruptedException {
        sendColorChoice(2);  // Green
    }
    @FXML private void onColorBlue() throws IOException, InterruptedException {
        sendColorChoice(3);  // Blue
    }

    private void sendColorChoice(int color) {
        // 1) Send the wild card play
        JSONObject playJson = new JSONObject();
        playJson.put("playerName", playerName);
        playJson.put("actionType", Index);


            try {
                ApiClient.post("/game/match/makeMove", playJson.toString());
            } catch (IOException e) {
                System.err.println("Error sending wild play:");
                e.printStackTrace();
            }

        // 2) Send the chosen color
        JSONObject colorJson = new JSONObject();
        colorJson.put("playerName", playerName);
        colorJson.put("actionType", color - 4);  // adjust if needed

        new Thread(() -> {
            try {
                ApiClient.post("/game/match/makeMove", colorJson.toString());
            } catch (IOException e) {
                System.err.println("Error sending color choice:");
                e.printStackTrace();
            }
        }).start();

        // hide the color picker immediately
        colorPicker.setVisible(false);
        colorPicker.setManaged(false);
    }

    private void updateTurnIndicators(int turn) {
        // Reset all dots to hidden
        dotTop.setOpacity(0);
        dotRight.setOpacity(0);
        dotLeft.setOpacity(0);
        dotBottom.setOpacity(0);

        // Set the current turn's dot to be visible
        switch (turn) {
            case 0:
                dotBottom.setOpacity(1);
                break;
            case 1:
                dotRight.setOpacity(1);
                break;
            case 2:
                dotTop.setOpacity(1);
                break;
            case 3:
                dotLeft.setOpacity(1);
                break;
        }
    }


    public void updateUnoIndicators(int bottomCount, int leftCount, int topCount, int rightCount) {
        unoIndicatorTop.setVisible(bottomCount == 1);
        unoIndicatorTopText.setVisible(bottomCount == 1);
        unoIndicatorRight.setVisible(leftCount == 1);
        unoIndicatorRightText.setVisible(leftCount == 1);
        unoIndicatorBottom.setVisible(topCount == 1);
        unoIndicatorBottomText.setVisible(topCount == 1);
        unoIndicatorLeft.setVisible(rightCount == 1);
        unoIndicatorLeftText.setVisible(rightCount == 1);
    }

}
