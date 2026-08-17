package org.allsparks.trace;

/**
 * Explicit recording intensity. Advanced modes never activate merely because an
 * integration library is on the classpath.
 */
public enum TraceMode {
    /** Negligible runtime work and no recording. */
    OFF,
    /** Lifecycle, warnings, faults, and semantic events. */
    EVENTS,
    /** Events plus a small configured set of operational signals. */
    ESSENTIAL,
    /** Replay-relevant inputs, outputs, metadata, and diagnostics. */
    FULL,
    /**
     * Recorded inputs replace hardware inputs; physical outputs are disabled.
     * Not implemented in Phases 0–3; selecting it fails closed.
     */
    REPLAY;

    public boolean recordsEvents() {
        return this == EVENTS || this == ESSENTIAL || this == FULL || this == REPLAY;
    }

    public boolean recordsSignals() {
        return this == ESSENTIAL || this == FULL || this == REPLAY;
    }

    public boolean recordsFullDetail() {
        return this == FULL || this == REPLAY;
    }
}
