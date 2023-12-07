package net.jomity.typeracer.shared;

import java.io.Serial;

public class HeartbeatPacket extends Packet {
    @Serial
    private static final long serialVersionUID = -4259559440789762007L;

    @Override
    public PacketType getType() {
        return PacketType.HEARTBEAT;
    }
}