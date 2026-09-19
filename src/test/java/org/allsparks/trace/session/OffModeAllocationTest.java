package org.allsparks.trace.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import org.allsparks.trace.Trace;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.core.Units;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OffModeAllocationTest {
    @AfterEach
    void resetFacade() {
        Trace.resetForTests(TraceConfig.off());
    }

    @Test
    void offIgnoresBuilderSinkFlags() {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.OFF)
                .memorySink(true)
                .consoleSink(true)
                .build());
        try {
            assertFalse(session.recordingSinksAllocated());
            assertEquals(0, session.recorded().size());
            assertFalse(session.health().enabled());
            session.event("Autonomous started");
            session.record("Battery/Voltage", 13.1, Units.VOLTS);
            assertEquals(0, session.recorded().size());
            assertEquals(0, session.health().dropped());
        } finally {
            session.close();
        }
    }

    @Test
    void offDoesNotStartWriterThread(@TempDir Path dir) {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.OFF)
                .memorySink(true)
                .fileSink(true)
                .storageDirectory(dir)
                .build());
        try {
            assertFalse(session.recordingSinksAllocated());
            assertEquals(null, session.recordingIoFile());
        } finally {
            session.close();
        }
    }

    @Test
    void classInitializationOffSessionHasNoSinks() {
        Trace.configure(TraceConfig.off());
        assertFalse(Trace.session().recordingSinksAllocated());
        assertFalse(Trace.health().enabled());
    }
}
