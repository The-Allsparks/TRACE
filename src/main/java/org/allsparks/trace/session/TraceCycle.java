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
        recordInput(name, value, units, null);
    }

    /**
     * Input scalar plus a Driver Station group. Empty group is tlog only.
     */
    public void recordInput(String name, double value, Units units, String dsGroup) {
        session.record(
                RecordCategory.INPUT, name, value, units, TracePriority.HIGH, TraceQuality.OK, "", dsGroup);
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
        recordOutput(name, value, units, null);
    }

    /**
     * Output scalar plus a Driver Station group. Empty group is tlog only.
     */
    public void recordOutput(String name, double value, Units units, String dsGroup) {
        session.record(
                RecordCategory.OUTPUT, name, value, units, TracePriority.NORMAL, TraceQuality.OK, "", dsGroup);
    }

    public void recordOutput(String name, Pose2d pose) {
        session.record(RecordCategory.OUTPUT, name, TypedValue.ofPose(pose), Units.METERS, TracePriority.NORMAL, TraceQuality.OK, "");
    }

    public void recordOutput(String name, Object structured) {
        recordOutput(name, structured, null);
    }

    /**
     * Output text plus a Driver Station group. Empty group is tlog only.
     */
    public void recordOutput(String name, Object structured, String dsGroup) {
        session.record(
                RecordCategory.OUTPUT,
                name,
                TypedValue.ofString(String.valueOf(structured)),
                Units.NONE,
                TracePriority.NORMAL,
                TraceQuality.OK,
                "",
                dsGroup);
    }

    /**
     * Record many named input scalars. Parallel arrays; shortest length wins.
     * Null name, null units, or NaN value is skipped. Reuse the arrays across loops.
     */
    public void recordInputs(String[] names, double[] values, Units[] units) {
        recordMany(
                RecordCategory.INPUT,
                TracePriority.HIGH,
                names,
                values,
                units,
                length(names, values, units),
                null);
    }

    /**
     * Record many named input scalars that share one unit.
     */
    public void recordInputs(String[] names, double[] values, Units units) {
        recordManySameUnit(RecordCategory.INPUT, TracePriority.HIGH, names, values, units);
    }

    /**
     * Record a reused {@link TraceBatch} as inputs. Call {@link TraceBatch#clear()} first each loop.
     */
    public void recordInputs(TraceBatch batch) {
        if (batch == null) {
            return;
        }
        recordMany(
                RecordCategory.INPUT,
                TracePriority.HIGH,
                batch.names(),
                batch.values(),
                batch.units(),
                batch.size(),
                batch.dsGroups());
    }

    /**
     * Record many named output scalars. Parallel arrays; shortest length wins.
     * Null name, null units, or NaN value is skipped. Reuse the arrays across loops.
     */
    public void recordOutputs(String[] names, double[] values, Units[] units) {
        recordMany(
                RecordCategory.OUTPUT,
                TracePriority.NORMAL,
                names,
                values,
                units,
                length(names, values, units),
                null);
    }

    /**
     * Record many named output scalars that share one unit.
     */
    public void recordOutputs(String[] names, double[] values, Units units) {
        recordManySameUnit(RecordCategory.OUTPUT, TracePriority.NORMAL, names, values, units);
    }

    /**
     * Record a reused {@link TraceBatch} as outputs. Call {@link TraceBatch#clear()} first each loop.
     */
    public void recordOutputs(TraceBatch batch) {
        if (batch == null) {
            return;
        }
        recordMany(
                RecordCategory.OUTPUT,
                TracePriority.NORMAL,
                batch.names(),
                batch.values(),
                batch.units(),
                batch.size(),
                batch.dsGroups());
    }

    private void recordManySameUnit(
            RecordCategory category, TracePriority priority, String[] names, double[] values, Units units) {
        if (names == null || values == null || units == null) {
            return;
        }
        int n = Math.min(names.length, values.length);
        for (int i = 0; i < n; i++) {
            if (names[i] == null || Double.isNaN(values[i])) {
                continue;
            }
            session.record(category, names[i], values[i], units, priority, TraceQuality.OK, "");
        }
    }

    private void recordMany(
            RecordCategory category,
            TracePriority priority,
            String[] names,
            double[] values,
            Units[] units,
            int count,
            String[] dsGroups) {
        if (names == null || values == null || units == null || count <= 0) {
            return;
        }
        int n = Math.min(count, Math.min(names.length, Math.min(values.length, units.length)));
        for (int i = 0; i < n; i++) {
            if (names[i] == null || units[i] == null || Double.isNaN(values[i])) {
                continue;
            }
            String dsGroup = dsGroups == null || i >= dsGroups.length ? null : dsGroups[i];
            session.record(category, names[i], values[i], units[i], priority, TraceQuality.OK, "", dsGroup);
        }
    }

    private static int length(String[] names, double[] values, Units[] units) {
        if (names == null || values == null || units == null) {
            return 0;
        }
        return Math.min(names.length, Math.min(values.length, units.length));
    }

    public void event(String message) {
        event("TRACE/Event", message, TraceSeverity.INFO, TracePriority.HIGH);
    }

    public void event(String name, String message, TraceSeverity severity, TracePriority priority) {
        session.event(name, message, severity, priority);
    }

    /**
     * Pack Driver Station groups from this cycle onto the telemetry adapter.
     * Call once at the end of the loop, before {@code telemetry.update()}.
     */
    public void publishTelemetry() {
        session.publishTelemetry();
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
