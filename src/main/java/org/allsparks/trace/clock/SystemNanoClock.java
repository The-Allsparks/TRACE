package org.allsparks.trace.clock;

/**
 * Production clock backed by {@link System#nanoTime()} and wall-clock millis.
 */
public final class SystemNanoClock implements TraceClock {
    @Override
    public long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public long wallClockMillis() {
        return System.currentTimeMillis();
    }
}
