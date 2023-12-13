package net.jomity.typeracer.shared.network.packets;

import net.jomity.typeracer.shared.constants.PacketType;

public class WordPacket extends Packet {
    private static final long serialVersionUID = 2016529976808453372L;

    @Override
    public PacketType getType() {
        return PacketType.WORD;
    }
}
