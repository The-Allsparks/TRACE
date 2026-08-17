package org.allsparks.trace.sink;

import org.allsparks.trace.core.TraceRecord;

/** Sink used when TRACE is {@code OFF} or a destination is disabled. */
public final class NoOpSink implements TraceSink {
    public static final NoOpSink INSTANCE = new NoOpSink();

    private NoOpSink() {}

    @Override
    public void accept(TraceRecord record) {
        // Intentionally empty.
    }
}
