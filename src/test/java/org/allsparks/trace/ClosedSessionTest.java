package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.ftc.OpModeLifecycle;
import org.allsparks.trace.session.TraceSession;
import org.allsparks.trace.storage.TlogReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClosedSessionTest {
    @AfterEach
    void reset() {
        Trace.resetForTests(TraceConfig.off());
    }

    @Test
    void closeRejectsFurtherEventsAndRecords() {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .build());
        session.event("Autonomous started");
        session.record("Battery/Voltage", 13.1, Units.VOLTS);
        session.close();

        assertFalse(session.isOpen());
        assertFalse(session.health().enabled());
        assertTrue(session.recorded().stream().anyMatch(r -> r.name().value().equals("TRACE/Session/Stop")));

        long accepted = session.health().accepted();
        List<TraceRecord> before = session.recorded();
        session.event("after close");
        session.record("PostClose/Signal", 1.0, Units.DIMENSIONLESS);
        session.recordInput("PostClose/Input", 2.0, Units.DIMENSIONLESS);

        assertEquals(accepted, session.health().accepted());
        assertEquals(before.size(), session.recorded().size());
        assertTrue(session.recorded().stream().noneMatch(r -> "after close".equals(r.message())));
        assertTrue(session.recorded().stream().noneMatch(r -> r.name().value().startsWith("PostClose/")));
    }

    @Test
    void closeRejectsFurtherFileRecords(@TempDir Path dir) throws Exception {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .storageDirectory(dir)
                .fileSink(true)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .shutdownFlushTimeout(java.time.Duration.ofSeconds(5))
                .build());
        session.event("Autonomous started");
        session.close();

        Path file = session.recordingFile();
        assertTrue(file != null && Files.exists(file));
        long accepted = session.health().accepted();
        session.event("after close");
        session.record("PostClose/Signal", 1.0, Units.DIMENSIONLESS);

        assertEquals(accepted, session.health().accepted());
        TlogReader reader = TlogReader.read(file);
        assertTrue(reader.records().stream().anyMatch(r -> r.name().value().equals("TRACE/Session/Stop")));
        assertTrue(reader.records().stream().noneMatch(r -> "after close".equals(r.message())));
        assertTrue(reader.records().stream().noneMatch(r -> r.name().value().equals("PostClose/Signal")));
    }

    @Test
    void opModeStopIsRecordedBeforeSessionRefusesWork() {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.EVENTS)
                .memorySink(true)
                .opModeName("Auto")
                .build());
        OpModeLifecycle lifecycle = new OpModeLifecycle(session);
        lifecycle.init();
        lifecycle.start();
        lifecycle.stop();

        List<TraceRecord> recorded = session.recorded();
        int opModeStop = indexOfName(recorded, "TRACE/OpMode/Stop");
        int sessionStop = indexOfName(recorded, "TRACE/Session/Stop");
        assertTrue(opModeStop >= 0, "OpMode stop event missing");
        assertTrue(sessionStop >= 0, "session finalization event missing");
        assertTrue(opModeStop < sessionStop, "OpMode stop must be recorded before session finalization");
        assertFalse(session.isOpen());

        long accepted = session.health().accepted();
        session.event("after stop");
        lifecycle.stop();
        assertEquals(accepted, session.health().accepted());
        assertTrue(session.recorded().stream().noneMatch(r -> "after stop".equals(r.message())));
        assertEquals(1, session.recorded().stream().filter(r -> r.name().value().equals("TRACE/OpMode/Stop")).count());
        assertEquals(1, session.recorded().stream().filter(r -> r.name().value().equals("TRACE/Session/Stop")).count());
    }

    @Test
    void doubleCloseDoesNotThrow() {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .memorySink(true)
                .build());
        assertDoesNotThrow(() -> {
            session.close();
            session.close();
        });
        assertFalse(session.isOpen());
        assertFalse(session.health().enabled());
    }

    @Test
    void stopLeavesFacadeDisabledAndIgnoresFurtherCalls() {
        Trace.configure(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .build());
        Trace.event("Autonomous started");
        Trace.stop();

        assertFalse(Trace.health().enabled());
        assertEquals(TraceMode.OFF, Trace.health().mode());
        long accepted = Trace.health().accepted();
        Trace.event("after stop");
        Trace.record("PostClose/Signal", 1.0, Units.DIMENSIONLESS);
        assertEquals(accepted, Trace.health().accepted());
        assertEquals(0, Trace.session().recorded().size());
    }

    @Test
    void doubleStopDoesNotThrow() {
        Trace.configure(TraceConfig.builder().mode(TraceMode.EVENTS).memorySink(true).build());
        assertDoesNotThrow(() -> {
            Trace.stop();
            Trace.stop();
        });
        assertFalse(Trace.health().enabled());
    }

    @Test
    void replayModeRemainsImpossibleAfterStop() {
        Trace.configure(TraceConfig.builder().mode(TraceMode.FULL).memorySink(true).build());
        Trace.stop();
        assertThrows(IllegalArgumentException.class, () -> TraceConfig.builder().mode(TraceMode.REPLAY).build());
        assertFalse(Trace.health().enabled());
    }

    private static int indexOfName(List<TraceRecord> records, String name) {
        for (int i = 0; i < records.size(); i++) {
            if (name.equals(records.get(i).name().value())) {
                return i;
            }
        }
        return -1;
    }
}
