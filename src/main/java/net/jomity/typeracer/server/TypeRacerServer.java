package net.jomity.typeracer.typeracerproject.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import net.jomity.typeracer.typeracerproject.server.client.Client;
import net.jomity.typeracer.typeracerproject.server.client.ClientPacket;
import net.jomity.typeracer.typeracerproject.shared.HeartbeatPacket;
import net.jomity.typeracer.typeracerproject.shared.Packet;
import net.jomity.typeracer.typeracerproject.shared.SignalPacket;
import net.jomity.typeracer.typeracerproject.shared.StartPacket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public class TypeRacerServer extends Application {
    public static final int TIMEOUT = 5000;

    private ServerSocket serverSocket;

    private final ConcurrentMap<Thread, Game> activeGames = new ConcurrentHashMap<>();

    private final TextArea console = new TextArea();

    private Client waiting = null;

    public int nClients = 0;

    public void log(String text) {
        Platform.runLater(() -> {
            console.appendText(text + "\n");
        });
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

                    log("Client #" + nClients + " connected with hostname " + socket.getInetAddress().getHostAddress());

                    if (waiting == null) {
                        log("Client #" + client.id + " is waiting for another player to join");
                        waiting = client;
                    } else {
                        Game gameInstance = new Game(waiting, client);
                        Thread gameThread = new Thread(gameInstance);
                        gameThread.start();

                        activeGames.put(gameThread, gameInstance);
                    }
                }
            } catch (IOException e) {
                Platform.exit();
            }
        }).start();
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
    public void exitApplication(ActionEvent event) {
        Platform.exit();
    }

    class Game implements Runnable {
        private volatile boolean gameRunning = true;

        public Client player1;
        public Client player2;

        private final BlockingQueue<ClientPacket> packetQueue;

        private Thread monitor1;
        private Thread monitor2;

        public Game(Client player1, Client player2) {
            this.player1 = player1;
            this.player2 = player2;

            this.packetQueue = new LinkedBlockingQueue<>();

            monitor1 = new Thread(() -> monitorConnection(player1));
            monitor2 = new Thread(() -> monitorConnection(player2));
        }

        @Override
        public void run() {
            log("Match created between player #" + player1.id + " and " + player2.id);

            player1.sendPacket(new StartPacket());
            player2.sendPacket(new StartPacket());

            monitor1.start();
            monitor2.start();

            player1.listenForPackets(packetQueue);
            player2.listenForPackets(packetQueue);

            while (gameRunning) {
                try {
                    ClientPacket clientPacket = packetQueue.take();

                    if (clientPacket.packet instanceof SignalPacket) {
                        if (!gameRunning) {
                            log("Game is closing");
                            break;
                        }
                        if (player1.disconnected) {
                            log("Client #" + player1.id + " disconnected");
                            break;
                        }
                        if (player2.disconnected) {
                            log("Client #" + player2.id + " disconnected");
                            break;
                        }
                        continue;
                    }

                    handlePacket(clientPacket.packet, clientPacket.sourceClient);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            stop();
        }

        private void handlePacket(Packet packet, Client client) {
            switch (packet.getType()) {
                case HEARTBEAT:
                    log("Received heartbeat from client #" + client.id);
                    client.setHeartbeatReceived(true);
                    break;
            }
        }

        private void monitorConnection(Client client) {
            while (gameRunning && !client.disconnected) {
                log("Sending heartbeat request to client #" + client.id);
                client.sendPacket(new HeartbeatPacket());
                client.setHeartbeatReceived(false);

                try {
                    Thread.sleep(TIMEOUT);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (!client.hasRespondedToHeartbeat()) {
                    client.disconnected = true;
                    try {
                        packetQueue.put(new ClientPacket(new SignalPacket(), client));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        public void stop() {
            log("Game ended");

            gameRunning = false;
            monitor1.interrupt();
            monitor2.interrupt();

            try {
                player1.socket.close();
            } catch (IOException ignored) {}

            try {
                player2.socket.close();
            } catch (IOException ignored) {}

            try {
                packetQueue.put(new ClientPacket(new SignalPacket(), null));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

