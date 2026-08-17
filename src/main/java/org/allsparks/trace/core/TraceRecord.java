package org.allsparks.trace.core;

import java.util.Objects;

/**
 * Immutable recorded fact, decision, command, or event.
 */
public final class TraceRecord {
    private final long monotonicNanos;
    private final long wallClockMillis;
    private final long cycle;
    private final String source;
    private final SignalName name;
    private final RecordCategory category;
    private final TypedValue value;
    private final Units units;
    private final TraceQuality quality;
    private final TracePriority priority;
    private final int schemaVersion;
    private final TraceSeverity severity;
    private final String message;

    private TraceRecord(Builder builder) {
        this.monotonicNanos = builder.monotonicNanos;
        this.wallClockMillis = builder.wallClockMillis;
        this.cycle = builder.cycle;
        this.source = builder.source;
        this.name = builder.name;
        this.category = builder.category;
        this.value = builder.value;
        this.units = builder.units;
        this.quality = builder.quality;
        this.priority = builder.priority;
        this.schemaVersion = builder.schemaVersion;
        this.severity = builder.severity;
        this.message = builder.message;
    }

    public long monotonicNanos() {
        return monotonicNanos;
    }

    public long wallClockMillis() {
        return wallClockMillis;
    }

    public boolean hasWallClock() {
        return wallClockMillis > 0L;
    }

    public long cycle() {
        return cycle;
    }

    public String source() {
        return source;
    }

    public SignalName name() {
        return name;
    }

    public RecordCategory category() {
        return category;
    }

    public TypedValue value() {
        return value;
    }

    public Units units() {
        return units;
    }

    public TraceQuality quality() {
        return quality;
    }

    public TracePriority priority() {
        return priority;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public TraceSeverity severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long monotonicNanos;
        private long wallClockMillis;
        private long cycle;
        private String source = "TRACE";
        private SignalName name = SignalName.parse("TRACE/Unnamed");
        private RecordCategory category = RecordCategory.OUTPUT;
        private TypedValue value = TypedValue.none();
        private Units units = Units.NONE;
        private TraceQuality quality = TraceQuality.OK;
        private TracePriority priority = TracePriority.NORMAL;
        private int schemaVersion = SchemaVersion.RECORD;
        private TraceSeverity severity = TraceSeverity.INFO;
        private String message = "";

        public Builder monotonicNanos(long monotonicNanos) {
            this.monotonicNanos = monotonicNanos;
            return this;
        }

        public Builder wallClockMillis(long wallClockMillis) {
            this.wallClockMillis = wallClockMillis;
            return this;
        }

        public Builder cycle(long cycle) {
            this.cycle = cycle;
            return this;
        }

        public Builder source(String source) {
            this.source = Objects.requireNonNull(source, "source");
            return this;
        }

        public Builder name(String name) {
            this.name = SignalName.parse(name);
            return this;
        }

        public Builder name(SignalName name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        public Builder category(RecordCategory category) {
            this.category = Objects.requireNonNull(category, "category");
            return this;
        }

        public Builder value(TypedValue value) {
            this.value = Objects.requireNonNull(value, "value");
            return this;
        }

        public Builder units(Units units) {
            this.units = units == null ? Units.NONE : units;
            return this;
        }

        public Builder quality(TraceQuality quality) {
            this.quality = Objects.requireNonNull(quality, "quality");
            return this;
        }

        public Builder priority(TracePriority priority) {
            this.priority = Objects.requireNonNull(priority, "priority");
            return this;
        }

        public Builder schemaVersion(int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder severity(TraceSeverity severity) {
            this.severity = severity == null ? TraceSeverity.INFO : severity;
            return this;
        }

        public Builder message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        public TraceRecord build() {
            return new TraceRecord(this);
        }
    }
}
