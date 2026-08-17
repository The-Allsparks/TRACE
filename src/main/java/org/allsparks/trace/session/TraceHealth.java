package org.allsparks.trace.session;

import java.util.Map;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.core.DropReason;
import org.allsparks.trace.core.DroppedRecordStats;
import org.allsparks.trace.core.RecordCategory;

/** Observable recording health. TRACE never hides dropped-data accounting. */
public final class TraceHealth {
    private final TraceMode mode;
    private final boolean enabled;
    private final boolean writerFailed;
    private final boolean storageExhausted;
    private final long accepted;
    private final long dropped;
    private final long cycle;
    private final long queueHighWaterMark;
    private final long bytesWritten;
    private final Map<DropReason, Long> dropsByReason;
    private final Map<RecordCategory, Long> dropsByCategory;
    private final long lastLoopDurationNanos;
    private final boolean loopOverrun;

    public TraceHealth(
            TraceMode mode,
            boolean enabled,
            boolean writerFailed,
            boolean storageExhausted,
            long accepted,
            long dropped,
            long cycle,
            long queueHighWaterMark,
            long bytesWritten,
            Map<DropReason, Long> dropsByReason,
            Map<RecordCategory, Long> dropsByCategory,
            long lastLoopDurationNanos,
            boolean loopOverrun) {
        this.mode = mode;
        this.enabled = enabled;
        this.writerFailed = writerFailed;
        this.storageExhausted = storageExhausted;
        this.accepted = accepted;
        this.dropped = dropped;
        this.cycle = cycle;
        this.queueHighWaterMark = queueHighWaterMark;
        this.bytesWritten = bytesWritten;
        this.dropsByReason = dropsByReason;
        this.dropsByCategory = dropsByCategory;
        this.lastLoopDurationNanos = lastLoopDurationNanos;
        this.loopOverrun = loopOverrun;
    }

    public TraceMode mode() {
        return mode;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean writerFailed() {
        return writerFailed;
    }

    public boolean storageExhausted() {
        return storageExhausted;
    }

    public long accepted() {
        return accepted;
    }

    public long dropped() {
        return dropped;
    }

    public long cycle() {
        return cycle;
    }

    public long queueHighWaterMark() {
        return queueHighWaterMark;
    }

    public long bytesWritten() {
        return bytesWritten;
    }

    public Map<DropReason, Long> dropsByReason() {
        return dropsByReason;
    }

    public Map<RecordCategory, Long> dropsByCategory() {
        return dropsByCategory;
    }

    public long lastLoopDurationNanos() {
        return lastLoopDurationNanos;
    }

    public boolean loopOverrun() {
        return loopOverrun;
    }
}
