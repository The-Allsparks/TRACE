package org.allsparks.trace.storage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.core.DropReason;
import org.allsparks.trace.core.DroppedRecordStats;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.session.SessionMetadata;
import org.allsparks.trace.sink.TraceSink;

/**
 * Asynchronous bounded writer. The control loop only enqueues; file I/O runs
 * on a dedicated thread with batched writes, rotation, and quotas.
 */
public final class AsyncBoundedWriter implements TraceSink {
    private final TraceConfig config;
    private final DroppedRecordStats drops;
    private final FileRotator rotator;
    private final SessionMetadata metadata;
    private final ArrayDeque<TraceRecord> queue;
    private final Object lock = new Object();
    private final Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean writerFailed = new AtomicBoolean(false);
    private final AtomicBoolean storageExhausted = new AtomicBoolean(false);
    private final AtomicLong bytesWritten = new AtomicLong();
    private final AtomicLong highWaterMark = new AtomicLong();
    private final AtomicLong batches = new AtomicLong();
    private final CountDownLatch started = new CountDownLatch(1);
    private final RollingPreFaultBuffer rollingBuffer;
    private File currentFile;
    private OutputStream output;
    private long currentFileBytes;
    /**
     * Coalesced drop summary for the next writer cycle. The OpMode thread
     * writes these fields from {@link #noteDrop} while holding {@link #lock}.
     * {@code trace-writer} copies them out in {@link #maybeEmitDropRecord}
     * under the same lock. That lock is the happens-before edge.
     *
     * <p>Several drops between writer cycles become one
     * {@code TRACE/Health/Dropped} record. {@link DroppedRecordStats} still
     * counts each drop with atomics.
     */
    private long pendingDropCount;
    private RecordCategory pendingDropCategory = RecordCategory.OUTPUT;
    private DropReason pendingDropReason = DropReason.QUEUE_FULL;

    public AsyncBoundedWriter(
            TraceConfig config, SessionMetadata metadata, DroppedRecordStats drops) {
        this.config = config;
        this.metadata = metadata;
        this.drops = drops;
        this.rotator = new FileRotator(
                config.storageDirectoryFile(),
                FileRotator.sanitize(metadata.sessionId()),
                config.maxFileBytes(),
                config.maxTotalBytes());
        this.queue = new ArrayDeque<>(config.queueCapacity());
        this.rollingBuffer = new RollingPreFaultBuffer(config.rollingBufferSize());
        this.thread = new Thread(this::run, "trace-writer");
        this.thread.setDaemon(true);
        // Yield to the OpMode loop: disk I/O is optional evidence, driving is not.
        this.thread.setPriority(Thread.MIN_PRIORITY);
        this.thread.start();
    }

    @Override
    public void accept(TraceRecord record) {
        if (!running.get() || writerFailed.get() || storageExhausted.get()) {
            DropReason reason = writerFailed.get()
                    ? DropReason.WRITER_FAILED
                    : storageExhausted.get() ? DropReason.STORAGE_EXHAUSTED : DropReason.SHUTDOWN;
            drops.record(record.category(), reason, 1);
            synchronized (lock) {
                noteDrop(record.category(), reason);
            }
            return;
        }
        synchronized (lock) {
            if (queue.size() < config.queueCapacity()) {
                queue.addLast(record);
                highWaterMark.updateAndGet(current -> Math.max(current, queue.size()));
                lock.notifyAll();
                return;
            }
            TraceRecord evicted = lowestPriority(queue);
            if (evicted != null && record.priority().outranks(evicted.priority())) {
                queue.remove(evicted);
                queue.addLast(record);
                drops.record(evicted.category(), DropReason.LOWER_PRIORITY_EVICTED, 1);
                noteDrop(evicted.category(), DropReason.LOWER_PRIORITY_EVICTED);
                lock.notifyAll();
                return;
            }
            drops.record(record.category(), DropReason.QUEUE_FULL, 1);
            noteDrop(record.category(), DropReason.QUEUE_FULL);
        }
    }

    public Path currentFile() {
        return currentFile == null ? null : Paths.get(currentFile.getAbsolutePath());
    }

    public File currentIoFile() {
        return currentFile;
    }

    public long highWaterMark() {
        return highWaterMark.get();
    }

    public long bytesWritten() {
        return bytesWritten.get();
    }

    public boolean writerFailed() {
        return writerFailed.get();
    }

    public boolean storageExhausted() {
        return storageExhausted.get();
    }

    public RollingPreFaultBuffer rollingBuffer() {
        return rollingBuffer;
    }

    public void awaitStart(long timeoutMs) throws InterruptedException {
        started.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void flush() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void close() {
        running.set(false);
        synchronized (lock) {
            lock.notifyAll();
        }
        try {
            thread.join(config.shutdownFlushTimeout().toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        if (!thread.isAlive()) {
            closeQuietly();
        }
    }

    private void run() {
        try {
            JavaIoFiles.createDirectories(config.storageDirectoryFile());
            rotator.enforceQuota();
            openNextFile();
            started.countDown();
            byte[] batch = new byte[0];
            int batchSize = 0;
            while (running.get() || hasQueued()) {
                TraceRecord record = take();
                if (record == null) {
                    if (batchSize > 0) {
                        writeBatch(batch, batchSize);
                        batchSize = 0;
                    }
                    continue;
                }
                rollingBuffer.offer(record);
                byte[] encoded = TlogCodec.encodeRecord(record);
                if (batchSize + encoded.length > config.batchBytes() && batchSize > 0) {
                    writeBatch(batch, batchSize);
                    batchSize = 0;
                }
                if (batch.length < batchSize + encoded.length) {
                    byte[] grown = new byte[Math.max(batchSize + encoded.length, (int) config.batchBytes())];
                    if (batchSize > 0) {
                        System.arraycopy(batch, 0, grown, 0, batchSize);
                    }
                    batch = grown;
                }
                System.arraycopy(encoded, 0, batch, batchSize, encoded.length);
                batchSize += encoded.length;
                maybeEmitDropRecord();
            }
            if (batchSize > 0) {
                writeBatch(batch, batchSize);
            }
        } catch (IOException exception) {
            writerFailed.set(true);
        } catch (RuntimeException | Error exception) {
            // NoSuchMethodError is an Error. The Hub used to kill this thread
            // on File.toPath() / Files.* without setting writerFailed.
            writerFailed.set(true);
        } finally {
            started.countDown();
            closeQuietly();
        }
    }

    private void writeBatch(byte[] batch, int length) throws IOException {
        if (output == null) {
            storageExhausted.set(true);
            return;
        }
        try {
            if (rotator.exceedsFileLimit(currentFileBytes + length)) {
                openNextFile();
            }
            long total = rotator.enforceQuota();
            if (total + length > config.maxTotalBytes()) {
                storageExhausted.set(true);
                drops.record(RecordCategory.OUTPUT, DropReason.STORAGE_EXHAUSTED, 1);
                return;
            }
            output.write(batch, 0, length);
            currentFileBytes += length;
            bytesWritten.addAndGet(length);
            batches.incrementAndGet();
        } catch (IOException exception) {
            writerFailed.set(true);
            drops.record(RecordCategory.OUTPUT, DropReason.WRITER_FAILED, 1);
            throw exception;
        }
    }

    private void openNextFile() throws IOException {
        closeQuietly();
        currentFile = rotator.nextFile();
        output = new BufferedOutputStream(
                JavaIoFiles.newOutputStream(currentFile),
                (int) config.batchBytes());
        byte[] header = TlogCodec.encodeHeader(metadata);
        output.write(header);
        currentFileBytes = header.length;
        bytesWritten.addAndGet(header.length);
    }

    private TraceRecord take() {
        synchronized (lock) {
            while (queue.isEmpty() && running.get()) {
                try {
                    lock.wait(25);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return queue.pollFirst();
        }
    }

    private boolean hasQueued() {
        synchronized (lock) {
            return !queue.isEmpty();
        }
    }

    /** Caller must hold {@link #lock}. */
    private void noteDrop(RecordCategory category, DropReason reason) {
        pendingDropCount++;
        pendingDropCategory = category;
        pendingDropReason = reason;
    }

    private void maybeEmitDropRecord() {
        long count;
        RecordCategory category;
        DropReason reason;
        synchronized (lock) {
            if (pendingDropCount <= 0) {
                return;
            }
            count = pendingDropCount;
            category = pendingDropCategory;
            reason = pendingDropReason;
            pendingDropCount = 0;
        }
        TraceRecord drop = TraceRecord.builder()
                .category(RecordCategory.DROP)
                .name("TRACE/Health/Dropped")
                .source("TRACE")
                .priority(TracePriority.HIGH)
                .value(TypedValue.ofLong(count))
                .message(category + ":" + reason)
                .build();
        rollingBuffer.offer(drop);
        try {
            byte[] encoded = TlogCodec.encodeRecord(drop);
            if (output != null) {
                output.write(encoded);
                currentFileBytes += encoded.length;
                bytesWritten.addAndGet(encoded.length);
            }
        } catch (IOException ignored) {
            writerFailed.set(true);
        }
    }

    private synchronized void closeQuietly() {
        if (output != null) {
            try {
                output.flush();
                output.close();
            } catch (IOException ignored) {
                writerFailed.set(true);
            }
            output = null;
        }
    }

    private static TraceRecord lowestPriority(ArrayDeque<TraceRecord> queue) {
        TraceRecord lowest = null;
        for (TraceRecord record : queue) {
            if (lowest == null || record.priority().rank() > lowest.priority().rank()) {
                lowest = record;
            }
        }
        return lowest;
    }
}
