package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.adapter.FailOpen;
import org.allsparks.trace.clock.ManualClock;
import org.allsparks.trace.core.DropReason;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;

class SinkHostApiTest {
    @Test
    void badSignalNameDoesNotThrowAndCountsInvalidRecord() {
        TraceSession session = memory();
        assertDoesNotThrow(() -> session.record("not a name!", 1.0, Units.NONE));
        assertEquals(0, session.recorded().size());
        assertTrue(session.drops().count(DropReason.INVALID_RECORD) >= 1);
        session.close();
    }

    @Test
    void badEventNameDoesNotThrow() {
        TraceSession session = memory();
        assertDoesNotThrow(() -> session.event("bad name", "msg", org.allsparks.trace.core.TraceSeverity.INFO, TracePriority.HIGH));
        assertTrue(session.drops().count(DropReason.INVALID_RECORD) >= 1);
        session.close();
    }

    @Test
    void wouldAcceptIsFalseDuringEssentialIntervalWithoutCountingADrop() {
        ManualClock clock = new ManualClock();
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .clock(clock)
                .essentialSampleIntervalNanos(1_000)
                .memorySink(true)
                .build());
        session.record("Battery/Voltage", 12.0, Units.VOLTS);
        long dropsBefore = session.drops().total();
        assertFalse(session.wouldAccept("Battery/Voltage", RecordCategory.OUTPUT, TracePriority.NORMAL));
        assertEquals(dropsBefore, session.drops().total());
        clock.advanceNanos(1_000);
        assertTrue(session.wouldAccept("Battery/Voltage", RecordCategory.OUTPUT, TracePriority.NORMAL));
        session.close();
    }

    @Test
    void integrationEnabledIsFalseUntilListed() {
        TraceSession session = memory();
        assertFalse(session.integrationEnabled("AMPER"));
        session.close();
        TraceSession enabled = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .memorySink(true)
                .enableIntegration("AMPER")
                .build());
        assertTrue(enabled.integrationEnabled("AMPER"));
        assertFalse(enabled.integrationEnabled("SHIFT"));
        enabled.close();
    }

    @Test
    void failOpenSwallowsAdapterThrowAndRecordsException() {
        Trace.resetForTests(TraceConfig.builder().mode(TraceMode.ESSENTIAL).memorySink(true).build());
        FailOpen.run(() -> {
            throw new IllegalStateException("adapter boom");
        });
        assertTrue(Trace.session().recorded().stream().anyMatch(r -> r.name().value().equals("TRACE/Exception")));
        Trace.stop();
    }

    @Test
    void facadeWouldAcceptDelegates() {
        Trace.resetForTests(TraceConfig.builder().mode(TraceMode.ESSENTIAL).memorySink(true).enableIntegration("SHIFT").build());
        assertTrue(Trace.wouldAccept("SHIFT/driver/LeftStickX", RecordCategory.INPUT, TracePriority.HIGH));
        assertTrue(Trace.integrationEnabled("SHIFT"));
        Trace.stop();
        assertFalse(Trace.wouldAccept("SHIFT/driver/LeftStickX", RecordCategory.INPUT, TracePriority.HIGH));
    }

    private static TraceSession memory() {
        return new TraceSession(TraceConfig.builder().mode(TraceMode.ESSENTIAL).memorySink(true).build());
    }
}
