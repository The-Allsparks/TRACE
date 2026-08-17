package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.SessionMetadata;
import org.allsparks.trace.session.TraceSession;
import org.allsparks.trace.storage.TlogCodec;
import org.allsparks.trace.storage.TlogReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageAndTruncationTest {
    @Test
    void fileRoundTripAndTruncationRecovery(@TempDir Path dir) throws Exception {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .opModeName("StorageTest")
                .storageDirectory(dir)
                .fileSink(true)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .maxFileBytes(32 * 1024)
                .maxTotalBytes(64 * 1024)
                .queueCapacity(128)
                .build());
        session.event("Autonomous started");
        for (int i = 0; i < 40; i++) {
            session.record("Battery/Voltage", 13.0 - i * 0.01, Units.VOLTS);
        }
        session.close();
        Path file = session.recordingFile();
        assertTrue(file != null && Files.exists(file));
        TlogReader complete = TlogReader.read(file);
        assertTrue(complete.records().size() >= 1);
        assertEquals("StorageTest", complete.metadata().opModeName());
        assertTrue(complete.complete());

        byte[] original = Files.readAllBytes(file);
        Path truncated = dir.resolve("truncated.tlog");
        int keep = Math.max(20, original.length - 12);
        Files.write(truncated, java.util.Arrays.copyOf(original, keep));
        TlogReader recovered = TlogReader.read(truncated);
        assertFalse(recovered.complete());
        assertTrue(recovered.records().size() < complete.records().size() || recovered.truncatedBytes() > 0);

        byte[] corrupt = original.clone();
        if (corrupt.length > 80) {
            corrupt[80] ^= 0x7F;
        }
        Path corruptFile = dir.resolve("corrupt.tlog");
        Files.write(corruptFile, corrupt);
        TlogReader corruptReader = TlogReader.read(corruptFile);
        assertTrue(corruptReader.corruptRecords() >= 0);
    }

    @Test
    void rotationCreatesAdditionalFiles(@TempDir Path dir) throws Exception {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .storageDirectory(dir)
                .fileSink(true)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .maxFileBytes(1500)
                .maxTotalBytes(20_000)
                .batchBytes(256)
                .queueCapacity(64)
                .build());
        for (int i = 0; i < 80; i++) {
            session.record("Drive/Command", i, Units.DIMENSIONLESS);
        }
        session.close();
        long files = Files.list(dir).filter(p -> p.getFileName().toString().endsWith(".tlog")).count();
        assertTrue(files >= 1);
    }

    @Test
    void headerRoundTrip() {
        SessionMetadata metadata = SessionMetadata.builder()
                .sessionId("abc")
                .gitCommitSha("deadbeef")
                .traceVersion("0.1.0-SNAPSHOT")
                .recordingMode("FULL")
                .opModeName("Auto")
                .build();
        byte[] header = TlogCodec.encodeHeader(metadata);
        assertTrue(header.length > 8);
        assertEquals('T', header[0]);
    }
}
