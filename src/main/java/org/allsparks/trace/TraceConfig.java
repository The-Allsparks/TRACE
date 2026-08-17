package org.allsparks.trace;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.allsparks.trace.clock.SystemNanoClock;
import org.allsparks.trace.clock.TraceClock;
import org.allsparks.trace.core.TracePriority;

/**
 * Explicit, reversible TRACE configuration. No advanced mode activates merely
 * because another Allsparks library is present.
 */
public final class TraceConfig {
    public static final int DEFAULT_QUEUE_CAPACITY = 4096;
    public static final int DEFAULT_MEMORY_CAPACITY = 2048;
    public static final long DEFAULT_MAX_FILE_BYTES = 8L * 1024L * 1024L;
    public static final long DEFAULT_MAX_TOTAL_BYTES = 32L * 1024L * 1024L;
    public static final long DEFAULT_BATCH_BYTES = 16L * 1024L;
    public static final long DEFAULT_ESSENTIAL_INTERVAL_NANOS = Duration.ofMillis(50).toNanos();

    private final TraceMode mode;
    private final TraceClock clock;
    private final boolean captureWallClock;
    private final int queueCapacity;
    private final int memoryCapacity;
    private final Path storageDirectory;
    private final long maxFileBytes;
    private final long maxTotalBytes;
    private final long batchBytes;
    private final long essentialSampleIntervalNanos;
    private final double changeThreshold;
    private final boolean changeBasedRecording;
    private final TracePriority minimumPriority;
    private final String robotName;
    private final String opModeName;
    private final Set<String> enabledIntegrations;
    private final Set<String> featureFlags;
    private final boolean consoleSink;
    private final boolean memorySink;
    private final boolean fileSink;
    private final Duration shutdownFlushTimeout;
    private final int rollingBufferSize;

    private TraceConfig(Builder builder) {
        this.mode = builder.mode;
        this.clock = builder.clock;
        this.captureWallClock = builder.captureWallClock;
        this.queueCapacity = builder.queueCapacity;
        this.memoryCapacity = builder.memoryCapacity;
        this.storageDirectory = builder.storageDirectory;
        this.maxFileBytes = builder.maxFileBytes;
        this.maxTotalBytes = builder.maxTotalBytes;
        this.batchBytes = builder.batchBytes;
        this.essentialSampleIntervalNanos = builder.essentialSampleIntervalNanos;
        this.changeThreshold = builder.changeThreshold;
        this.changeBasedRecording = builder.changeBasedRecording;
        this.minimumPriority = builder.minimumPriority;
        this.robotName = builder.robotName;
        this.opModeName = builder.opModeName;
        this.enabledIntegrations = Collections.unmodifiableSet(new LinkedHashSet<>(builder.enabledIntegrations));
        this.featureFlags = Collections.unmodifiableSet(new LinkedHashSet<>(builder.featureFlags));
        this.consoleSink = builder.consoleSink;
        this.memorySink = builder.memorySink;
        this.fileSink = builder.fileSink;
        this.shutdownFlushTimeout = builder.shutdownFlushTimeout;
        this.rollingBufferSize = builder.rollingBufferSize;
    }

    public static TraceConfig off() {
        return builder().mode(TraceMode.OFF).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public TraceMode mode() {
        return mode;
    }

    public TraceClock clock() {
        return clock;
    }

    public boolean captureWallClock() {
        return captureWallClock;
    }

    public int queueCapacity() {
        return queueCapacity;
    }

    public int memoryCapacity() {
        return memoryCapacity;
    }

    public Path storageDirectory() {
        return storageDirectory;
    }

    public long maxFileBytes() {
        return maxFileBytes;
    }

    public long maxTotalBytes() {
        return maxTotalBytes;
    }

    public long batchBytes() {
        return batchBytes;
    }

    public long essentialSampleIntervalNanos() {
        return essentialSampleIntervalNanos;
    }

    public double changeThreshold() {
        return changeThreshold;
    }

    public boolean changeBasedRecording() {
        return changeBasedRecording;
    }

    public TracePriority minimumPriority() {
        return minimumPriority;
    }

    public String robotName() {
        return robotName;
    }

    public String opModeName() {
        return opModeName;
    }

    public Set<String> enabledIntegrations() {
        return enabledIntegrations;
    }

    public Set<String> featureFlags() {
        return featureFlags;
    }

    public boolean consoleSink() {
        return consoleSink;
    }

    public boolean memorySink() {
        return memorySink;
    }

    public boolean fileSink() {
        return fileSink;
    }

    public Duration shutdownFlushTimeout() {
        return shutdownFlushTimeout;
    }

    public int rollingBufferSize() {
        return rollingBufferSize;
    }

    public boolean isEnabled() {
        return mode != TraceMode.OFF;
    }

    public static final class Builder {
        private TraceMode mode = TraceMode.OFF;
        private TraceClock clock = new SystemNanoClock();
        private boolean captureWallClock = true;
        private int queueCapacity = DEFAULT_QUEUE_CAPACITY;
        private int memoryCapacity = DEFAULT_MEMORY_CAPACITY;
        private Path storageDirectory;
        private long maxFileBytes = DEFAULT_MAX_FILE_BYTES;
        private long maxTotalBytes = DEFAULT_MAX_TOTAL_BYTES;
        private long batchBytes = DEFAULT_BATCH_BYTES;
        private long essentialSampleIntervalNanos = DEFAULT_ESSENTIAL_INTERVAL_NANOS;
        private double changeThreshold = 0.0;
        private boolean changeBasedRecording = false;
        private TracePriority minimumPriority = TracePriority.VERBOSE;
        private String robotName = "unknown";
        private String opModeName = "unknown";
        private final Set<String> enabledIntegrations = new LinkedHashSet<>();
        private final Set<String> featureFlags = new LinkedHashSet<>();
        private boolean consoleSink = false;
        private boolean memorySink = true;
        private boolean fileSink = false;
        private Duration shutdownFlushTimeout = Duration.ofMillis(250);
        private int rollingBufferSize = 128;

        public Builder mode(TraceMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            if (mode == TraceMode.REPLAY) {
                throw new IllegalArgumentException(
                        "REPLAY mode is not implemented in Phases 0–3 and cannot be enabled");
            }
            return this;
        }

        public Builder clock(TraceClock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        public Builder captureWallClock(boolean captureWallClock) {
            this.captureWallClock = captureWallClock;
            return this;
        }

        public Builder queueCapacity(int queueCapacity) {
            if (queueCapacity < 1) {
                throw new IllegalArgumentException("queueCapacity must be positive");
            }
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder memoryCapacity(int memoryCapacity) {
            if (memoryCapacity < 1) {
                throw new IllegalArgumentException("memoryCapacity must be positive");
            }
            this.memoryCapacity = memoryCapacity;
            return this;
        }

        public Builder storageDirectory(Path storageDirectory) {
            this.storageDirectory = storageDirectory;
            return this;
        }

        public Builder maxFileBytes(long maxFileBytes) {
            if (maxFileBytes < 1024) {
                throw new IllegalArgumentException("maxFileBytes must be at least 1024");
            }
            this.maxFileBytes = maxFileBytes;
            return this;
        }

        public Builder maxTotalBytes(long maxTotalBytes) {
            if (maxTotalBytes < 1024) {
                throw new IllegalArgumentException("maxTotalBytes must be at least 1024");
            }
            this.maxTotalBytes = maxTotalBytes;
            return this;
        }

        public Builder batchBytes(long batchBytes) {
            if (batchBytes < 256) {
                throw new IllegalArgumentException("batchBytes must be at least 256");
            }
            this.batchBytes = batchBytes;
            return this;
        }

        public Builder essentialSampleIntervalNanos(long essentialSampleIntervalNanos) {
            if (essentialSampleIntervalNanos < 0) {
                throw new IllegalArgumentException("interval must be non-negative");
            }
            this.essentialSampleIntervalNanos = essentialSampleIntervalNanos;
            return this;
        }

        public Builder changeThreshold(double changeThreshold) {
            this.changeThreshold = changeThreshold;
            return this;
        }

        public Builder changeBasedRecording(boolean changeBasedRecording) {
            this.changeBasedRecording = changeBasedRecording;
            return this;
        }

        public Builder minimumPriority(TracePriority minimumPriority) {
            this.minimumPriority = Objects.requireNonNull(minimumPriority, "minimumPriority");
            return this;
        }

        public Builder robotName(String robotName) {
            this.robotName = robotName == null ? "unknown" : robotName;
            return this;
        }

        public Builder opModeName(String opModeName) {
            this.opModeName = opModeName == null ? "unknown" : opModeName;
            return this;
        }

        public Builder enableIntegration(String name) {
            if (name != null && !name.isEmpty()) {
                enabledIntegrations.add(name);
            }
            return this;
        }

        public Builder featureFlag(String name) {
            if (name != null && !name.isEmpty()) {
                featureFlags.add(name);
            }
            return this;
        }

        public Builder consoleSink(boolean consoleSink) {
            this.consoleSink = consoleSink;
            return this;
        }

        public Builder memorySink(boolean memorySink) {
            this.memorySink = memorySink;
            return this;
        }

        public Builder fileSink(boolean fileSink) {
            this.fileSink = fileSink;
            return this;
        }

        public Builder shutdownFlushTimeout(Duration shutdownFlushTimeout) {
            this.shutdownFlushTimeout = Objects.requireNonNull(shutdownFlushTimeout, "shutdownFlushTimeout");
            return this;
        }

        public Builder rollingBufferSize(int rollingBufferSize) {
            if (rollingBufferSize < 1) {
                throw new IllegalArgumentException("rollingBufferSize must be positive");
            }
            this.rollingBufferSize = rollingBufferSize;
            return this;
        }

        public TraceConfig build() {
            if (fileSink && storageDirectory == null) {
                throw new IllegalArgumentException("fileSink requires storageDirectory");
            }
            if (maxTotalBytes < maxFileBytes) {
                throw new IllegalArgumentException("maxTotalBytes must be >= maxFileBytes");
            }
            return new TraceConfig(this);
        }
    }
}
