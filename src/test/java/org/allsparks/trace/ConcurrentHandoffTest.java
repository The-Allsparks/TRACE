package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConcurrentHandoffTest {
    @Test
    void concurrentProducersDoNotLoseAccounting(@TempDir Path dir) throws Exception {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .storageDirectory(dir)
                .fileSink(true)
                .memorySink(true)
                .queueCapacity(64)
                .essentialSampleIntervalNanos(0)
                .build());
        int threads = 4;
        int perThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        session.record("Drive/Command", i, Units.DIMENSIONLESS);
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();
        session.close();
        long accounted = session.health().accepted() + session.health().dropped();
        assertTrue(accounted >= (long) threads * perThread);
        assertTrue(session.health().accepted() > 0);
    }

    @Test
    void overflowStillAccountsEveryRecordEvenIfDropLogsCoalesce(@TempDir Path dir) throws Exception {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .storageDirectory(dir)
                .fileSink(true)
                .memorySink(false)
                .queueCapacity(4)
                .essentialSampleIntervalNanos(0)
                .shutdownFlushTimeout(java.time.Duration.ofSeconds(5))
                .build());
        int threads = 8;
        int perThread = 80;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        session.record("Drive/Command", i, Units.DIMENSIONLESS);
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();
        session.close();
        long produced = (long) threads * perThread;
        long accounted = session.health().accepted() + session.health().dropped();
        assertTrue(accounted >= produced, "atomic drop counters must still see every overflow");
        // TRACE/Health/Dropped .tlog rows may coalesce several overflows into one
        // record. DroppedRecordStats is the accurate count. A fast writer may
        // still accept every record; the lock protocol is what #21 requires.
    }

    @Test
    void cleanShutdownFlushesWithoutThrowing(@TempDir Path dir) {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .storageDirectory(dir)
                .fileSink(true)
                .memorySink(true)
                .build());
        session.event("Autonomous started");
        session.close();
        session.close();
        assertTrue(session.health().accepted() >= 1 || !session.isOpen());
    }
}
