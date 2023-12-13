package net.jomity.typeracer.shared.network.packets;

import net.jomity.typeracer.shared.constants.PacketType;

import java.io.Serializable;

public abstract class Packet implements Serializable {
    private static final long serialVersionUID = 3286645130074599007L;

    public abstract PacketType getType();
}