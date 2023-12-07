package net.jomity.typeracer.shared;

import javafx.scene.paint.Color;

import java.io.Serial;

public class RegisterPacket extends Packet {
    @Serial
    private static final long serialVersionUID = -7430141599644222468L;

    private final String name;
    private final double red;
    private final double green;
    private final double blue;

    public RegisterPacket(String name, Color color) {
        this.name = name;
        this.red = color.getRed();
        this.green = color.getGreen();
        this.blue = color.getBlue();
    }

    @Override
    public PacketType getType() {
        return PacketType.REGISTER;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return new Color(red, green, blue, 1);
    }
}
