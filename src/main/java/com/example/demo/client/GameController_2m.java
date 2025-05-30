package com.example.demo.client;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import java.util.concurrent.TimeUnit;

public class GameController_2m implements Initializable {
    private ScheduledExecutorService scheduler;
    String playerName = "";
    @FXML private BorderPane rootPane;
    @FXML private VBox topContainer;
    @FXML private HBox bottomContainer;
    @FXML private HBox playerHand;
    @FXML private HBox topHand;
    @FXML private ImageView pileImage;
    @FXML private ImageView backCardImage;
    @FXML private Circle unoIndicatorTop;
    @FXML private Text unoIndicatorTopText;
    @FXML private Circle unoIndicatorBottom;
    @FXML private Text unoIndicatorBottomText;
    @FXML private ImageView directionImage;
    @FXML private Region gameColorIndicator;
    @FXML private GridPane colorPicker;
    private String wildDrawFourPlayerName;
    private String challengeBaseCardImagePath;
    private boolean gameEnded= false;
    private boolean challengePending= false;
    private int turn=0;
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
    private int Index=0;
    @FXML private Circle dotTop;
    @FXML private Circle dotBottom;

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
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (!gameEnded) {
                try {
                    String newResponse = ApiClient.get("/game/getState?playerName=" + playerName);
                    JSONObject jsonState = new JSONObject(newResponse);

                    Platform.runLater(() -> {
                        try {
                            updateGameState(jsonState);
                        } catch (Exception e) {
                            System.out.println("Failed to update game state: " + e.getMessage());
                        }
                    });
                } catch (IOException e) {
                    System.out.println("Polling error: " + e.getMessage());
                }
            } else {
                shutdownScheduler();

                Platform.runLater(() -> {
                    winnerText.setText("Winner is: " + winner);
                    try {
                        String newResponse = ApiClient.get("/game/getScore");
                        int score = Integer.parseInt(newResponse.trim());
                        scoreText.setText("Score is: " + score);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    gameOverOverlay.setVisible(true);
                    PauseTransition pause = new PauseTransition(Duration.seconds(4));
                    pause.setOnFinished(event -> SceneManager.switchTo("/lobby.fxml"));
                    pause.play();
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void shutdownScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }



    private void updateGameState(JSONObject json2) {
        gameEnded = json2.getBoolean("gameEnded");
        //System.out.println("Game ended: " + gameEnded);
        this.sizes = jsonArrayToIntegerList(json2.getJSONArray("sizes"));
        this.playerCards= jsonArrayToList(json2.getJSONArray("playerCardPaths"));
        challengePending= json2.getBoolean("challengePending");
        //challengeBaseCardImagePath= json2.getString("challengeBaseCardImagePath");
       // wildDrawFourPlayerName= json2.getString("wildDrawFourPlayerName");
        setDirection(json2.getInt("counterclockwise") == 1);
        gameColor= json2.getInt("gameColor");
        winner= json2.getString("winner");
        winnerScore= json2.getInt("winnerScore");
        updateGameColorIndicator(gameColor);
        //System.out.println("Game Color " + gameColor);
        pileImage.setImage(getCachedImage(json2.getString("pileTopImagePath")));
        pileImage.setFitWidth(CARD_WIDTH);
        pileImage.setFitHeight(CARD_HEIGHT);

        playerHand.getChildren().clear();
        topHand.getChildren().clear();

        for (int i = 0; i < playerCards.size(); i++) {
            String path = playerCards.get(i);
            ImageView cardView = createCardImageView(path, 0, true, i);  // Pass the index
            playerHand.getChildren().add(cardView);
        }
        for (int i = 1; i < sizes.size(); i++) {
            Integer size = sizes.get(i);
            for (int j = 0; j < size; j++){
                // Use a meaningful index instead of player number
                ImageView cardView = createCardImageView("/images/uno_card-back.png", 0, false, -1);  // Use -1 for opponent cards
                topHand.getChildren().add(cardView);
            }
        }

        //topOpponentCards.forEach(path -> topHand.getChildren().add(createCardImageView(path, 180, false, 0 )));
        turn= json2.getInt("turn");
        updateTurnIndicators(turn);
        updateUnoIndicators(sizes.get(0), sizes.get(1));
        applySpacing();
    }

    // Replace your createCardImageView method with this debug version:

    private ImageView createCardImageView(String path, double rotation, boolean isClickable, int index) {
        //System.out.println("Creating card with path: " + path + ", index: " + index + ", clickable: " + isClickable);

        ImageView iv = new ImageView(getCachedImage(path));
        iv.setFitWidth(CARD_WIDTH);
        iv.setFitHeight(CARD_HEIGHT);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setRotate(rotation);
        iv.setPickOnBounds(true);
        iv.setMouseTransparent(false);  // Make sure this is false for clickable cards
        iv.toFront();
        iv.setStyle(isClickable ? "-fx-cursor: hand;" : "-fx-cursor: default;");

        // Add mouse entered/exited handlers for debugging
//        iv.setOnMouseEntered(event -> {
//            System.out.println("Mouse entered card at index: " + index);
//            if (isClickable) {
//                iv.setOpacity(0.8); // Visual feedback
//            }
//        });
//
//        iv.setOnMouseExited(event -> {
//            System.out.println("Mouse exited card at index: " + index);
//            iv.setOpacity(1.0); // Reset opacity
//        });

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

                // Only proceed if it's the player's turn and game hasn't ended
                if (gameEnded) {
                    System.out.println("Game has ended, ignoring click");
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
        } else {
            //System.out.println("Card at index " + index + " is NOT clickable");
        }

        return iv;
    }


    private void applySpacing() {
        // Use Platform.runLater to ensure layout is updated before calculating spacing
        Platform.runLater(() -> {
            adjustSpacingHBox(playerHand, playerCards.size());
            adjustSpacingHBox(topHand, topOpponentCards.size());
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
        playJson.put("cardIndex", Index);


            try {
                ApiClient.post("/game/match/playCard", playJson.toString());
            } catch (IOException e) {
                System.err.println("Error sending wild play:");
                e.printStackTrace();
            }

        // 2) Send the chosen color
        JSONObject colorJson = new JSONObject();
        colorJson.put("playerName", playerName);
        colorJson.put("color", color );  // adjust if needed

        new Thread(() -> {
            try {
                ApiClient.post("/game/match/changeColor", colorJson.toString());
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
        System.out.println("Turn: " + turn);
        // Reset all dots to hidden
        dotTop.setOpacity(0);
        dotBottom.setOpacity(0);

        // Set the current turn's dot to be visible
        switch (turn) {
            case 0:
                dotBottom.setOpacity(1);
                break;

            case 2:
                dotTop.setOpacity(1);
                break;

        }
    }


    public void updateUnoIndicators(int bottomCount,  int topCount) {
        unoIndicatorTop.setVisible(bottomCount == 1);
        unoIndicatorTopText.setVisible(bottomCount == 1);
        unoIndicatorBottom.setVisible(topCount == 1);
        unoIndicatorBottomText.setVisible(topCount == 1);

    }

}
