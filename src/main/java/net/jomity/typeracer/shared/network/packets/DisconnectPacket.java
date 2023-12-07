package net.jomity.typeracer.shared.network.packets;

import net.jomity.typeracer.shared.constants.PacketType;

import java.io.Serial;

public class DisconnectPacket extends Packet {
    @Serial
    private static final long serialVersionUID = 5085677657762661045L;

    @Override
    public PacketType getType() {
        return PacketType.DISCONNECT;
    }
}