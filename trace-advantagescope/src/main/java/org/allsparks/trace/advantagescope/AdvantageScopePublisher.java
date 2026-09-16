package org.allsparks.trace.advantagescope;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.live.AdvantageScopeLive;
import org.allsparks.trace.live.AdvantageScopeLiveStats;
import org.allsparks.trace.live.AdvantageScopeMetricSink;

/**
 * Optional live TRACE publisher. Constructed by {@code Class.forName} from core
 * when streaming is enabled and this module is on the classpath.
 *
 * <p>Credit: ACME Robotics FTC Dashboard defined the WebSocket message types
 * AdvantageScope already speaks. This module implements a compatible subset
 * (status heartbeat + telemetry). It does not run DashboardCore or the
 * Dashboard UI.
 */
public final class AdvantageScopePublisher implements AdvantageScopeLive {
    private static final long METRIC_PERIOD_NANOS = 1_000_000_000L;

    private final TraceConfig config;
    private final AdvantageScopeMetricSink metrics;
    private final LatestValueMailbox mailbox;
    private final AdvantageScopeWebSocketServer server;
    private final Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger clientCount = new AtomicInteger();
    private final AtomicLong framesPrepared = new AtomicLong();
    private final AtomicLong framesSent = new AtomicLong();
    private final AtomicLong framesDropped = new AtomicLong();
    private final AtomicLong sendErrors = new AtomicLong();
    private final AtomicLong valuesSent = new AtomicLong();
    private final AtomicLong bytesSent = new AtomicLong();
    private final AtomicLong lastSendDurationMs = new AtomicLong();
    private final AtomicLong maxSendDurationMs = new AtomicLong();
    private final AtomicLong serializationDurationMs = new AtomicLong();
    private final StringBuilder jsonBuffer = new StringBuilder(4096);
    private volatile boolean listening;
    private volatile int listenPort = -1;
    private long lastPublishedSeq = -1L;
    private long lastMetricNanos;
    private long lastErrorMetricNanos;

    public AdvantageScopePublisher(TraceConfig config, AdvantageScopeMetricSink metrics) {
        this.config = config;
        this.metrics = metrics;
        this.mailbox = new LatestValueMailbox(config.advantageScopeMaxValues());
        this.server = new AdvantageScopeWebSocketServer(config.advantageScopePort(), config.opModeName());
        try {
            server.start(0, true);
            this.listenPort = server.getListeningPort();
            this.listening = listenPort > 0;
        } catch (IOException bindFailed) {
            this.listening = false;
            this.listenPort = -1;
            noteError("live listen failed: " + bindFailed.getClass().getSimpleName());
        }
        this.worker = new Thread(this::run, "trace-advantagescope");
        this.worker.setDaemon(true);
        this.worker.setPriority(Thread.MIN_PRIORITY);
        this.worker.start();
    }

    @Override
    public void offer(TraceRecord record) {
        mailbox.offer(record);
    }

    @Override
    public int listenPort() {
        return listenPort;
    }

    @Override
    public AdvantageScopeLiveStats stats() {
        clientCount.set(server.clientCount());
        return snapshot();
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        worker.interrupt();
        server.shutdown();
        listening = false;
        try {
            worker.join(500L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void run() {
        long periodMs = Math.max(1L, 1000L / config.advantageScopeRateHz());
        while (running.get()) {
            try {
                Thread.sleep(periodMs);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!running.get() || !listening) {
                continue;
            }
            int clients = server.clientCount();
            clientCount.set(clients);
            publishMetricsIfDue();
            if (clients == 0) {
                continue;
            }
            long seqNow = mailbox.sequence();
            if (lastPublishedSeq >= 0L && seqNow > lastPublishedSeq) {
                framesDropped.addAndGet(Math.max(0L, seqNow - lastPublishedSeq - 1L));
            }
            lastPublishedSeq = seqNow;
            framesPrepared.incrementAndGet();
            long serializeStart = System.nanoTime();
            String payload = AdvantageScopeJson.telemetry(mailbox, snapshot(), jsonBuffer);
            serializationDurationMs.set((System.nanoTime() - serializeStart) / 1_000_000L);
            valuesSent.set(mailbox.size());
            long sendStart = System.nanoTime();
            int sent;
            try {
                sent = server.broadcast(payload);
            } catch (RuntimeException failed) {
                sendErrors.incrementAndGet();
                noteError("live send failed: " + failed.getClass().getSimpleName());
                continue;
            }
            long sendMs = (System.nanoTime() - sendStart) / 1_000_000L;
            lastSendDurationMs.set(sendMs);
            if (sendMs > maxSendDurationMs.get()) {
                maxSendDurationMs.set(sendMs);
            }
            if (sent > 0) {
                framesSent.incrementAndGet();
                bytesSent.addAndGet((long) payload.getBytes(StandardCharsets.UTF_8).length * (long) sent);
            } else {
                sendErrors.incrementAndGet();
            }
        }
    }

    private AdvantageScopeLiveStats snapshot() {
        return new AdvantageScopeLiveStats(
                listening,
                listening,
                listenPort,
                config.advantageScopeRateHz(),
                clientCount.get(),
                framesPrepared.get(),
                framesSent.get(),
                framesDropped.get(),
                sendErrors.get(),
                valuesSent.get(),
                mailbox.valuesSkipped(),
                bytesSent.get(),
                server.reconnectCount(),
                lastSendDurationMs.get(),
                maxSendDurationMs.get(),
                serializationDurationMs.get());
    }

    private void publishMetricsIfDue() {
        long now = System.nanoTime();
        if (now - lastMetricNanos < METRIC_PERIOD_NANOS) {
            return;
        }
        lastMetricNanos = now;
        AdvantageScopeLiveStats stats = snapshot();
        metrics.record("TRACE/AdvantageScope/Enabled", stats.enabled());
        metrics.record("TRACE/AdvantageScope/ConfiguredRateHz", (long) stats.configuredRateHz(), Units.NONE);
        metrics.record("TRACE/AdvantageScope/ClientCount", (long) stats.clientCount(), Units.NONE);
        metrics.record("TRACE/AdvantageScope/FramesPrepared", stats.framesPrepared(), Units.NONE);
        metrics.record("TRACE/AdvantageScope/FramesSent", stats.framesSent(), Units.NONE);
        metrics.record("TRACE/AdvantageScope/FramesDropped", stats.framesDropped(), Units.NONE);
        metrics.record("TRACE/AdvantageScope/SendErrors", stats.sendErrors(), Units.NONE);
        metrics.record("TRACE/AdvantageScope/ValuesSent", stats.valuesSent(), Units.NONE);
        metrics.record("TRACE/AdvantageScope/ValuesSkipped", stats.valuesSkipped(), Units.NONE);
        metrics.record("TRACE/AdvantageScope/LastSendDurationMs", stats.lastSendDurationMs(), Units.MILLISECONDS);
        metrics.record("TRACE/AdvantageScope/MaxSendDurationMs", stats.maxSendDurationMs(), Units.MILLISECONDS);
        metrics.record("TRACE/AdvantageScope/SerializationDurationMs", stats.serializationDurationMs(), Units.MILLISECONDS);
        metrics.record("TRACE/AdvantageScope/BytesSent", stats.bytesSent(), Units.NONE);
        metrics.record("TRACE/AdvantageScope/ReconnectCount", stats.reconnectCount(), Units.NONE);
    }

    private void noteError(String message) {
        long now = System.nanoTime();
        if (now - lastErrorMetricNanos < METRIC_PERIOD_NANOS) {
            return;
        }
        lastErrorMetricNanos = now;
        metrics.record("TRACE/AdvantageScope/LastError", 1L, Units.NONE);
        // Keep the string off the hot path; the count is the durable signal.
        if (message != null) {
            metrics.record("TRACE/AdvantageScope/ErrorCount", sendErrors.get() + 1L, Units.NONE);
        }
    }
}
