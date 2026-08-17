package org.allsparks.trace.session;

import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceQuality;
import org.allsparks.trace.core.TraceSeverity;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;

/**
 * One control-cycle recording context. Closing the cycle does not write
 * hardware outputs; TRACE remains observational in Phases 0–3.
 */
public final class TraceCycle implements AutoCloseable {
    private final TraceSession session;
    private final long number;
    private final long startNanos;
    private boolean closed;

    TraceCycle(TraceSession session, long number, long startNanos) {
        this.session = session;
        this.number = number;
        this.startNanos = startNanos;
    }

    public long number() {
        return number;
    }

    public long startNanos() {
        return startNanos;
    }

    public void recordInput(String name, double value, Units units) {
        session.record(RecordCategory.INPUT, name, TypedValue.ofDouble(value), units, TracePriority.HIGH, TraceQuality.OK, "");
    }

    public void recordInput(String name, Pose2d pose) {
        session.record(RecordCategory.INPUT, name, TypedValue.ofPose(pose), Units.METERS, TracePriority.HIGH, TraceQuality.OK, "");
    }

    public void recordInput(String name, Object structured) {
        session.record(
                RecordCategory.INPUT,
                name,
                TypedValue.ofString(String.valueOf(structured)),
                Units.NONE,
                TracePriority.HIGH,
                TraceQuality.OK,
                "");
    }

    public void recordOutput(String name, double value, Units units) {
        session.record(RecordCategory.OUTPUT, name, TypedValue.ofDouble(value), units, TracePriority.NORMAL, TraceQuality.OK, "");
    }

    public void recordOutput(String name, Pose2d pose) {
        session.record(RecordCategory.OUTPUT, name, TypedValue.ofPose(pose), Units.METERS, TracePriority.NORMAL, TraceQuality.OK, "");
    }

    public void recordOutput(String name, Object structured) {
        session.record(
                RecordCategory.OUTPUT,
                name,
                TypedValue.ofString(String.valueOf(structured)),
                Units.NONE,
                TracePriority.NORMAL,
                TraceQuality.OK,
                "");
    }

    public void event(String message) {
        event("TRACE/Event", message, TraceSeverity.INFO, TracePriority.HIGH);
    }

    public void event(String name, String message, TraceSeverity severity, TracePriority priority) {
        session.event(name, message, severity, priority);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        session.endCycle(this);
    }
}
