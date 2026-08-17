package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.clock.ManualClock;
import org.allsparks.trace.core.TraceSeverity;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.ftc.OpModeLifecycle;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;

class EventRecorderTest {
    @Test
    void eventsPreserveOrderAndLifecycle() {
        ManualClock clock = new ManualClock();
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.EVENTS)
                .clock(clock)
                .memorySink(true)
                .opModeName("Auto")
                .build());
        OpModeLifecycle lifecycle = new OpModeLifecycle(session);
        lifecycle.init();
        clock.advanceNanos(10);
        lifecycle.start();
        clock.advanceNanos(10);
        session.event("Autonomous started");
        session.recordException(new IllegalStateException("boom"));
        try (var cycle = session.beginCycle()) {
            clock.advanceNanos(40_000_000);
        }
        String text = session.exportHumanReadable();
        assertTrue(text.contains("TRACE/OpMode/Init"));
        assertTrue(text.contains("TRACE/OpMode/Start"));
        assertTrue(text.contains("Autonomous started"));
        assertTrue(text.contains("IllegalStateException"));
        assertTrue(text.contains("TRACE/Loop/Overrun"));
        long previous = -1;
        for (var record : session.recorded()) {
            assertTrue(record.monotonicNanos() >= previous);
            previous = record.monotonicNanos();
        }
        assertEquals(TraceSeverity.ERROR, session.recorded().stream()
                .filter(r -> r.name().value().equals("TRACE/Exception"))
                .findFirst()
                .orElseThrow()
                .severity());
        session.close();
        assertTrue(session.exportHumanReadable().contains("session finalized")
                || session.recorded().stream().anyMatch(r -> r.name().value().equals("TRACE/Session/Stop")));
    }

    @Test
    void essentialModeDoesNotRequireUnitsForSimpleRecord() {
        TraceSession session = new TraceSession(TraceConfig.builder().mode(TraceMode.ESSENTIAL).memorySink(true).essentialSampleIntervalNanos(0).build());
        session.record("Drive/Command", 0.3, Units.DIMENSIONLESS);
        assertEquals(1, session.recorded().stream().filter(r -> r.name().value().equals("Drive/Command")).count());
        session.close();
    }
}
