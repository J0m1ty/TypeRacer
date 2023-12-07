package net.jomity.typeracer.shared;

import java.io.Serial;

public class StartPacket extends Packet {
    @Serial
    private static final long serialVersionUID = 4766630151662633442L;

    @Override
    public PacketType getType() {
        return PacketType.START;
    }
}