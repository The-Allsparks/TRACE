package org.allsparks.trace.advantagescope;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;

/**
 * Latest-value mailbox. {@link #offer(TraceRecord)} is safe for the OpMode
 * thread: map put plus a sequence increment, no JSON and no network.
 */
final class LatestValueMailbox {
    static final String METRIC_PREFIX = "TRACE/AdvantageScope/";

    private final int maxValues;
    private final ConcurrentHashMap<String, LiveSlot> values = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong valuesSkipped = new AtomicLong();

    LatestValueMailbox(int maxValues) {
        this.maxValues = maxValues;
    }

    void offer(TraceRecord record) {
        if (record == null) {
            return;
        }
        String name = record.name().value();
        if (name.startsWith(METRIC_PREFIX)) {
            return;
        }
        TypedValue value = record.value();
        if (value.kind() == TypedValue.Kind.DOUBLE_ARRAY || value.kind() == TypedValue.Kind.NONE) {
            valuesSkipped.incrementAndGet();
            return;
        }
        if (!putSlot(name, value, record.units(), record.wallClockMillis())) {
            valuesSkipped.incrementAndGet();
            return;
        }
        sequence.incrementAndGet();
    }

    long sequence() {
        return sequence.get();
    }

    long valuesSkipped() {
        return valuesSkipped.get();
    }

    Iterator<Map.Entry<String, LiveSlot>> entries() {
        return values.entrySet().iterator();
    }

    int size() {
        return values.size();
    }

    private boolean putSlot(String name, TypedValue value, Units units, long wallClockMillis) {
        LiveSlot slot = values.get(name);
        if (slot == null) {
            if (values.size() >= maxValues) {
                return false;
            }
            LiveSlot created = new LiveSlot();
            LiveSlot raced = values.putIfAbsent(name, created);
            slot = raced == null ? created : raced;
            if (raced == null && values.size() > maxValues) {
                values.remove(name, created);
                return false;
            }
        }
        slot.update(value, units, wallClockMillis);
        return true;
    }

    static final class LiveSlot {
        private volatile TypedValue value;
        private volatile Units units;
        private volatile long wallClockMillis;

        void update(TypedValue value, Units units, long wallClockMillis) {
            this.value = value;
            this.units = units;
            this.wallClockMillis = wallClockMillis;
        }

        TypedValue value() {
            return value;
        }

        Units units() {
            return units;
        }

        long wallClockMillis() {
            return wallClockMillis;
        }
    }
}
