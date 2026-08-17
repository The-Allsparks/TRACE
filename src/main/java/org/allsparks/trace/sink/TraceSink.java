package org.allsparks.trace.sink;

import org.allsparks.trace.core.TraceRecord;

/**
 * Destination for TRACE records. Sinks must not block the control loop for
 * filesystem I/O. Implementations may drop records only by reporting through
 * the session drop accounting path.
 */
public interface TraceSink extends AutoCloseable {
    void accept(TraceRecord record);

    default void flush() {}

    @Override
    default void close() {}
}
