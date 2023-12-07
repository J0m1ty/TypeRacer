package net.jomity.typeracer.typeracerproject.server.client;

import net.jomity.typeracer.typeracerproject.shared.Packet;

public class ClientPacket {
    public final Packet packet;
    public final Client sourceClient;

    public ClientPacket(Packet packet, Client sourceClient) {
        this.packet = packet;
        this.sourceClient = sourceClient;
    }
}
