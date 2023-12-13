package net.jomity.typeracer.shared.network.packets;

import net.jomity.typeracer.shared.constants.PacketType;

public class SignalPacket extends Packet {
    private static final long serialVersionUID = 6549773769689986859L;

    @Override
    public PacketType getType() {
        return PacketType.SIGNAL;
    }
}