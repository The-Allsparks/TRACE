package org.allsparks.trace;

/**
 * Student-facing implementation phases. Later phases remain behind explicit
 * approval gates and feature flags.
 */
public enum TracePhase {
    PHASE_0_FOUNDATION,
    PHASE_1_EVENT_RECORDER,
    PHASE_2_ESSENTIAL_TELEMETRY,
    PHASE_3_FLIGHT_RECORDER,
    PHASE_4_ADAPTERS,
    PHASE_5_ADVANTAGESCOPE,
    PHASE_6_IO_BOUNDARIES,
    PHASE_7_REPLAY,
    PHASE_8_REGRESSION
}
