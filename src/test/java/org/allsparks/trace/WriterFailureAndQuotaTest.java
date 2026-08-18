package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.allsparks.trace.core.DropReason;
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
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        int recorded = 0;
        while (System.nanoTime() < deadline
                && !session.health().writerFailed()
                && session.drops().count(DropReason.WRITER_FAILED) == 0) {
            session.record("Drive/Command", recorded++, Units.DIMENSIONLESS);
            Thread.sleep(10);
        }
        session.close();
        assertTrue(
                session.health().writerFailed() || session.drops().count(DropReason.WRITER_FAILED) > 0,
                "writer failure must set health().writerFailed() and/or WRITER_FAILED drops");
    }

    @Test
    void storageQuotaStopsGrowth(@TempDir Path dir) throws Exception {
        long maxFileBytes = 2048L;
        long maxTotalBytes = 4096L;
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .storageDirectory(dir)
                .fileSink(true)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .maxFileBytes(maxFileBytes)
                .maxTotalBytes(maxTotalBytes)
                .batchBytes(256)
                .queueCapacity(512)
                .shutdownFlushTimeout(java.time.Duration.ofSeconds(5))
                .build());
        for (int i = 0; i < 2000; i++) {
            session.record("Drive/Command", i, Units.DIMENSIONLESS);
        }
        session.close();
        long total = 0;
        try (var stream = Files.list(dir)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (path.getFileName().toString().endsWith(".tlog")) {
                    total += Files.size(path);
                }
            }
        }
        // FileRotator.enforceQuota deletes oldest files until total <= maxTotalBytes
        // but always keeps at least one file. Rotation can add a new segment (header +
        // records) before the next enforceQuota pass, so on-disk totals may briefly
        // exceed maxTotalBytes by less than one maxFileBytes segment. TraceConfig
        // requires maxTotalBytes >= maxFileBytes. This is still far tighter than the
        // old silent 2x fudge (8192 for a 4096 cap).
        long quotaUpperBound = maxTotalBytes + maxFileBytes;
        assertTrue(
                total <= quotaUpperBound,
                "quota should keep total .tlog bytes <= maxTotalBytes + maxFileBytes ("
                        + quotaUpperBound + "), was " + total);
    }
}
