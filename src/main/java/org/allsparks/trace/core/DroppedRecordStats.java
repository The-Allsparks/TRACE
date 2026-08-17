package org.allsparks.trace.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe dropped-record counters grouped by reason and category.
 */
public final class DroppedRecordStats {
    private final EnumMap<DropReason, AtomicLong> byReason = new EnumMap<>(DropReason.class);
    private final EnumMap<RecordCategory, AtomicLong> byCategory = new EnumMap<>(RecordCategory.class);
    private final AtomicLong total = new AtomicLong();

    public DroppedRecordStats() {
        for (DropReason reason : DropReason.values()) {
            byReason.put(reason, new AtomicLong());
        }
        for (RecordCategory category : RecordCategory.values()) {
            byCategory.put(category, new AtomicLong());
        }
    }

    public void record(RecordCategory category, DropReason reason, long count) {
        if (count <= 0) {
            return;
        }
        byReason.get(reason).addAndGet(count);
        byCategory.get(category).addAndGet(count);
        total.addAndGet(count);
    }

    public long total() {
        return total.get();
    }

    public long count(DropReason reason) {
        return byReason.get(reason).get();
    }

    public long count(RecordCategory category) {
        return byCategory.get(category).get();
    }

    public Map<DropReason, Long> snapshotByReason() {
        EnumMap<DropReason, Long> snapshot = new EnumMap<>(DropReason.class);
        for (Map.Entry<DropReason, AtomicLong> entry : byReason.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public Map<RecordCategory, Long> snapshotByCategory() {
        EnumMap<RecordCategory, Long> snapshot = new EnumMap<>(RecordCategory.class);
        for (Map.Entry<RecordCategory, AtomicLong> entry : byCategory.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return Collections.unmodifiableMap(snapshot);
    }
}
