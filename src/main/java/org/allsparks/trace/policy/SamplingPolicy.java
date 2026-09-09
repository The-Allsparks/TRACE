package org.allsparks.trace.policy;

import java.util.concurrent.ConcurrentHashMap;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TypedValue;

/**
 * Configurable sampling, downsampling, and optional change-based recording.
 * Critical events are never sampled away.
 *
 * <p>Callers should {@link #intervalAllows(String, RecordCategory, TracePriority, long)}
 * before allocating a {@link TraceRecord}. {@link #accept} marks the sample after
 * the payload exists. Events, drops, and {@link TracePriority#CRITICAL} skip the
 * interval and are not entered in the last-accepted maps.
 */
public final class SamplingPolicy {
    private final long minIntervalNanos;
    private final double changeThreshold;
    private final boolean changeBased;
    private final ConcurrentHashMap<String, Long> lastAcceptedNanos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TypedValue> lastAcceptedValue = new ConcurrentHashMap<>();

    public SamplingPolicy(long minIntervalNanos, double changeThreshold, boolean changeBased) {
        this.minIntervalNanos = minIntervalNanos;
        this.changeThreshold = changeThreshold;
        this.changeBased = changeBased;
    }

    /**
     * Peek only: true when this name is due (or is never sampled). Does not
     * record a sample. OpMode loops use this to skip {@code TypedValue} /
     * {@code TraceRecord} allocation on ESSENTIAL downsampling.
     */
    public boolean intervalAllows(String name, RecordCategory category, TracePriority priority, long monotonicNanos) {
        if (uncapped(category, priority)) {
            return true;
        }
        Long last = lastAcceptedNanos.get(name);
        return last == null || monotonicNanos - last >= minIntervalNanos;
    }

    /**
     * Interval plus optional change-threshold. On true, records this name as
     * accepted so the next call inside {@code minIntervalNanos} is skipped.
     */
    public boolean accept(
            String name,
            RecordCategory category,
            TracePriority priority,
            long monotonicNanos,
            TypedValue value) {
        if (uncapped(category, priority)) {
            return true;
        }
        Long last = lastAcceptedNanos.get(name);
        if (last != null && monotonicNanos - last < minIntervalNanos) {
            return false;
        }
        if (changeBased && value != null) {
            TypedValue previous = lastAcceptedValue.get(name);
            if (previous != null && previous.approximatelyEquals(value, changeThreshold)) {
                return false;
            }
            lastAcceptedValue.put(name, value);
        }
        lastAcceptedNanos.put(name, monotonicNanos);
        return true;
    }

    public boolean shouldRecord(TraceRecord record) {
        return accept(
                record.name().value(),
                record.category(),
                record.priority(),
                record.monotonicNanos(),
                record.value());
    }

    public void reset() {
        lastAcceptedNanos.clear();
        lastAcceptedValue.clear();
    }

    private static boolean uncapped(RecordCategory category, TracePriority priority) {
        return category == RecordCategory.EVENT
                || category == RecordCategory.DROP
                || priority == TracePriority.CRITICAL;
    }
}
