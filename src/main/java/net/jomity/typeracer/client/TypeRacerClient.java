package net.jomity.typeracer.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableNumberValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import net.jomity.typeracer.shared.constants.DisconnectionReason;
import net.jomity.typeracer.shared.constants.PlayerInformation;
import net.jomity.typeracer.shared.constants.Result;
import net.jomity.typeracer.shared.network.Connection;
import net.jomity.typeracer.shared.network.packets.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

enum SceneName {
    MENU, WAITING, GAME, GAMEOVER
}

class TextGroup {
    int pointer = 0;
    String content;
    ArrayList<String> written = new ArrayList<>();
    ArrayList<String> upcoming = new ArrayList<>();
    public Text writtenText = new Text();
    public Text selectedText = new Text();
    public Text upcomingText = new Text();
    public TextFlow flow;
    boolean inError = false;
    Color errorColor;
    Color normalColor;
    boolean lastWasSpace = false;

    public TextGroup(String content, ObservableNumberValue height, Color color) {
        this.content = content;
        this.errorColor = TypeRacerClient.combineColor(color, Color.MEDIUMVIOLETRED);
        this.normalColor = TypeRacerClient.combineColor(TypeRacerClient.combineColor(color, Color.BLACK), Color.DARKGRAY);

        writtenText.setFont(TypeRacerClient.FONT);
        writtenText.setFill(TypeRacerClient.combineColor(TypeRacerClient.combineColor(color, Color.BLACK), Color.BLACK));
        writtenText.setBoundsType(TextBoundsType.VISUAL);

        selectedText.setFont(TypeRacerClient.FONT);
        selectedText.setFill(this.normalColor);
        selectedText.setBoundsType(TextBoundsType.VISUAL);
        selectedText.setUnderline(true);

        upcomingText.setFont(TypeRacerClient.FONT);
        upcomingText.setFill(this.normalColor);

        flow = new TextFlow(writtenText, selectedText, upcomingText);
        flow.setPrefWidth(Region.USE_COMPUTED_SIZE);
        flow.setTextAlignment(TextAlignment.CENTER);
        flow.translateYProperty().bind(Bindings.subtract(Bindings.divide(height, 2), writtenText.getLayoutBounds().getHeight() / 2));

        for (int i = this.content.length() - 1; i >= 0; i--) {
            upcoming.addFirst(this.content.substring(i, i + 1));
        }

        update();
    }

    // returns true if a word was completed correctly (space)
    public boolean type(String ch) {
        String atPos = content.substring(pointer, pointer + 1);
        if (!atPos.equals(ch)) {
            inError = true;
        }
        else {
            inError = false;
            written.addFirst(upcoming.removeFirst());
            pointer++;
        }

        update();

        if (!atPos.equals(" ")) {
            lastWasSpace = false;
            return false;
        }

        if (lastWasSpace) {
            return false;
        }
        else {
            lastWasSpace = true;
            return true;
        }
    }

    public void update() {
        writtenText.setText(""); selectedText.setText(""); upcomingText.setText("");
        selectedText.setFill(this.inError ? this.errorColor : this.normalColor);

        int upcomingMax = Math.min(upcoming.size(), 25);

        StringBuilder out1 = new StringBuilder();
        for (int i = 0; i < Math.min(written.size(), 30 - upcomingMax); i++) {
            out1.append(written.get(i));
        }
        writtenText.setText(out1.reverse().toString());

        StringBuilder out2 = new StringBuilder();
        for (int i = 0; i < upcomingMax; i++) {
            String ch = upcoming.get(i);
            if (i == 0) {
                selectedText.setText(ch.equals(" ") ? "_" : ch);
                continue;
            }
            out2.append(ch);
        }
        upcomingText.setText(out2.toString());

        flow.getChildren().clear();
        flow.getChildren().add(writtenText);
        flow.getChildren().add(selectedText);
        flow.getChildren().add(upcomingText);
    }
}

public class TypeRacerClient extends Application {
    public final static Font FONT = Font.font("Monospaced", FontWeight.SEMI_BOLD, 25);
    public final static Font TITLE_FONT = Font.font("Monospaced", FontWeight.BOLD, 30);
    public final static Font TEXT_FONT = Font.font("Monospaced", FontWeight.NORMAL, 12);
    public final static Font NAME_FONT = Font.font("Monospaced", FontWeight.BOLD, 20);

    Connection connection;
    public String content;
    public Result result;
    public double playerWPM;
    public double opponentWPM;
    public PlayerInformation player;
    public PlayerInformation opponent;

    SceneName activeScene;

    Stage stage;
    Label information = new Label();

    // game info
    TextGroup playerText;
    TextGroup opponentText;
    boolean gameStarted = false;
    Label timeLeft = new Label();

    private Scene MenuScene(double width, double height) {
        activeScene = SceneName.MENU;

        Label title = new Label();
        title.setAlignment(Pos.CENTER);
        title.setText("TYPE RACER");
        title.setFont(TITLE_FONT);

        TextField serverField = new TextField();
        serverField.setFocusTraversable(false);
        serverField.setFont(TEXT_FONT);
        serverField.setText("localhost");
        serverField.setPrefWidth(100); serverField.setMaxWidth(100);

        Button playButton = new Button();
        playButton.setFont(TEXT_FONT);
        playButton.setFocusTraversable(false);
        playButton.setText("Play Game");

        VBox center = new VBox();
        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(title, serverField, playButton);
        VBox.setMargin(playButton, new Insets(5, 5, 5, 5));

        Label description = new Label();
        description.setFont(TEXT_FONT);
        description.setText("Your Information:");

        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setFocusTraversable(false);

        TextField nameField = new TextField();
        nameField.setFont(TEXT_FONT);
        nameField.setFocusTraversable(false);
        nameField.setText("Player");
        nameField.prefWidthProperty().bind(colorPicker.widthProperty());
        nameField.maxWidthProperty().bind(colorPicker.widthProperty());

        VBox options = new VBox();
        options.setAlignment(Pos.TOP_LEFT);
        options.setSpacing(5);
        options.getChildren().addAll(description, nameField, colorPicker);

        information.setAlignment(Pos.CENTER);
        VBox info = new VBox();
        info.setAlignment(Pos.CENTER);
        info.getChildren().add(information);

        AnchorPane anchorPane = new AnchorPane();
        anchorPane.getChildren().add(options);
        AnchorPane.setTopAnchor(options, 10.0);
        AnchorPane.setLeftAnchor(options, 10.0);

        BorderPane main = new BorderPane();
        BorderPane.setMargin(info, new Insets(10, 10, 10, 10));
        main.setCenter(center);
        main.setBottom(info);
        main.getChildren().add(anchorPane);

        playButton.setOnAction(event -> {
            playButton.setDisable(true);
            information.setTextFill(Color.GRAY);
            information.setText("Connecting to the server...");

            new Thread(() -> {
                try {
                    connection = new Connection(new Socket(serverField.getText(), 3001));

                    listenForPackets();

                    player = new PlayerInformation(nameField.getText(), colorPicker.getValue());
                    connection.sendPacket(new RegisterPacket(player));

                    Platform.runLater(() -> setScene(WaitingScene(stage.getScene().getWidth(), stage.getScene().getHeight())));
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        information.setTextFill(Color.MEDIUMVIOLETRED);
                        information.setText("Connection failed. Try again later.");
                    });
                } finally {
                    Platform.runLater(() -> playButton.setDisable(false));
                }
            }).start();
        });

        nameField.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                String text = nameField.getText().trim();
                System.out.println(text.length() + " " + text.matches("^[\\w\\-\\s]+$"));
                if (!text.matches("^[\\w\\-\\s]+$") || text.length() > 16 || text.length() < 3) {
                    nameField.setText("Player");
                } else {
                    nameField.setText(text);
                }
            }
        });

        serverField.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                playButton.setDisable(!serverField.getText().matches("^localhost|(((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4})$"));
            }
        });

        nameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                nameField.getParent().requestFocus();
            }
        });

        serverField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                serverField.getParent().requestFocus();
            }
        });

        Scene menu = new Scene(main, width, height);
        menu.setOnMousePressed(event -> {
            if (!nameField.equals(event.getSource())) {
                nameField.getParent().requestFocus();
            }

            if (!serverField.equals(event.getSource())) {
                serverField.getParent().requestFocus();
            }
        });

        return menu;
    }

    private Scene WaitingScene(double width, double height) {
        activeScene = SceneName.WAITING;

        Label title = new Label();
        title.setAlignment(Pos.CENTER);
        title.setText("Searching for opponent...");
        title.setFont(TITLE_FONT);

        Button cancelButton = new Button();
        cancelButton.setFont(TEXT_FONT);
        cancelButton.setText("Cancel search");
        cancelButton.setFocusTraversable(false);
        cancelButton.setOnAction(event -> {
            disconnect(DisconnectionReason.USER);
            connection = null;

            Platform.runLater(() -> {
                information.setText("");
                setScene(MenuScene(stage.getScene().getWidth(), stage.getScene().getHeight()));
            });
        });

        VBox center = new VBox();
        center.setAlignment(Pos.CENTER);
        VBox.setMargin(cancelButton, new Insets(5, 5, 5, 5));
        center.getChildren().addAll(title, cancelButton);

        BorderPane main = new BorderPane();
        main.setCenter(center);

        return new Scene(main, width, height);
    }

    private Scene GameScene(double width, double height) {
        activeScene = SceneName.GAME;

        // PLAYER
        BorderPane playerContainer = new BorderPane();
        AnchorPane playerAnchor = maxAnchor(new AnchorPane());
        BorderPane subContent = new BorderPane();
        BorderPane nameContent = new BorderPane();
        Rectangle playerBackground = new Rectangle();
        playerBackground.setStroke(combineColor(combineColor(player.color, Color.LIGHTGRAY), Color.LIGHTGRAY)); playerBackground.setStrokeWidth(5); playerBackground.setFill(combineColor(player.color, Color.LIGHTGRAY));
        playerBackground.setArcWidth(30); playerBackground.setArcHeight(30);
        playerBackground.widthProperty().bind(playerAnchor.widthProperty());
        playerBackground.heightProperty().bind(playerAnchor.heightProperty());
        playerContainer.getChildren().add(playerBackground);
        Rectangle countdownOverlay = new Rectangle();
        countdownOverlay.setFill(new Color(0, 0, 0, 0.5));
        countdownOverlay.setArcWidth(30); countdownOverlay.setArcHeight(30);
        countdownOverlay.widthProperty().bind(playerAnchor.widthProperty());
        countdownOverlay.heightProperty().bind(playerAnchor.heightProperty());
        BorderPane countdownContent = new BorderPane();
        StackPane sub = new StackPane();
        sub.getChildren().add(playerAnchor);
        sub.getChildren().add(subContent);
        sub.getChildren().add(nameContent);
        sub.getChildren().add(countdownOverlay);
        sub.getChildren().add(countdownContent);
        playerContainer.setCenter(sub);

        Label playerName = new Label(player.name);
        playerName.setAlignment(Pos.CENTER_LEFT);
        playerName.setFont(NAME_FONT);
        playerName.setTextFill(combineColor(player.color, Color.BLACK));
        BorderPane.setMargin(playerName, new Insets(15, 15, 15, 15));
        nameContent.setTop(playerName);

        playerText = new TextGroup(content, playerAnchor.heightProperty(), player.color);
        subContent.setCenter(playerText.flow);

        Text countdownText = new Text();
        countdownText.setText("Game starts in\n3");
        countdownText.setTextAlignment(TextAlignment.CENTER);
        countdownText.setFill(Color.WHITE);
        countdownText.setFont(FONT);
        countdownContent.setCenter(countdownText);

        // OPPONENT
        BorderPane opponentContainer = new BorderPane();
        AnchorPane opponentAnchor = maxAnchor(new AnchorPane());
        BorderPane subContent2 = new BorderPane();
        BorderPane nameContent2 = new BorderPane();
        Rectangle opponentBackground = new Rectangle();
        opponentBackground.setStroke(combineColor(combineColor(opponent.color, Color.LIGHTGRAY), Color.LIGHTGRAY)); opponentBackground.setStrokeWidth(5); opponentBackground.setFill(combineColor(opponent.color, Color.LIGHTGRAY));
        opponentBackground.setArcWidth(30); opponentBackground.setArcHeight(30);
        opponentBackground.widthProperty().bind(opponentAnchor.widthProperty());
        opponentBackground.heightProperty().bind(opponentAnchor.heightProperty());
        opponentContainer.getChildren().add(opponentBackground);
        Rectangle countdownOverlay2 = new Rectangle();
        countdownOverlay2.setFill(new Color(0, 0, 0, 0.5));
        countdownOverlay2.setArcWidth(30); countdownOverlay2.setArcHeight(30);
        countdownOverlay2.widthProperty().bind(opponentAnchor.widthProperty());
        countdownOverlay2.heightProperty().bind(opponentAnchor.heightProperty());
        BorderPane countdownContent2 = new BorderPane();
        StackPane sub2 = new StackPane();
        sub2.getChildren().add(opponentAnchor);
        sub2.getChildren().add(subContent2);
        sub2.getChildren().add(nameContent2);
        sub2.getChildren().add(countdownOverlay2);
        sub2.getChildren().add(countdownContent2);
        opponentContainer.setCenter(sub2);

        Label opponentName = new Label(opponent.name);
        opponentName.setAlignment(Pos.CENTER_LEFT);
        opponentName.setFont(NAME_FONT);
        opponentName.setTextFill(combineColor(opponent.color, Color.BLACK));
        BorderPane.setMargin(opponentName, new Insets(15, 15, 15, 15));
        nameContent2.setTop(opponentName);

        opponentText = new TextGroup(content, playerAnchor.heightProperty(), opponent.color);
        subContent2.setCenter(opponentText.flow);

        Text countdownText2 = new Text();
        countdownText2.setText("Game starts in\n3");
        countdownText2.setTextAlignment(TextAlignment.CENTER);
        countdownText2.setFill(Color.WHITE);
        countdownText2.setFont(FONT);
        countdownContent2.setCenter(countdownText2);

        // COMBINER
        VBox container = new VBox();
        VBox.setVgrow(playerContainer, Priority.ALWAYS);
        VBox.setVgrow(opponentContainer, Priority.ALWAYS);
        VBox.setMargin(playerContainer, new Insets(15, 15, 15, 15));
        VBox.setMargin(opponentContainer, new Insets(5, 15, 15, 15));
        container.getChildren().addAll(opponentContainer, playerContainer);

        BorderPane main = new BorderPane();
        timeLeft.setText("Time Left: 15");
        timeLeft.setFont(TEXT_FONT);
        timeLeft.setAlignment(Pos.CENTER);
        timeLeft.prefWidthProperty().bind(main.widthProperty());
        main.setTop(timeLeft);
        BorderPane.setMargin(timeLeft, new Insets(5, 5, 5, 5));
        main.setCenter(container);

        gameStarted = false;

        new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                String text = "Game starts in\n" + (3 - i);
                countdownText.setText(text);
                countdownText2.setText(text);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            countdownText.setVisible(false);
            countdownText2.setVisible(false);
            countdownOverlay.setVisible(false);
            countdownOverlay2.setVisible(false);

            gameStarted = true;
        }).start();

        Scene game = new Scene(main, width, height);

        game.setOnKeyTyped(event -> {
            if (!gameStarted) return;

            String ch = event.getCharacter();
            boolean wordDone = playerText.type(ch);
            connection.sendPacket(new TypingPacket(ch));

            if (wordDone) {
                connection.sendPacket(new WordPacket());
            }
        });

        return game;
    }

    private Scene GameOverScene(double width, double height) {
        activeScene = SceneName.GAMEOVER;

        Label title = new Label();
        title.setAlignment(Pos.CENTER);
        title.setText(result == Result.WIN ? "You Win!" : result == Result.LOSE ? "You Lose!" : "Tie!");
        title.setFont(TITLE_FONT);

        Label info = new Label();
        info.setAlignment(Pos.CENTER);
        info.setTextAlignment(TextAlignment.CENTER);
        info.setText("You typed " + playerWPM + " WPM\nYour opponent typed " + opponentWPM + " WPM");
        info.setFont(TEXT_FONT);

        Button replayButton = new Button();
        replayButton.setText("Play Again");
        replayButton.setFont(TEXT_FONT);
        replayButton.setFocusTraversable(false);
        replayButton.setOnAction(event -> {
            disconnect(DisconnectionReason.USER);
            connection = null;

            Platform.runLater(() -> {
                information.setText("");
                setScene(MenuScene(stage.getScene().getWidth(), stage.getScene().getHeight()));
            });
        });

        VBox center = new VBox();
        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(title, info, replayButton);
        VBox.setMargin(replayButton, new Insets(5, 5, 5, 5));

        BorderPane main = new BorderPane();
        main.setCenter(center);

        return new Scene(main, width, height);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        int width = 600, height = 400;

        stage.setTitle("Type Racer Client");
        stage.setScene(MenuScene(width, height));
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        stage.show();
    }

    public void setScene(Scene scene) {
        Platform.runLater(() -> stage.setScene(scene));
    }

    public AnchorPane maxAnchor(AnchorPane anchor) {
        AnchorPane.setTopAnchor(anchor, 0.0);
        AnchorPane.setBottomAnchor(anchor, 0.0);
        AnchorPane.setLeftAnchor(anchor, 0.0);
        AnchorPane.setRightAnchor(anchor, 0.0);
        return anchor;
    }

    public static Color combineColor(Color a, Color b) {
        return new Color((a.getRed() + b.getRed()) / 2.0, (a.getGreen() + b.getGreen()) / 2.0, (a.getBlue() + b.getBlue()) / 2.0, 1);
    }

    public void listenForPackets() {
        new Thread(() -> {
            while (connection.isConnected()) {
                try {
                    Packet raw = connection.readPacket();

                    if (raw instanceof SignalPacket) continue;

                    handlePacket(raw);
                } catch (IOException | ClassNotFoundException e) {
                    disconnect(DisconnectionReason.ERROR);
                    break;
                }
            }

            stop();
        }).start();
    }

    public void handlePacket(Packet raw) {
        if (raw instanceof TypingPacket packet) {
            if (activeScene == SceneName.GAME) {
                Platform.runLater(() -> opponentText.type(packet.getContent()));
            }
        } if (raw instanceof HeartbeatPacket) {
            connection.sendPacket(new HeartbeatPacket());
        } else if (raw instanceof TimeLeftPacket packet) {
            if (activeScene == SceneName.GAME) {
                Platform.runLater(() -> timeLeft.setText("Time Left: " + packet.getTimeLeft()));
            }
        } else if (raw instanceof StartPacket packet) {
            if (activeScene == SceneName.WAITING) {
                content = packet.getContent();
                opponent = new PlayerInformation(packet.getName(), packet.getColor());
                setScene(GameScene(stage.getScene().getWidth(), stage.getScene().getHeight()));
            }
        } else if (raw instanceof GameOverPacket packet) {
            if (activeScene == SceneName.GAME) {
                result = packet.getResult();
                playerWPM = packet.getPlayerWPM();
                opponentWPM = packet.getOpponentWPM();
                setScene(GameOverScene(stage.getScene().getWidth(), stage.getScene().getHeight()));
            }
        }
        else if (raw instanceof DisconnectPacket) {
            disconnect(DisconnectionReason.REMOTE);

            if (activeScene != SceneName.GAMEOVER) {
                Platform.runLater(() -> {
                    information.setTextFill(Color.MEDIUMVIOLETRED);
                    information.setText("Disconnected from server");
                    setScene(MenuScene(stage.getScene().getWidth(), stage.getScene().getHeight()));
                });
            }
        }
    }

    @Override
    public void stop() {
        disconnect(DisconnectionReason.UNKNOWN);
    }

    @FXML
    public void exitApplication(ActionEvent event) {
        disconnect(DisconnectionReason.USER);
        Platform.exit();
    }

    public void disconnect(DisconnectionReason reason) {
        if (connection != null) connection.disconnect(reason);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
