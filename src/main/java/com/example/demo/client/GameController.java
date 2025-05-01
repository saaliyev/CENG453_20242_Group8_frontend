package com.example.demo.client;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class GameController implements Initializable {

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox topContainer;
    @FXML
    private HBox bottomContainer;
    @FXML
    private HBox playerHand;
    @FXML
    private VBox leftHand;
    @FXML
    private VBox rightHand;
    @FXML
    private HBox topHand;
    @FXML
    private ImageView pileImage;
    @FXML
    private ImageView backCardImage;
    @FXML private ToggleButton unoTogglePlayer;
    @FXML private ToggleButton unoToggleLeft;
    @FXML private ToggleButton unoToggleTop;
    @FXML private ToggleButton unoToggleRight;

    @FXML
    private ImageView directionImage;



    // UNO indicators
    @FXML
    private ImageView unoBottomIndicator;
    @FXML
    private ImageView unoTopIndicator;
    @FXML
    private ImageView unoLeftIndicator;
    @FXML
    private ImageView unoRightIndicator;

    private final List<ImageView> allHandCards = new ArrayList<>();

    private static final double CARD_WIDTH = 140;
    private static final double CARD_HEIGHT = CARD_WIDTH * 1.4;
    private static final double MAX_HAND_WIDTH = 580;

    private List<String> playerCards = List.of();
    private List<String> leftOpponentCards = List.of();
    private List<String> topOpponentCards = List.of();
    private List<String> rightOpponentCards = List.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        topContainer.setMaxHeight(CARD_HEIGHT + 20);
        bottomContainer.setMaxHeight(CARD_HEIGHT + 20);
        backCardImage.setImage(new Image(getClass().getResourceAsStream("/images/uno_card-back.png")));
        backCardImage.setFitWidth(CARD_WIDTH);
        backCardImage.setFitHeight(CARD_HEIGHT);
        backCardImage.setStyle("-fx-cursor: hand;");
        backCardImage.setOnMouseClicked(event -> System.out.println("Deck clicked!"));

        Boolean isMultiplayer = false;
        String playerName = "";

        JSONObject json = new JSONObject();
        json.put("playerName", playerName);
        json.put("isMultiplayer", isMultiplayer);
        try {
            String response = ApiClient.post("/game/create", String.valueOf(json));
            System.out.println("Game Creation Successfull: " + response); // debug
        } catch (Exception e) {
            System.out.println("Game Creation failed: " + e.getMessage()); // debug
        }

        String response= null;
        try {
            response = ApiClient.get("/game/state");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Game state: " + response);

        JSONObject json2 = new JSONObject(response);

        this.playerCards = jsonArrayToList(json2.getJSONArray("player0CardPaths"));
        this.leftOpponentCards = jsonArrayToList(json2.getJSONArray("player3CardPaths"));
        this.topOpponentCards = jsonArrayToList(json2.getJSONArray("player2CardPaths"));
        this.rightOpponentCards = jsonArrayToList(json2.getJSONArray("player1CardPaths"));

//        JSONObject json2 = new JSONObject(response);
//
//        // Access the first card object
//        JSONObject firstCard = json2.getJSONArray("player0Cards").getJSONObject(0);
//
//        // Extract individual values
//        boolean special = firstCard.getBoolean("special");
//        String color = firstCard.getString("color");
//        String value = firstCard.getString("value");
//
//        System.out.println("Special: " + special);
//        System.out.println("Color: " + color);
//        System.out.println("Value: " + value);

        setDirection(true);

        pileImage.setImage(new Image(getClass().getResourceAsStream("/images/uno_card-yellow2.png")));
        pileImage.setFitWidth(CARD_WIDTH);
        pileImage.setFitHeight(CARD_HEIGHT);



        playerCards.forEach(path -> playerHand.getChildren().add(createCardImageView(path, 0)));
        leftOpponentCards.forEach(path -> leftHand.getChildren().add(createCardImageView(path, 90)));
        topOpponentCards.forEach(path -> topHand.getChildren().add(createCardImageView(path, 180)));
        rightOpponentCards.forEach(path -> rightHand.getChildren().add(createCardImageView(path, -90)));

        ChangeListener<Number> resizeListener = (obs, oldVal, newVal) -> applySpacing();
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.widthProperty().addListener(resizeListener);
                newScene.heightProperty().addListener(resizeListener);
                applySpacing();
            }
        });

    }

    private ImageView createCardImageView(String path, double rotation) {
        ImageView iv = new ImageView(new Image(getClass().getResourceAsStream(path)));
        iv.setFitWidth(CARD_WIDTH);
        iv.setFitHeight(CARD_HEIGHT);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setRotate(rotation);
        iv.setStyle("-fx-cursor: hand;");
        iv.setOnMouseClicked(event -> System.out.println("Card clicked: " + path));
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



    private void setDirection(boolean clockwise) {
        String imagePath = clockwise ? "/images/arrow_cw.png" : "/images/arrow_ccw.png";
        directionImage.setImage(new Image(getClass().getResourceAsStream(imagePath)));
        directionImage.setPreserveRatio(true);
        directionImage.setSmooth(true);
    }

    @FXML
    public void onSkipClicked() {
        ;
    }

    @FXML
    public void onWildClicked() {
        ;
    }

    @FXML
    public void onReverseClicked() {
        ;
    }

    @FXML
    public void onDraw2Clicked() {
        ;
    }

    @FXML
    public void onWildDraw4Clicked() {
        ;
    }


    @FXML
    public void onUnoTogglePlayer() {
        boolean declaredUno = unoTogglePlayer.isSelected();
        unoTogglePlayer.setStyle(declaredUno
                ? "-fx-background-color: green; -fx-text-fill: white; -fx-font-weight: bold;"
                : "-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold;");
        System.out.println(declaredUno ? "You declared UNO!" : "You revoked UNO!");
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }

}