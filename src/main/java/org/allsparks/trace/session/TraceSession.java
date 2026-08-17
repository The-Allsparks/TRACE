package org.allsparks.trace.session;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.clock.TraceClock;
import org.allsparks.trace.core.DropReason;
import org.allsparks.trace.core.DroppedRecordStats;
import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceQuality;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TraceSeverity;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.export.CsvExporter;
import org.allsparks.trace.export.HumanReadableExporter;
import org.allsparks.trace.ftc.FtcTelemetryAdapter;
import org.allsparks.trace.policy.SamplingPolicy;
import org.allsparks.trace.sink.BoundedMemorySink;
import org.allsparks.trace.sink.CompositeSink;
import org.allsparks.trace.sink.ConsoleSink;
import org.allsparks.trace.sink.NoOpSink;
import org.allsparks.trace.sink.TraceSink;
import org.allsparks.trace.storage.AsyncBoundedWriter;

/**
 * Per-OpMode TRACE session. Observes and records; never commands motors,
 * servos, or mechanism states.
 */
public final class TraceSession implements AutoCloseable {
    public static final long DEFAULT_LOOP_BUDGET_NANOS = 30_000_000L;

    private final TraceConfig config;
    private final SessionMetadata metadata;
    private final TraceClock clock;
    private final DroppedRecordStats drops = new DroppedRecordStats();
    private final SamplingPolicy sampling;
    private final BoundedMemorySink memorySink;
    private final AsyncBoundedWriter fileWriter;
    private final TraceSink sink;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final AtomicLong cycle = new AtomicLong();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong lastLoopDuration = new AtomicLong();
    private final AtomicBoolean loopOverrun = new AtomicBoolean();
    private final long loopBudgetNanos;
    private volatile FtcTelemetryAdapter telemetryAdapter;
    private TraceCycle currentCycle;

    public TraceSession(TraceConfig config) {
        this(config, SessionMetadata.collect(config), DEFAULT_LOOP_BUDGET_NANOS);
    }

    public TraceSession(TraceConfig config, SessionMetadata metadata) {
        this(config, metadata, DEFAULT_LOOP_BUDGET_NANOS);
    }

    public TraceSession(TraceConfig config, SessionMetadata metadata, long loopBudgetNanos) {
        this.config = Objects.requireNonNull(config, "config");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.clock = config.clock();
        this.loopBudgetNanos = loopBudgetNanos;
        this.sampling = new SamplingPolicy(
                config.essentialSampleIntervalNanos(), config.changeThreshold(), config.changeBasedRecording());
        this.memorySink = config.memorySink() ? new BoundedMemorySink(config.memoryCapacity(), drops) : null;
        this.fileWriter = config.fileSink() ? new AsyncBoundedWriter(config, metadata, drops) : null;
        List<TraceSink> sinks = new ArrayList<>();
        if (config.consoleSink()) {
            sinks.add(new ConsoleSink());
        }
        if (memorySink != null) {
            sinks.add(memorySink);
        }
        if (fileWriter != null) {
            sinks.add(fileWriter);
        }
        this.sink = sinks.isEmpty() ? NoOpSink.INSTANCE : new CompositeSink(sinks);
    }

    public TraceConfig config() {
        return config;
    }

    public SessionMetadata metadata() {
        return metadata;
    }

    public boolean isOpen() {
        return open.get();
    }

    public long currentCycleNumber() {
        return cycle.get();
    }

    public void setTelemetryAdapter(FtcTelemetryAdapter telemetryAdapter) {
        this.telemetryAdapter = telemetryAdapter;
    }

    public TraceCycle beginCycle() {
        if (!open.get() || !config.isEnabled()) {
            return new TraceCycle(this, cycle.get(), clock.nanoTime());
        }
        long number = cycle.incrementAndGet();
        long start = clock.nanoTime();
        currentCycle = new TraceCycle(this, number, start);
        event("TRACE/Loop/Begin", "cycle " + number, TraceSeverity.INFO, TracePriority.DEBUG);
        return currentCycle;
    }

    void endCycle(TraceCycle ending) {
        long duration = clock.nanoTime() - ending.startNanos();
        lastLoopDuration.set(duration);
        boolean overrun = duration > loopBudgetNanos;
        loopOverrun.set(overrun);
        if (!config.isEnabled()) {
            return;
        }
        record(
                RecordCategory.OUTPUT,
                "TRACE/Loop/Duration",
                TypedValue.ofLong(duration),
                Units.NANOSECONDS,
                TracePriority.DEBUG,
                TraceQuality.OK,
                "");
        if (overrun) {
            event("TRACE/Loop/Overrun", "loop duration " + duration + " ns", TraceSeverity.WARNING, TracePriority.HIGH);
        }
        FtcTelemetryAdapter adapter = telemetryAdapter;
        if (adapter != null) {
            adapter.publish("TRACE/Health/Dropped", drops.total());
            adapter.publish("TRACE/Loop/DurationNs", duration);
        }
    }

    public void event(String message) {
        event("TRACE/Event", message, TraceSeverity.INFO, TracePriority.HIGH);
    }

    public void event(String name, String message, TraceSeverity severity, TracePriority priority) {
        if (!config.isEnabled() || !config.mode().recordsEvents()) {
            return;
        }
        TraceRecord record = baseBuilder(RecordCategory.EVENT, name, TypedValue.ofString(message), Units.NONE, priority, TraceQuality.OK)
                .severity(severity)
                .message(message)
                .build();
        publish(record);
    }

    public void recordException(Throwable throwable) {
        String message = throwable == null ? "unknown" : throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        event("TRACE/Exception", message, TraceSeverity.ERROR, TracePriority.CRITICAL);
    }

    public void record(String name, double value, Units units) {
        record(RecordCategory.OUTPUT, name, TypedValue.ofDouble(value), units, TracePriority.NORMAL, TraceQuality.OK, "");
    }

    public void record(String name, Pose2d pose) {
        record(RecordCategory.OUTPUT, name, TypedValue.ofPose(pose), Units.METERS, TracePriority.NORMAL, TraceQuality.OK, "");
    }

    public void recordInput(String name, double value, Units units) {
        record(RecordCategory.INPUT, name, TypedValue.ofDouble(value), units, TracePriority.HIGH, TraceQuality.OK, "");
    }

    public void record(
            RecordCategory category,
            String name,
            TypedValue value,
            Units units,
            TracePriority priority,
            TraceQuality quality,
            String message) {
        if (!config.isEnabled()) {
            return;
        }
        if (!allows(category, priority)) {
            drops.record(category, DropReason.MODE_FILTERED, 1);
            return;
        }
        TraceRecord record = baseBuilder(category, name, value, units, priority, quality).message(message).build();
        if (!sampling.shouldRecord(record)) {
            drops.record(category, DropReason.SAMPLE_SKIPPED, 1);
            return;
        }
        publish(record);
    }

    public List<TraceRecord> recorded() {
        return memorySink == null ? List.of() : memorySink.snapshot();
    }

    public String exportHumanReadable() {
        HumanReadableExporter exporter = new HumanReadableExporter();
        StringBuilder builder = new StringBuilder();
        for (TraceRecord record : recorded()) {
            builder.append(exporter.format(record)).append('\n');
        }
        return builder.toString();
    }

    public String exportCsv() {
        return new CsvExporter().export(recorded());
    }

    public String exportAdvantageScopeCsv() {
        return new CsvExporter().exportAdvantageScopeList(recorded());
    }

    public TraceHealth health() {
        boolean failed = fileWriter != null && fileWriter.writerFailed();
        boolean exhausted = fileWriter != null && fileWriter.storageExhausted();
        long water = fileWriter == null ? (memorySink == null ? 0L : memorySink.highWaterMark()) : fileWriter.highWaterMark();
        long bytes = fileWriter == null ? 0L : fileWriter.bytesWritten();
        return new TraceHealth(
                config.mode(),
                config.isEnabled() && open.get(),
                failed,
                exhausted,
                accepted.get(),
                drops.total(),
                cycle.get(),
                water,
                bytes,
                drops.snapshotByReason(),
                drops.snapshotByCategory(),
                lastLoopDuration.get(),
                loopOverrun.get());
    }

    public Path recordingFile() {
        return fileWriter == null ? null : fileWriter.currentFile();
    }

    public DroppedRecordStats drops() {
        return drops;
    }

    public void onOpModeInit() {
        event("TRACE/OpMode/Init", "initialized " + config.opModeName(), TraceSeverity.NOTICE, TracePriority.HIGH);
    }

    public void onOpModeStart() {
        event("TRACE/OpMode/Start", "started " + config.opModeName(), TraceSeverity.NOTICE, TracePriority.CRITICAL);
    }

    public void onOpModeStop() {
        event("TRACE/OpMode/Stop", "stopped " + config.opModeName(), TraceSeverity.NOTICE, TracePriority.CRITICAL);
        close();
    }

    @Override
    public void close() {
        if (!open.get()) {
            return;
        }
        event("TRACE/Session/Stop", "session finalized", TraceSeverity.NOTICE, TracePriority.HIGH);
        if (!open.compareAndSet(true, false)) {
            return;
        }
        sink.flush();
        sink.close();
    }

    private boolean allows(RecordCategory category, TracePriority priority) {
        if (!open.get() || !config.isEnabled()) {
            return false;
        }
        if (priority.rank() > config.minimumPriority().rank()) {
            return false;
        }
        TraceMode mode = config.mode();
        if (category == RecordCategory.EVENT || category == RecordCategory.DROP) {
            return mode.recordsEvents();
        }
        return mode.recordsSignals();
    }

    private TraceRecord.Builder baseBuilder(
            RecordCategory category,
            String name,
            TypedValue value,
            Units units,
            TracePriority priority,
            TraceQuality quality) {
        long now = clock.nanoTime();
        return TraceRecord.builder()
                .monotonicNanos(now)
                .wallClockMillis(config.captureWallClock() ? clock.wallClockMillis() : 0L)
                .cycle(cycle.get())
                .source(inferSource(name))
                .name(name)
                .category(category)
                .value(value)
                .units(units)
                .priority(priority)
                .quality(quality)
                .schemaVersion(org.allsparks.trace.core.SchemaVersion.RECORD);
    }

    private void publish(TraceRecord record) {
        accepted.incrementAndGet();
        sink.accept(record);
        FtcTelemetryAdapter adapter = telemetryAdapter;
        if (adapter != null && record.category() != RecordCategory.DROP) {
            adapter.publish(record.name().value(), record.value().render());
        }
    }

    private static String inferSource(String name) {
        int slash = name.indexOf('/');
        return slash <= 0 ? "TRACE" : name.substring(0, slash);
    }
}
