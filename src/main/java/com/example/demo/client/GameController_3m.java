package com.example.demo.client;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import java.util.concurrent.ScheduledExecutorService;

public class GameController_3m implements Initializable {
    private ScheduledExecutorService scheduler;
    String playerName = "";
    @FXML private BorderPane rootPane;
    @FXML private VBox topContainer;
    @FXML private HBox bottomContainer;

    @FXML private HBox playerHand;
    @FXML private VBox rightHand;
    @FXML private HBox topHand;
    @FXML private ImageView pileImage;
    @FXML private ImageView backCardImage;
    @FXML private Circle unoIndicatorTop;
    @FXML private Text unoIndicatorTopText;
    @FXML private Circle unoIndicatorBottom;
    @FXML private Text unoIndicatorBottomText;
    @FXML private Circle unoIndicatorRight;
    @FXML private Text unoIndicatorRightText;
    @FXML private ImageView directionImage;
    @FXML private Region gameColorIndicator;
    @FXML private GridPane colorPicker;
    @FXML private VBox challengeMenu; // New FXML element for the challenge menu
    private String wildDrawFourPlayerName; // This field should be populated by the backend
    private String challengeBaseCardImagePath;
    private boolean gameEnded= false;
    private boolean challengePending= false;
    private int turn=0;
    private int turn_additive=-0;
    private int winnerScore=0;
    private String winner= "";
    private int score=0;

    private int gameColor = -1;
    private final List<ImageView> allHandCards = new ArrayList<>();

    private static final double CARD_WIDTH = 140;
    private static final double CARD_HEIGHT = CARD_WIDTH * 1.4;
    private static final double MAX_HAND_WIDTH = 580;

    private List<Integer> sizes = new ArrayList<>();
    private List<String> playerCards = List.of();
    private List<String> topOpponentCards = List.of();
    private List<String> rightOpponentCards = List.of();
    private int Index=0;
    @FXML private Circle dotTop;
    @FXML private Circle dotBottom;
    @FXML private Circle dotRight;

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
        turn_additive= SessionManager.getInstance().getTurn();
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

            // Send the POST request with the JSON payload
            try {
                String response = ApiClient.post("/game/match/drawCard", json.toString());
                System.out.println("Response from backend: " + response);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });


        // Initial game state
        try {
            String response = ApiClient.get("/game/getState?playerName=" + playerName);
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
                    String newResponse = ApiClient.get("/game/getState?playerName=" + playerName);
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

    private void shutdownScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }



    private void updateGameState(JSONObject json2) {
        gameEnded = json2.getBoolean("gameEnded");
        this.sizes = jsonArrayToIntegerList(json2.getJSONArray("sizes"));
        this.playerCards = jsonArrayToList(json2.getJSONArray("playerCardPaths"));
        challengePending = json2.getBoolean("challengePending");
        wildDrawFourPlayerName = json2.optString("wildDrawFourPlayerName", "");
        setDirection(json2.getInt("counterclockwise") == 1);
        gameColor = json2.getInt("gameColor");
        winner = json2.getString("winner");
        winnerScore = json2.getInt("winnerScore");

        updateGameColorIndicator(gameColor);
        pileImage.setImage(getCachedImage(json2.getString("pileTopImagePath")));
        pileImage.setFitWidth(CARD_WIDTH);
        pileImage.setFitHeight(CARD_HEIGHT);

        // Clear all hands
        playerHand.getChildren().clear();
        topHand.getChildren().clear();
        rightHand.getChildren().clear();

        // Add cards to player's hand (bottom = self)
        for (int i = 0; i < playerCards.size(); i++) {
            String path = playerCards.get(i);
            ImageView cardView = createCardImageView(path, 0, true, i);
            playerHand.getChildren().add(cardView);
        }

        // Offsets from self (turn_additive) to UI positions
        int rightIndex = (turn_additive + 1) % 3;
        int topIndex = (turn_additive + 2) % 3;

        // Add card backs for opponents
        for (int i = 0; i < sizes.get(rightIndex); i++) {
            ImageView cardView = createCardImageView("/images/uno_card-back.png", 90, false, -1);
            rightHand.getChildren().add(cardView);
            System.out.println("right");
        }
        for (int i = 0; i < sizes.get(topIndex); i++) {
            ImageView cardView = createCardImageView("/images/uno_card-back.png", 180, false, -1);
            topHand.getChildren().add(cardView);
            System.out.println("top");
        }


        turn = json2.getInt("turn");

        // Challenge logic
        if (challengePending &&
                !wildDrawFourPlayerName.isEmpty() &&
                !playerName.equals(wildDrawFourPlayerName) &&
                (turn == turn_additive)) {
            challengeMenu.setVisible(true);
            challengeMenu.setManaged(true);
        } else {
            challengeMenu.setVisible(false);
            challengeMenu.setManaged(false);
        }

        updateTurnIndicators(turn);
        updateUnoIndicators(sizes); // Optional: update for all 4 players
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

        if (isClickable) {
            allHandCards.add(iv);
            iv.setOnMouseClicked(event -> {
                if (!Platform.isFxApplicationThread()) {
                    System.out.println("Click handler not on FX thread!");
                    return;
                }
                event.consume();
                System.out.println("=== CARD CLICKED DEBUG INFO ===");
                System.out.println("Card clicked! Index: " + index + ", Path: " + path);
                System.out.println("Event source: " + event.getSource());
                System.out.println("Click coordinates: " + event.getX() + ", " + event.getY());
                System.out.println("Player name: " + playerName);
                System.out.println("Turn: " + turn);
                System.out.println("Game ended: " + gameEnded);
                System.out.println("===============================");

                if (gameEnded || challengeMenu.isVisible()) { // Prevent card clicks if game ended or challenge menu is open
                    System.out.println("Game has ended or challenge is pending, ignoring card click");
                    return;
                }

                JSONObject json = new JSONObject();
                json.put("playerName", playerName);
                json.put("cardIndex", index);
                colorPicker.setVisible(false);
                colorPicker.setManaged(false);
                if (path.endsWith("uno_card-wildchange.png")) {
                    Index= index;
                    colorPicker.setVisible(true);
                    colorPicker.setManaged(true);
                }
                else if (path.endsWith("uno_card-wilddraw4.png")){
                    Index= index;
                    colorPicker.setVisible(true);
                    colorPicker.setManaged(true);
                }
                else {
                    new Thread(() -> {
                        try {
                            String response = ApiClient.post("/game/match/playCard", json.toString());
                            System.out.println("API Response: " + response);
                        } catch (Exception e) {
                            System.out.println("Error making move: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();
                }
            });
        }

        return iv;
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

    private void applySpacing() {
        Platform.runLater(() -> {
            // Apply spacing for human player
            adjustSpacingHBox(playerHand, playerCards.size());

            // Calculate opponent indices based on turn order
            int rightIndex = (turn_additive + 1) % 3;
            int topIndex = (turn_additive + 2) % 3;

            // Apply spacing for AI opponents using their card counts
            adjustSpacingHBox(topHand, sizes.get(topIndex));
            adjustSpacingVBox(rightHand, sizes.get(rightIndex));
        });
    }


    private void setDirection(boolean counterclockwise) {
        String imagePath = counterclockwise ? "/images/arrow_ccw.png" : "/images/arrow_cw.png";
        directionImage.setImage(getCachedImage(imagePath));
        directionImage.setPreserveRatio(true);
        directionImage.setSmooth(true);
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }
    private List<Integer> jsonArrayToIntegerList(JSONArray array) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getInt(i));
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

    @FXML private void onColorRed() {
        sendColorChoice(0);
    }
    @FXML private void onColorYellow() {
        sendColorChoice(1);
    }
    @FXML private void onColorGreen() {
        sendColorChoice(2);
    }
    @FXML private void onColorBlue() {
        sendColorChoice(3);
    }

    private void sendColorChoice(int color) {
        JSONObject playJson = new JSONObject();
        playJson.put("playerName", playerName);
        playJson.put("cardIndex", Index);

        try {
            ApiClient.post("/game/match/playCard", playJson.toString());
        } catch (IOException e) {
            System.err.println("Error sending wild play:");
            e.printStackTrace();
        }

        JSONObject colorJson = new JSONObject();
        colorJson.put("playerName", playerName);
        colorJson.put("color", color );

        new Thread(() -> {
            try {
                ApiClient.post("/game/match/changeColor", colorJson.toString());
            } catch (IOException e) {
                System.err.println("Error sending color choice:");
                e.printStackTrace();
            }
        }).start();

        colorPicker.setVisible(false);
        colorPicker.setManaged(false);
    }

    private void updateTurnIndicators(int turn) {
        System.out.println("Turn: " + turn);

        // Reset all indicators
        dotBottom.setOpacity(0);
        dotRight.setOpacity(0);
        dotTop.setOpacity(0);

        // Map the actual turn to a relative position from the perspective of the current player (turn_additive)
        int relativeTurn = (turn - turn_additive + 3) % 3;

        // Set indicator based on position
        switch (relativeTurn) {
            case 0:
                dotBottom.setOpacity(1);
                break;
            case 1:
                dotRight.setOpacity(1);
                break;
            case 2:
                dotTop.setOpacity(1);
                break;
        }
    }



    public void updateUnoIndicators(List<Integer> sizes) {
        // Indices relative to turn_additive (self)
        int bottomIndex = turn_additive;
        int rightIndex = (turn_additive + 1) % 3;
        int topIndex = (turn_additive + 2) % 3;

        // Get hand sizes
        int bottomCount = sizes.get(bottomIndex);
        int rightCount = sizes.get(rightIndex);
        int topCount = sizes.get(topIndex);

        // Show/hide indicators
        unoIndicatorBottom.setVisible(bottomCount == 1);
        unoIndicatorBottomText.setVisible(bottomCount == 1);

        unoIndicatorRight.setVisible(rightCount == 1);
        unoIndicatorRightText.setVisible(rightCount == 1);

        unoIndicatorTop.setVisible(topCount == 1);
        unoIndicatorTopText.setVisible(topCount == 1);

    }


    // --- New Methods for Challenge Menu ---
    @FXML
    private void onChallenge() {
        System.out.println("Challenge button clicked!");
        hideChallengeMenu();
        // Send challenge request to backend
        JSONObject json = new JSONObject();
        json.put("playerName", playerName);
        new Thread(() -> {
            try {
                String response = ApiClient.post("/game/match/challenge", json.toString());
                System.out.println("Challenge response: " + response);
            } catch (IOException e) {
                System.err.println("Error sending challenge:");
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onAccept() {
        System.out.println("Accept button clicked!");
        hideChallengeMenu();
        // Send accept request to backend (implicitly, by not challenging)
        // The backend should automatically apply the +4 penalty if no challenge is sent or if the challenge fails.
        // You might need an explicit "accept" API call if your backend requires it to signal the turn progression.
        // For now, we assume the backend handles it if no challenge is sent.
        JSONObject json = new JSONObject();
        json.put("playerName", playerName);
        new Thread(() -> {
            try {
                // Assuming a generic "continueTurn" or "skipChallenge" endpoint if necessary
                String response = ApiClient.post("/game/match/declineChallenge", json.toString());
                System.out.println("Accept response: " + response);
            } catch (IOException e) {
                System.err.println("Error sending accept:");
                e.printStackTrace();
            }
        }).start();
    }

    private void hideChallengeMenu() {
        challengeMenu.setVisible(false);
        challengeMenu.setManaged(false);
    }
}