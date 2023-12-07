package net.jomity.typeracer.shared.network.packets;

import javafx.scene.paint.Color;
import net.jomity.typeracer.shared.constants.PacketType;
import net.jomity.typeracer.shared.constants.PlayerInformation;

import java.io.Serial;

public class StartPacket extends Packet {
    @Serial
    private static final long serialVersionUID = 4766630151662633442L;

    private final String name;
    private final double red;
    private final double green;
    private final double blue;

    public StartPacket(PlayerInformation info) {
        this.name = info.name;
        this.red = info.color.getRed();
        this.green = info.color.getGreen();
        this.blue = info.color.getBlue();
    }

    @Override
    public PacketType getType() {
        return PacketType.START ;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return new Color(red, green, blue, 1);
    }
}