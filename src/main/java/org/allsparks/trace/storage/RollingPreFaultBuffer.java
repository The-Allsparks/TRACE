package org.allsparks.trace.storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceRecord;

/**
 * Rolling pre-fault buffer retained in memory so a sudden stop still keeps the
 * most recent high-value records.
 */
public final class RollingPreFaultBuffer {
    private final int capacity;
    private final ArrayDeque<TraceRecord> records;

    public RollingPreFaultBuffer(int capacity) {
        this.capacity = capacity;
        this.records = new ArrayDeque<>(capacity);
    }

    public synchronized void offer(TraceRecord record) {
        if (records.size() == capacity) {
            TraceRecord evicted = records.peekFirst();
            if (evicted != null && record.priority().outranks(evicted.priority())) {
                records.removeFirst();
            } else if (evicted != null && evicted.priority() == TracePriority.VERBOSE) {
                records.removeFirst();
            } else if (records.size() == capacity) {
                records.removeFirst();
            }
        }
        records.addLast(record);
    }

    public synchronized List<TraceRecord> snapshot() {
        return new ArrayList<>(records);
    }

    public synchronized void clear() {
        records.clear();
    }
}
