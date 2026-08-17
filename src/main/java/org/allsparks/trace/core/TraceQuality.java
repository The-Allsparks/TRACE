package org.allsparks.trace.core;

/**
 * Freshness and trustworthiness of a recorded value.
 */
public enum TraceQuality {
    OK((byte) 0),
    STALE((byte) 1),
    ESTIMATED((byte) 2),
    INVALID((byte) 3),
    MISSING((byte) 4),
    ASYNC((byte) 5);

    private final byte wireId;

    TraceQuality(byte wireId) {
        this.wireId = wireId;
    }

    public byte wireId() {
        return wireId;
    }

    public static TraceQuality fromWire(byte id) {
        for (TraceQuality value : values()) {
            if (value.wireId == id) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown quality id: " + id);
    }
}
