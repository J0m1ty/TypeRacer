package net.jomity.typeracer.server.client;

import net.jomity.typeracer.shared.network.packets.Packet;

public class ClientPacket {
    public final Packet packet;
    public final Client sourceClient;

    public ClientPacket(Packet packet, Client sourceClient) {
        this.packet = packet;
        this.sourceClient = sourceClient;
    }
}
