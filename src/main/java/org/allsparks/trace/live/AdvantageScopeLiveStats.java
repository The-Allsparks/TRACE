package org.allsparks.trace.live;

/**
 * Snapshot of live AdvantageScope publisher counters. Inactive when the optional
 * module is absent, streaming is off, or the listen socket failed to bind.
 */
public final class AdvantageScopeLiveStats {
    private final boolean enabled;
    private final boolean listening;
    private final int listenPort;
    private final int configuredRateHz;
    private final int clientCount;
    private final long framesPrepared;
    private final long framesSent;
    private final long framesDropped;
    private final long sendErrors;
    private final long valuesSent;
    private final long valuesSkipped;
    private final long bytesSent;
    private final long reconnectCount;
    private final long lastSendDurationMs;
    private final long maxSendDurationMs;
    private final long serializationDurationMs;

    public AdvantageScopeLiveStats(
            boolean enabled,
            boolean listening,
            int listenPort,
            int configuredRateHz,
            int clientCount,
            long framesPrepared,
            long framesSent,
            long framesDropped,
            long sendErrors,
            long valuesSent,
            long valuesSkipped,
            long bytesSent,
            long reconnectCount,
            long lastSendDurationMs,
            long maxSendDurationMs,
            long serializationDurationMs) {
        this.enabled = enabled;
        this.listening = listening;
        this.listenPort = listenPort;
        this.configuredRateHz = configuredRateHz;
        this.clientCount = clientCount;
        this.framesPrepared = framesPrepared;
        this.framesSent = framesSent;
        this.framesDropped = framesDropped;
        this.sendErrors = sendErrors;
        this.valuesSent = valuesSent;
        this.valuesSkipped = valuesSkipped;
        this.bytesSent = bytesSent;
        this.reconnectCount = reconnectCount;
        this.lastSendDurationMs = lastSendDurationMs;
        this.maxSendDurationMs = maxSendDurationMs;
        this.serializationDurationMs = serializationDurationMs;
    }

    public static AdvantageScopeLiveStats inactive() {
        return new AdvantageScopeLiveStats(
                false, false, -1, 0, 0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean listening() {
        return listening;
    }

    public int listenPort() {
        return listenPort;
    }

    public int configuredRateHz() {
        return configuredRateHz;
    }

    public int clientCount() {
        return clientCount;
    }

    public long framesPrepared() {
        return framesPrepared;
    }

    public long framesSent() {
        return framesSent;
    }

    public long framesDropped() {
        return framesDropped;
    }

    public long sendErrors() {
        return sendErrors;
    }

    public long valuesSent() {
        return valuesSent;
    }

    public long valuesSkipped() {
        return valuesSkipped;
    }

    public long bytesSent() {
        return bytesSent;
    }

    public long reconnectCount() {
        return reconnectCount;
    }

    public long lastSendDurationMs() {
        return lastSendDurationMs;
    }

    public long maxSendDurationMs() {
        return maxSendDurationMs;
    }

    public long serializationDurationMs() {
        return serializationDurationMs;
    }
}
