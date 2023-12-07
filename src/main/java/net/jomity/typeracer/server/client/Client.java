package net.jomity.typeracer.typeracerproject.server.client;
import net.jomity.typeracer.typeracerproject.server.TypeRacerServer;
import net.jomity.typeracer.typeracerproject.shared.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

class HeartbeatMonitor {
    private volatile boolean heartbeatReceived = false;
    private long lastHeartbeat = System.currentTimeMillis();

    public synchronized void setHeartbeatReceived(boolean received) {
        heartbeatReceived = received;
        lastHeartbeat = System.currentTimeMillis();
    }

    public synchronized boolean hasRespondedToHeartbeat() {
        return heartbeatReceived && (System.currentTimeMillis() - lastHeartbeat <= TypeRacerServer.TIMEOUT);
    }
}

public class Client extends HeartbeatMonitor {
    public int id;
    public Socket socket;
    public ObjectOutputStream output;
    public ObjectInputStream input;
    public boolean disconnected = false;

    public Client(int id, Socket socket) {
        this.id = id;
        this.socket = socket;

        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            disconnected = true;
        }
    }

    public void sendPacket(Packet packet) {
        if (!disconnected) {
            try {
                output.writeObject(packet);
                output.flush();
            } catch (IOException e) {
                disconnected = true;
            }
        }
    }

    public void listenForPackets(BlockingQueue<ClientPacket> queue) {
        new Thread(() -> {
            while (!disconnected) {
                try {
                    Packet packet = (Packet) input.readObject();
                    queue.put(new ClientPacket(packet, this));
                } catch (IOException | ClassNotFoundException e) {
                    disconnected = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }).start();
    }
}