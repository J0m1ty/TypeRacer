package net.jomity.typeracer.shared.network.packets;

import net.jomity.typeracer.shared.constants.PacketType;
import net.jomity.typeracer.shared.constants.Result;

import java.io.Serial;

public class GameOverPacket extends Packet {
    @Serial
    private static final long serialVersionUID = -6871750598817118281L;

    private final Result result;

    public GameOverPacket(Result result) {
        this.result = result;
    }

    @Override
    public PacketType getType() {
        return PacketType.GAMEOVER;
    }

    public Result getResult() {
        return result;
    }
}
