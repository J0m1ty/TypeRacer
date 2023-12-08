package net.jomity.typeracer.shared.network;

import net.jomity.typeracer.shared.constants.Constants;

public class HeartbeatMonitor {
    private volatile boolean heartbeatReceived = false;
    private long lastHeartbeat = System.currentTimeMillis();

    public synchronized void setState(boolean received) {
        heartbeatReceived = received;
        if (!received) lastHeartbeat = System.currentTimeMillis();
    }

    public synchronized boolean hasResponded() {
        long response = (System.currentTimeMillis() - lastHeartbeat) - Constants.TIMEOUT;
        return heartbeatReceived && (response >= 0);
    }
}