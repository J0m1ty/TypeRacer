package net.jomity.typeracer.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import net.jomity.typeracer.server.client.Client;
import net.jomity.typeracer.server.client.ClientPacket;
import net.jomity.typeracer.shared.constants.Result;
import net.jomity.typeracer.shared.constants.DisconnectionReason;
import net.jomity.typeracer.shared.network.packets.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public class TypeRacerServer extends Application {
    public static final long TIMEOUT = 100;

    private ServerSocket serverSocket;

    private final ConcurrentMap<Thread, Game> activeGames = new ConcurrentHashMap<>();

    private final TextArea console = new TextArea();

    private Client waiting = null;

    public int nClients = 0;

    public void log(String text) {
        Platform.runLater(() -> console.appendText(text + "\n"));
    }

    @Override
    public void start(Stage stage) {
        ScrollPane scroll = new ScrollPane();
        scroll.setContent(console);
        console.setWrapText(true);
        scroll.setFitToWidth(true); scroll.setFitToHeight(true);

        Scene scene = new Scene(scroll, 600, 400);
        stage.setTitle("Type Racer Server");
        stage.setScene(scene);
        stage.show();

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(3001);

                log("Server started at " + new Date());

                while (true) {
                    Socket socket = serverSocket.accept();

                    Client client = new Client(++nClients, socket);
                    client.listenForPackets();
                    client.monitorConnection(TIMEOUT);

                    log("Client #" + nClients + " connected with hostname " + socket.getInetAddress().getHostAddress());

                    if (waiting == null || !waiting.isConnected()) {
                        log("Client #" + client.id + " is waiting for another player to join");
                        waiting = client;
                    } else {
                        Game gameInstance = new Game(waiting, client);
                        Thread gameThread = new Thread(gameInstance);
                        gameThread.start();

                        activeGames.put(gameThread, gameInstance);

                        waiting = null;
                    }
                }
            } catch (IOException e) {
                Platform.exit();
            }
        }).start();
    }

    class Game implements Runnable {
        private volatile boolean gameRunning = true;

        public Client player1;
        public Client player2;
        public boolean initialized = false;

        private final BlockingQueue<ClientPacket> packetQueue;

        public Game(Client player1, Client player2) {
            this.player1 = player1;
            this.player2 = player2;

            player1.registerOpponent(player2);
            player2.registerOpponent(player1);

            this.packetQueue = new LinkedBlockingQueue<>();

            player1.registerQueue(packetQueue);
            player2.registerQueue(packetQueue);
        }

        @Override
        public void run() {
            log("Game starting between client #" + player1.id + " and client # " + player2.id);

            while (gameRunning) {
                try {
                    ClientPacket clientPacket = packetQueue.take();
                    Packet raw = clientPacket.packet;
                    Client client = clientPacket.sourceClient;

                    if (!gameRunning) {
                        log("Game is closing");
                        break;
                    }

                    if (!player1.isConnected()) {
                        log("Client #" + player1.id + " disconnected");
                        player2.sendPacket(new GameOverPacket(Result.WIN));
                        break;
                    }

                    if (!player2.isConnected()) {
                        log("Client #" + player2.id + " disconnected");
                        player1.sendPacket(new GameOverPacket(Result.WIN));
                        break;
                    }

                    if (raw instanceof SignalPacket) continue;

                    if (!initialized) {
                        if (player1.initialized && player2.initialized) {
                            player1.sendPacket(new StartPacket(player1.information));
                            player2.sendPacket(new StartPacket(player2.information));
                            initialized = true;
                        }
                        else {
                            continue;
                        }
                    }

                    System.out.println("Unhandled packet: " + raw.getType() + " from client #" + client.id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            stop();
        }

        public void stop() {
            log("Game ended");

            gameRunning = false;

            player1.disconnect(DisconnectionReason.UNKNOWN);
            player2.disconnect(DisconnectionReason.UNKNOWN);
        }
    }

    @Override
    public void stop() {
        activeGames.forEach((thread, game) -> {
            game.stop();
            thread.interrupt();
        });

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
    }

    @FXML
    public void exitApplication(ActionEvent ignoredEvent) {
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

