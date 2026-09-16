package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.core.Units;
import org.allsparks.trace.live.AdvantageScopeLiveLoader;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AdvantageScopeAbsentTest {
    @AfterEach
    void reset() {
        Trace.resetForTests(TraceConfig.off());
    }

    @Test
    void streamingDefaultsOffAndDoesNotRequireTheOptionalModule() {
        TraceConfig config = TraceConfig.builder().mode(TraceMode.ESSENTIAL).memorySink(true).build();
        assertFalse(config.advantageScopeStreaming());
        assertEquals(8000, config.advantageScopePort());
        assertEquals(20, config.advantageScopeRateHz());
        Trace.configure(config);
        Trace.record("Battery/Voltage", 12.8, Units.VOLTS);
        assertTrue(Trace.session().recorded().stream().anyMatch(r -> r.name().value().equals("Battery/Voltage")));
        assertFalse(Trace.session().advantageScopeLive().enabled());
        assertEquals(-1, Trace.session().advantageScopeLive().listenPort());
    }

    @Test
    void streamingFlagWithoutImplementationLeavesRecordingIntact() {
        boolean modulePresent;
        try {
            Class.forName(AdvantageScopeLiveLoader.IMPLEMENTATION);
            modulePresent = true;
        } catch (ClassNotFoundException absent) {
            modulePresent = false;
        }
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .memorySink(true)
                .advantageScopeStreaming(true)
                .advantageScopePort(0)
                .build());
        session.record("Drive/Command", 0.4, Units.DIMENSIONLESS);
        assertTrue(session.recorded().stream().anyMatch(r -> r.name().value().equals("Drive/Command")));
        if (!modulePresent) {
            assertFalse(session.advantageScopeLive().listening());
            assertTrue(session.recorded().stream().anyMatch(r -> r.name().value().equals("TRACE/AdvantageScope/Error")));
        }
        session.close();
    }
}
