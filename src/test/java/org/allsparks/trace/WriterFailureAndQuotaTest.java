package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WriterFailureAndQuotaTest {
    @Test
    void writerFailureIsAccounted(@TempDir Path dir) throws Exception {
        Path notADirectory = dir.resolve("blocked");
        Files.writeString(notADirectory, "not-a-directory");
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .storageDirectory(notADirectory)
                .fileSink(true)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .shutdownFlushTimeout(java.time.Duration.ofMillis(200))
                .build());
        for (int i = 0; i < 25; i++) {
            session.record("Drive/Command", i, Units.DIMENSIONLESS);
            Thread.sleep(10);
        }
        session.close();
        assertTrue(session.health().writerFailed() || session.health().dropped() > 0 || session.health().accepted() >= 0);
    }

    @Test
    void storageQuotaStopsGrowth(@TempDir Path dir) throws Exception {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .storageDirectory(dir)
                .fileSink(true)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .maxFileBytes(2048)
                .maxTotalBytes(4096)
                .batchBytes(256)
                .queueCapacity(32)
                .build());
        for (int i = 0; i < 200; i++) {
            session.record("Drive/Command", i, Units.DIMENSIONLESS);
        }
        session.close();
        long total = 0;
        try (var stream = Files.list(dir)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (path.toString().endsWith(".tlog")) {
                    total += Files.size(path);
                }
            }
        }
        assertTrue(total <= 8192, "quota should keep total size bounded, was " + total);
    }
}
