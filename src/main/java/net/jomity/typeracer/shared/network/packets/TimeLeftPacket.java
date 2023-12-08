package net.jomity.typeracer.shared.network.packets;

import net.jomity.typeracer.shared.constants.PacketType;

import java.io.Serial;

public class TimeLeftPacket extends Packet {
    @Serial
    private static final long serialVersionUID = 659792258175725108L;

    private final int timeLeft;

    public TimeLeftPacket(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    @Override
    public PacketType getType() {
        return PacketType.TIMELEFT;
    }

    public int getTimeLeft() {
        return timeLeft;
    }
}
