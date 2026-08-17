package org.allsparks.trace.clock;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic clock for tests and future replay. Does not read hardware.
 */
public final class ManualClock implements TraceClock {
    private final AtomicLong nanos = new AtomicLong();
    private final AtomicLong wallMillis = new AtomicLong();

    public ManualClock() {
        this(0L, 0L);
    }

    public ManualClock(long nanos, long wallMillis) {
        this.nanos.set(nanos);
        this.wallMillis.set(wallMillis);
    }

    public void setNanos(long value) {
        nanos.set(value);
    }

    public void advanceNanos(long delta) {
        nanos.addAndGet(delta);
    }

    public void setWallClockMillis(long value) {
        wallMillis.set(value);
    }

    @Override
    public long nanoTime() {
        return nanos.get();
    }

    @Override
    public long wallClockMillis() {
        return wallMillis.get();
    }
}
