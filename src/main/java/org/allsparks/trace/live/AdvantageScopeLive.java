package org.allsparks.trace.live;

import org.allsparks.trace.core.TraceRecord;

/**
 * Optional live AdvantageScope publisher loaded by {@code Class.forName}.
 * The OpMode thread may only call {@link #offer(TraceRecord)}.
 */
public interface AdvantageScopeLive extends AutoCloseable {
    /**
     * Latest-value mailbox put plus sequence increment. Must not serialize JSON,
     * touch the network, or wait for AdvantageScope.
     */
    void offer(TraceRecord record);

    AdvantageScopeLiveStats stats();

    /** Bound TCP port after start, or {@code -1} if the server is not listening. */
    int listenPort();

    @Override
    void close();
}
