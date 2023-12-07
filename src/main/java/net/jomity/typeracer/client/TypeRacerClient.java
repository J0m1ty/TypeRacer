package net.jomity.typeracer.typeracerproject.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TypeRacerClient extends Application {
    boolean connecting = false;
    boolean connected = false;

    Socket socket = null;
    ObjectOutputStream toServer = null;
    ObjectInputStream fromServer = null;

    Label information = new Label();
    Button playButton = new Button();

    private Scene MenuScene() {
        Label title = new Label();
        title.setAlignment(Pos.CENTER);
        title.setText("TYPE RACER");
        title.setFont(new Font(30));

        playButton.setText("Play Game");

        VBox center = new VBox();
        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(title, playButton);

        information.setAlignment(Pos.CENTER);

        VBox info = new VBox();
        info.setAlignment(Pos.CENTER);
        info.getChildren().add(information);

        BorderPane main = new BorderPane();
        BorderPane.setMargin(info, new Insets(10, 10, 10, 10));
        main.setCenter(center);
        main.setBottom(info);


        return new Scene(main, 600, 400);
    }

    private Scene WaitingScene(double width, double height) {
        BorderPane main = new BorderPane();

        return new Scene(main, width, height);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Type Racer Client");
        stage.setScene(MenuScene());
        stage.show();

        playButton.setOnAction(e -> {
            if (connecting) return;

            connecting = true;

            playButton.setDisable(true);
            information.setTextFill(Color.GRAY);
            information.setText("Connecting to the server...");

            new Thread(() -> {
                try {
                    socket = new Socket("localhost", 3001);
                    toServer = new ObjectOutputStream(socket.getOutputStream());
                    fromServer = new ObjectInputStream(socket.getInputStream());
                    connected = true;
                } catch (IOException ignored) {}

                if (!connected) {
                    connecting = false;

                    Platform.runLater(() -> {
                        playButton.setDisable(false);
                        information.setTextFill(Color.MEDIUMVIOLETRED);
                        information.setText("Connection failed. Try again later.");
                    });
                    return;
                }

                Platform.runLater(() -> {
                    stage.setScene(WaitingScene(stage.getScene().getWidth(), stage.getScene().getHeight()));
                });
            }).start();
        });
    }

    @Override
    public void stop() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    @FXML
    public void exitApplication(ActionEvent event) {
        Platform.exit();
    }
}
