package org.allsparks.trace.sink;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.allsparks.trace.core.DropReason;
import org.allsparks.trace.core.DroppedRecordStats;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceRecord;

/**
 * Bounded in-memory ring used for event reconstruction and tests.
 * Evicts the lowest-priority retained record when full.
 */
public final class BoundedMemorySink implements TraceSink {
    private final int capacity;
    private final ArrayDeque<TraceRecord> records;
    private final DroppedRecordStats drops;
    private long highWaterMark;

    public BoundedMemorySink(int capacity, DroppedRecordStats drops) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.records = new ArrayDeque<>(capacity);
        this.drops = drops;
    }

    @Override
    public synchronized void accept(TraceRecord record) {
        if (records.size() < capacity) {
            records.addLast(record);
            highWaterMark = Math.max(highWaterMark, records.size());
            return;
        }
        TraceRecord evicted = findLowestPriority();
        if (evicted != null && record.priority().outranks(evicted.priority())) {
            records.remove(evicted);
            records.addLast(record);
            drops.record(evicted.category(), DropReason.LOWER_PRIORITY_EVICTED, 1);
            return;
        }
        drops.record(record.category(), DropReason.QUEUE_FULL, 1);
    }

    public synchronized List<TraceRecord> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    public synchronized long highWaterMark() {
        return highWaterMark;
    }

    public synchronized int size() {
        return records.size();
    }

    public synchronized void clear() {
        records.clear();
    }

    private TraceRecord findLowestPriority() {
        TraceRecord lowest = null;
        for (TraceRecord record : records) {
            if (lowest == null || record.priority().rank() > lowest.priority().rank()) {
                lowest = record;
            }
            if (lowest.priority() == TracePriority.VERBOSE) {
                return lowest;
            }
        }
        return lowest;
    }
}
