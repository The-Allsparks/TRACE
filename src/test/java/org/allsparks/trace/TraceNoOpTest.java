package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.core.Units;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TraceNoOpTest {
    @AfterEach
    void reset() {
        Trace.resetForTests(TraceConfig.off());
    }

    @Test
    void offModeDoesNotRecord() {
        Trace.configure(TraceConfig.off());
        Trace.event("Autonomous started");
        Trace.record("Battery/Voltage", 13.1, Units.VOLTS);
        assertFalse(Trace.health().enabled());
        assertEquals(0, Trace.session().recorded().size());
        assertEquals(0, Trace.health().dropped());
    }

    @Test
    void eventsModeRecordsEventsOnly() {
        Trace.configure(TraceConfig.builder().mode(TraceMode.EVENTS).memorySink(true).build());
        Trace.event("Autonomous started");
        Trace.record("Battery/Voltage", 13.1, Units.VOLTS);
        assertTrue(Trace.session().recorded().stream().anyMatch(r -> r.message().equals("Autonomous started")));
        assertTrue(Trace.session().recorded().stream().noneMatch(r -> r.name().value().equals("Battery/Voltage")));
    }
}
