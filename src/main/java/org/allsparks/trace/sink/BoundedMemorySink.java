package org.allsparks.trace.sink;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.allsparks.trace.core.DropReason;
import org.allsparks.trace.core.DroppedRecordStats;
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
    // Worst rank currently stored. A full sink drops an ordinary row without walking the ring.
    private int worstRank;
    private boolean worstKnown;

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
            noteRank(record);
            return;
        }
        // Full. Same-or-lower priority is a drop. Walking 2048 rows here used to run on the OpMode thread.
        if (!worstKnown) {
            recomputeWorst();
        }
        if (worstKnown && record.priority().rank() < worstRank) {
            TraceRecord evicted = removeOneAtWorstRank();
            if (evicted != null) {
                records.addLast(record);
                worstKnown = false;
                drops.record(evicted.category(), DropReason.LOWER_PRIORITY_EVICTED, 1);
                return;
            }
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
        worstKnown = false;
    }

    /** After addLast. One record defines the worst. A later worse row updates it. */
    private void noteRank(TraceRecord record) {
        int rank = record.priority().rank();
        if (records.size() == 1) {
            worstRank = rank;
            worstKnown = true;
            return;
        }
        if (worstKnown && rank > worstRank) {
            worstRank = rank;
        }
    }

    /** One walk, only after a removal made the cached worst stale. */
    private void recomputeWorst() {
        worstKnown = false;
        for (TraceRecord record : records) {
            int rank = record.priority().rank();
            if (!worstKnown || rank > worstRank) {
                worstRank = rank;
                worstKnown = true;
            }
        }
    }

    /** Removes one retained row at {@link #worstRank}. Caller holds the sink lock. */
    private TraceRecord removeOneAtWorstRank() {
        Iterator<TraceRecord> iterator = records.iterator();
        while (iterator.hasNext()) {
            TraceRecord candidate = iterator.next();
            if (candidate.priority().rank() == worstRank) {
                iterator.remove();
                return candidate;
            }
        }
        return null;
    }
}
