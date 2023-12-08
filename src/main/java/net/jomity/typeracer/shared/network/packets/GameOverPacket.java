package net.jomity.typeracer.shared.network.packets;

import net.jomity.typeracer.shared.constants.PacketType;
import net.jomity.typeracer.shared.constants.Result;
import net.jomity.typeracer.shared.constants.ResultType;

import java.io.Serial;

public class GameOverPacket extends Packet {
    @Serial
    private static final long serialVersionUID = -6871750598817118281L;

    private final Result result;
    private final ResultType resultType;
    private final double playerWPM;
    private final double opponentWPM;

    public GameOverPacket(Result result, ResultType resultType, double playerWPM, double opponentWPM) {
        this.result = result;
        this.resultType = resultType;
        this.playerWPM = playerWPM;
        this.opponentWPM = opponentWPM;
    }

    @Override
    public PacketType getType() {
        return PacketType.GAMEOVER;
    }

    public Result getResult() {
        return result;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public double getPlayerWPM() {
        return playerWPM;
    }

    public double getOpponentWPM() {
        return opponentWPM;
    }
}
