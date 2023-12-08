package net.jomity.typeracer.shared.network.packets;

import net.jomity.typeracer.shared.constants.PacketType;

import java.io.Serial;

public class TypingPacket extends Packet {
    @Serial
    private static final long serialVersionUID = -2209177355770023524L;

    private final char content;

    public TypingPacket(String content) {
        this.content = content.charAt(0);
    }

    @Override
    public PacketType getType() {
        return PacketType.TYPING;
    }

    public String getContent() {
        return String.valueOf(content);
    }
}
