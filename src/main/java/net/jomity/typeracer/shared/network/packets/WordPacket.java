package net.jomity.typeracer.shared.network.packets;

import net.jomity.typeracer.shared.constants.PacketType;

import java.io.Serial;

public class WordPacket extends Packet {

    @Serial
    private static final long serialVersionUID = 2016529976808453372L;

    @Override
    public PacketType getType() {
        return PacketType.WORD;
    }
}
