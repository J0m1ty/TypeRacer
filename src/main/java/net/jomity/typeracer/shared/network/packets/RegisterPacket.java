package net.jomity.typeracer.shared.network.packets;

import javafx.scene.paint.Color;
import net.jomity.typeracer.shared.constants.PacketType;
import net.jomity.typeracer.shared.constants.PlayerInformation;

public class RegisterPacket extends Packet {
    private static final long serialVersionUID = -7430141599644222468L;

    private String name;
    private double red;
    private double green;
    private double blue;

    public RegisterPacket(PlayerInformation info) {
        this.name = info.name;
        this.red = info.color.getRed();
        this.green = info.color.getGreen();
        this.blue = info.color.getBlue();
    }

    public void validate() {
        name = name.trim().substring(0, Math.min(name.length(), 16)).replaceAll("[^a-zA-Z0-9\\s]", "");
        if (name.length() < 3) name = "Player";

        red = Math.max(0, Math.min(1, red));
        green = Math.max(0, Math.min(1, green));
        blue = Math.max(0, Math.min(1, blue));
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
