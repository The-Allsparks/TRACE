package org.allsparks.trace.core;

/**
 * Record priority used for backpressure. Critical safety events and
 * replay-essential inputs outrank verbose diagnostics.
 */
public enum TracePriority {
    CRITICAL(0),
    HIGH(1),
    NORMAL(2),
    DEBUG(3),
    VERBOSE(4);

    private final int rank;

    TracePriority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public byte wireId() {
        return (byte) rank;
    }

    public boolean outranks(TracePriority other) {
        return this.rank < other.rank;
    }

    public static TracePriority fromWire(byte id) {
        for (TracePriority value : values()) {
            if (value.rank == id) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown priority id: " + id);
    }
}
