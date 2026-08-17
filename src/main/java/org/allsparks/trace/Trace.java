package org.allsparks.trace;

import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceCycle;
import org.allsparks.trace.session.TraceHealth;
import org.allsparks.trace.session.TraceSession;

/**
 * Student-facing TRACE facade. Observational only in Phases 0–3: these methods
 * never command motors, servos, or mechanism states.
 *
 * <pre>{@code
 * Trace.configure(TraceConfig.builder().mode(TraceMode.EVENTS).memorySink(true).build());
 * Trace.event("Autonomous started");
 * Trace.record("Battery/Voltage", voltage, Units.VOLTS);
 * Trace.record("Drive/Pose", pose);
 * }</pre>
 */
public final class Trace {
    private static final Object LOCK = new Object();
    private static volatile TraceSession session = new TraceSession(TraceConfig.off());

    private Trace() {}

    public static void configure(TraceConfig config) {
        synchronized (LOCK) {
            session.close();
            session = new TraceSession(config);
        }
    }

    public static TraceSession session() {
        return session;
    }

    public static void event(String message) {
        session.event(message);
    }

    public static void record(String name, double value) {
        session.record(name, value, Units.NONE);
    }

    public static void record(String name, double value, Units units) {
        session.record(name, value, units);
    }

    public static void record(String name, Pose2d pose) {
        session.record(name, pose);
    }

    public static void recordInput(String name, double value, Units units) {
        session.recordInput(name, value, units);
    }

    public static TraceCycle beginCycle() {
        return session.beginCycle();
    }

    public static TraceHealth health() {
        return session.health();
    }

    public static void stop() {
        session.close();
    }

    public static void resetForTests(TraceConfig config) {
        configure(config);
    }
}
