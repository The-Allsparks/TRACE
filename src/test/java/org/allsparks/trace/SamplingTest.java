package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.clock.ManualClock;
import org.allsparks.trace.core.DropReason;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;

class SamplingTest {
    @Test
    void intervalDownsamplingSkipsUnchangedWindow() {
        ManualClock clock = new ManualClock();
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .clock(clock)
                .essentialSampleIntervalNanos(1_000)
                .memorySink(true)
                .build());
        session.record("Battery/Voltage", 12.0, Units.VOLTS);
        session.record("Battery/Voltage", 12.1, Units.VOLTS);
        clock.advanceNanos(1_000);
        session.record("Battery/Voltage", 12.2, Units.VOLTS);
        long kept = session.recorded().stream().filter(r -> r.name().value().equals("Battery/Voltage")).count();
        assertEquals(2, kept);
        assertTrue(session.drops().count(DropReason.SAMPLE_SKIPPED) >= 1);
        session.close();
    }

    @Test
    void changeBasedRecordingSkipsNearlyEqualValues() {
        ManualClock clock = new ManualClock();
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .clock(clock)
                .essentialSampleIntervalNanos(0)
                .changeBasedRecording(true)
                .changeThreshold(0.05)
                .memorySink(true)
                .build());
        session.record("Battery/Voltage", 12.00, Units.VOLTS);
        clock.advanceNanos(10);
        session.record("Battery/Voltage", 12.01, Units.VOLTS);
        clock.advanceNanos(10);
        session.record("Battery/Voltage", 12.20, Units.VOLTS);
        long kept = session.recorded().stream().filter(r -> r.name().value().equals("Battery/Voltage")).count();
        assertEquals(2, kept);
        session.close();
    }
}
