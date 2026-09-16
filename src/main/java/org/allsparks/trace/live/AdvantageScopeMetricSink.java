package org.allsparks.trace.live;

import org.allsparks.trace.core.Units;

/**
 * Records {@code TRACE/AdvantageScope/*} self-metrics into the normal TRACE
 * sinks (memory / {@code .tlog}) without re-entering the live mailbox.
 */
public interface AdvantageScopeMetricSink {
    void record(String name, double value, Units units);

    void record(String name, long value, Units units);

    void record(String name, boolean value);
}
