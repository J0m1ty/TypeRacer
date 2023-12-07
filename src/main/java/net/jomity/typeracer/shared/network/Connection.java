package net.jomity.typeracer.shared.network;

import net.jomity.typeracer.shared.constants.DisconnectionReason;
import net.jomity.typeracer.shared.network.packets.DisconnectPacket;
import net.jomity.typeracer.shared.network.packets.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connection {
    private final Lock readLock = new ReentrantLock();
    private final Lock writeLock = new ReentrantLock();

    protected boolean connected = false;
    protected boolean disconnected = false;

    protected Socket socket;
    protected ObjectOutputStream output;
    protected ObjectInputStream input;

    public Connection(Socket socket) {
        this.socket = socket;

        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            disconnect(DisconnectionReason.ERROR);
        } finally {
            connected = true;
        }
    }

    public boolean isConnected() {
        return connected && !disconnected;
    }

    public void sendPacket(Packet packet) {
        if (isConnected()) {
            writeLock.lock();
            try {
                output.reset();
                output.writeObject(packet);
                output.flush();
            } catch (IOException e) {
                disconnected = true;
            } finally {
                writeLock.unlock();
            }
        }
    }

    public Packet readPacket() throws IOException, ClassNotFoundException {
        readLock.lock();
        try {
            return (Packet) input.readObject();
        } finally {
            readLock.unlock();
        }
    }

    public void disconnect(DisconnectionReason reason) {
        if (disconnected) return;

        sendPacket(new DisconnectPacket());

        disconnected = true;

        try {
           socket.close();
        } catch (IOException ignored) {}
    }
}