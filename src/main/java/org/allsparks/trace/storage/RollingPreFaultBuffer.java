package org.allsparks.trace.storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceRecord;

/**
 * In-memory rolling buffer of records the file writer has dequeued.
 *
 * <p>This is a debug snapshot only. It lives in RAM, is not dumped on writer
 * failure, and is not power-loss durable. A JVM exit or power loss
 * discards it. Sudden stop may still keep the most recent high-value records
 * until process memory is gone. Power-loss recovery is {@code TlogReader}
 * truncation tolerance of the {@code .tlog} file, not this buffer.
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
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    public synchronized void clear() {
        records.clear();
    }
}
