package org.allsparks.trace.core;

/**
 * Event severity. Distinct from {@link TracePriority}: severity describes the
 * meaning of an event, while priority describes retention under backpressure.
 */
public enum TraceSeverity {
    INFO((byte) 0),
    NOTICE((byte) 1),
    WARNING((byte) 2),
    ERROR((byte) 3),
    FAULT((byte) 4);

    private final byte wireId;

    TraceSeverity(byte wireId) {
        this.wireId = wireId;
    }

    public byte wireId() {
        return wireId;
    }

    public static TraceSeverity fromWire(byte id) {
        for (TraceSeverity value : values()) {
            if (value.wireId == id) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown severity id: " + id);
    }
}
