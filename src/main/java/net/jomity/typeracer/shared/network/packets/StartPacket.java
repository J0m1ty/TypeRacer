package net.jomity.typeracer.shared.network.packets;

import javafx.scene.paint.Color;
import net.jomity.typeracer.shared.constants.PacketType;
import net.jomity.typeracer.shared.constants.PlayerInformation;

public class StartPacket extends Packet {
    private static final long serialVersionUID = 4766630151662633442L;

    private final String content;
    private final String name;
    private final double red;
    private final double green;
    private final double blue;

    public StartPacket(String content, PlayerInformation opponent) {
        this.content = content;
        this.name = opponent.name;
        this.red = opponent.color.getRed();
        this.green = opponent.color.getGreen();
        this.blue = opponent.color.getBlue();
    }

    @Override
    public PacketType getType() {
        return PacketType.START ;
    }

    public String getContent() { return content; }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return new Color(red, green, blue, 1);
    }
}