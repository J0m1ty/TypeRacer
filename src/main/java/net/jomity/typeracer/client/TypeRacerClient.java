package net.jomity.typeracer.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.FontFace;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.jomity.typeracer.shared.constants.PlayerInformation;
import net.jomity.typeracer.shared.constants.Result;
import net.jomity.typeracer.shared.network.Connection;
import net.jomity.typeracer.shared.constants.DisconnectionReason;
import net.jomity.typeracer.shared.network.packets.*;

import java.io.*;
import java.net.Socket;
import java.time.format.TextStyle;

enum SceneName {
    MENU, WAITING, GAME, GAMEOVER
}

public class TypeRacerClient extends Application {
    Connection connection;
    public Result result;
    public PlayerInformation player;
    public PlayerInformation opponent;

    SceneName activeScene;

    Stage stage;
    Label information = new Label();

    private Scene MenuScene(double width, double height) {
        activeScene = SceneName.MENU;

        Label title = new Label();
        title.setAlignment(Pos.CENTER);
        title.setText("TYPE RACER");
        title.setFont(new Font(30));

        TextField serverField = new TextField();
        serverField.setFocusTraversable(false);
        serverField.setText("localhost");
        serverField.setPrefWidth(100); serverField.setMaxWidth(100);

        Button playButton = new Button();
        playButton.setText("Play Game");

        VBox center = new VBox();
        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(title, serverField, playButton);
        VBox.setMargin(playButton, new Insets(5, 5, 5, 5));

        Label description = new Label();
        description.setText("Your Information:");

        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setFocusTraversable(false);

        TextField nameField = new TextField();
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
        title.setFont(new Font(30));

        Button cancelButton = new Button();
        cancelButton.setText("Cancel search");
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
        center.getChildren().addAll(title, cancelButton);

        BorderPane main = new BorderPane();
        main.setCenter(center);

        return new Scene(main, width, height);
    }

    private Scene GameScene(double width, double height) {
        activeScene = SceneName.GAME;

        BorderPane playerContainer = new BorderPane();
        AnchorPane playerAnchor = maxAnchor(new AnchorPane());
        Rectangle playerBackground = new Rectangle();
        playerBackground.setStroke(combineColor(combineColor(player.color, Color.LIGHTGRAY), Color.LIGHTGRAY)); playerBackground.setStrokeWidth(5); playerBackground.setFill(combineColor(player.color, Color.LIGHTGRAY));
        playerBackground.setArcWidth(30); playerBackground.setArcHeight(30);
        playerBackground.widthProperty().bind(playerAnchor.widthProperty());
        playerBackground.heightProperty().bind(playerAnchor.heightProperty());
        playerContainer.setCenter(playerAnchor);
        playerContainer.getChildren().add(playerBackground);

        Text playerText = new Text();
        playerText.setFont(new Font("Monospaced", 30));
        playerText.setText("THIS IS A TEST OF THE PUBLIC BROADCASTING SERVICE. TEST WILL CONCLUDE NOW. THANK YOU.");
        playerText.setTextAlignment(TextAlignment.CENTER);
        playerText.wrappingWidthProperty().bind(playerAnchor.widthProperty());
        playerText.yProperty().bind(playerAnchor.heightProperty());
        playerContainer.getChildren().add(playerText);

        BorderPane opponentContainer = new BorderPane();
        AnchorPane opponentAnchor = maxAnchor(new AnchorPane());
        Rectangle opponentBackground = new Rectangle();
        opponentBackground.setStroke(combineColor(combineColor(opponent.color, Color.LIGHTGRAY), Color.LIGHTGRAY)); opponentBackground.setStrokeWidth(5); opponentBackground.setFill(combineColor(opponent.color, Color.LIGHTGRAY));
        opponentBackground.setArcWidth(30); opponentBackground.setArcHeight(30);
        opponentBackground.widthProperty().bind(opponentAnchor.widthProperty());
        opponentBackground.heightProperty().bind(opponentAnchor.heightProperty());
        opponentContainer.setCenter(opponentAnchor);
        opponentContainer.getChildren().add(opponentBackground);


        VBox container = new VBox();
        VBox.setVgrow(playerContainer, Priority.ALWAYS);
        VBox.setVgrow(opponentContainer, Priority.ALWAYS);
        VBox.setMargin(playerContainer, new Insets(10, 10, 10, 10));
        VBox.setMargin(opponentContainer, new Insets(10, 10, 10, 10));
        container.getChildren().addAll(opponentContainer, playerContainer);

        BorderPane main = new BorderPane();
        main.setCenter(container);
        BorderPane.setMargin(container, new Insets(10, 10, 10, 10));

        return new Scene(main, width, height);
    }

    private Scene GameOverScene(double width, double height) {
        activeScene = SceneName.GAMEOVER;

        Label title = new Label();
        title.setAlignment(Pos.CENTER);
        title.setText("GameOver Scene");
        title.setFont(new Font(30));

        Button replayButton = new Button();
        replayButton.setText("Play Again");
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
        center.getChildren().addAll(title, replayButton);

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

    public Color combineColor(Color a, Color b) {
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
        if (raw instanceof HeartbeatPacket) {
            connection.sendPacket(new HeartbeatPacket());
        } else if (raw instanceof StartPacket packet) {
            if (activeScene == SceneName.WAITING) {
                opponent = new PlayerInformation(packet.getName(), packet.getColor());
                setScene(GameScene(stage.getScene().getWidth(), stage.getScene().getHeight()));
            }
        } else if (raw instanceof GameOverPacket packet) {
            if (activeScene == SceneName.GAME) {
                result = packet.getResult();
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
