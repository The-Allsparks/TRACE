package org.allsparks.trace.policy;

import java.util.concurrent.ConcurrentHashMap;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TypedValue;

/**
 * Configurable sampling, downsampling, and optional change-based recording.
 * Critical events are never sampled away.
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

    public boolean shouldRecord(TraceRecord record) {
        if (record.category() == RecordCategory.EVENT || record.category() == RecordCategory.DROP) {
            return true;
        }
        if (record.priority() == TracePriority.CRITICAL) {
            return true;
        }
        String key = record.name().value();
        Long last = lastAcceptedNanos.get(key);
        if (last != null && record.monotonicNanos() - last < minIntervalNanos) {
            return false;
        }
        if (changeBased) {
            TypedValue previous = lastAcceptedValue.get(key);
            if (previous != null && previous.approximatelyEquals(record.value(), changeThreshold)) {
                return false;
            }
            lastAcceptedValue.put(key, record.value());
        }
        lastAcceptedNanos.put(key, record.monotonicNanos());
        return true;
    }

    public void reset() {
        lastAcceptedNanos.clear();
        lastAcceptedValue.clear();
    }
}
