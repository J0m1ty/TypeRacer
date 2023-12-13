package net.jomity.typeracer.server.client;
import net.jomity.typeracer.shared.constants.PlayerInformation;
import net.jomity.typeracer.shared.network.Connection;
import net.jomity.typeracer.shared.constants.DisconnectionReason;
import net.jomity.typeracer.shared.network.HeartbeatMonitor;
import net.jomity.typeracer.shared.network.packets.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class Client extends Connection {
    public int id;

    public int wordsTyped = 0;

    private Client opponent;

    public PlayerInformation information;
    public boolean initialized = false;

    private final HeartbeatMonitor monitor = new HeartbeatMonitor();
    private BlockingQueue<ClientPacket> queue;

    private Thread listenerThread;
    private Thread monitorThread;

    public Client(int id, Socket socket) {
        super(socket);

        this.id = id;
    }

    public void registerOpponent(Client opponent) { this.opponent = opponent; }

    public Client getOpponent() {
        return opponent;
    }

    public void registerQueue(BlockingQueue<ClientPacket> queue) {
        this.queue = queue;
    }

    public void listenForPackets() {
        if (listenerThread != null) return;

        listenerThread = new Thread(() -> {
            while (isConnected()) {
                try {
                    Packet raw = readPacket();

                    if (raw instanceof HeartbeatPacket) {
                        monitor.setState(true);
                        continue;
                    }

                    if (raw instanceof DisconnectPacket) {
                        disconnect(DisconnectionReason.REMOTE);
                        continue;
                    }

                    if (raw instanceof RegisterPacket) {
                        RegisterPacket packet = (RegisterPacket) raw;
                        packet.validate();
                        information = new PlayerInformation(packet.getName(), packet.getColor());
                        initialized = true;
                    }

                    if (queue != null) queue.put(new ClientPacket(raw, this));
                } catch (IOException | ClassNotFoundException e) {
                    disconnect(DisconnectionReason.ERROR);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        listenerThread.start();
    }

    public void monitorConnection(long timeout) {
        if (monitorThread != null) return;

        monitorThread = new Thread(() -> {
            monitor.setState(false);
            sendPacket(new HeartbeatPacket());

            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (!monitor.hasResponded()) {
                disconnect(DisconnectionReason.ERROR);
            }
        });

        monitorThread.start();
    }

    @Override
    public void disconnect(DisconnectionReason reason) {
        super.disconnect(reason);

        try {
            if (queue != null) queue.put(new ClientPacket(new SignalPacket(), null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        listenerThread.interrupt();
        monitorThread.interrupt();
    }
}