package org.allsparks.trace.sink;

import java.util.ArrayList;
import java.util.List;
import org.allsparks.trace.core.TraceRecord;

/** Fan-out sink. Failures in one destination must not hide others. */
public final class CompositeSink implements TraceSink {
    private final List<TraceSink> sinks;

    public CompositeSink(List<TraceSink> sinks) {
        this.sinks = new ArrayList<>(sinks);
    }

    @Override
    public void accept(TraceRecord record) {
        RuntimeException first = null;
        for (TraceSink sink : sinks) {
            try {
                sink.accept(record);
            } catch (RuntimeException exception) {
                if (first == null) {
                    first = exception;
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }

    @Override
    public void flush() {
        for (TraceSink sink : sinks) {
            sink.flush();
        }
    }

    @Override
    public void close() {
        for (TraceSink sink : sinks) {
            sink.close();
        }
    }
}
