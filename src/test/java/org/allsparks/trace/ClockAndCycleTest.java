package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.clock.ManualClock;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.SchemaVersion;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceCycle;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;

class ClockAndCycleTest {
    @Test
    void timestampsAreMonotonicAndCyclesIncrease() {
        ManualClock clock = new ManualClock();
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .clock(clock)
                .captureWallClock(true)
                .memorySink(true)
                .build());
        long previous = -1;
        for (int i = 0; i < 5; i++) {
            clock.advanceNanos(1_000_000);
            clock.setWallClockMillis(1_000 + i);
            try (TraceCycle cycle = session.beginCycle()) {
                cycle.recordOutput("Drive/Command", i, Units.DIMENSIONLESS);
            }
            long last = session.recorded().get(session.recorded().size() - 1).monotonicNanos();
            assertTrue(last > previous);
            previous = last;
        }
        assertEquals(5, session.currentCycleNumber());
        assertTrue(session.recorded().stream().allMatch(r -> r.schemaVersion() == SchemaVersion.RECORD));
        assertTrue(session.recorded().stream().anyMatch(r -> r.category() == RecordCategory.OUTPUT));
        session.close();
    }

    @Test
    void replayModeFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> TraceConfig.builder().mode(TraceMode.REPLAY).build());
    }
}
