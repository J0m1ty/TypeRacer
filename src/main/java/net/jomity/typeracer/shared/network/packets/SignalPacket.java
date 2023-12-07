package net.jomity.typeracer.shared;

import java.io.Serial;

public class SignalPacket extends Packet {
    @Serial
    private static final long serialVersionUID = 6549773769689986859L;

    @Override
    public PacketType getType() {
        return PacketType.SIGNAL;
    }
}