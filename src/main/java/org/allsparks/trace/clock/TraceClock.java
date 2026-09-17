package org.allsparks.trace.clock;

import org.allsparks.contracts.time.MonotonicClock;

/**
 * Authoritative TRACE time source. Units are nanoseconds since an arbitrary
 * origin. Production code should use a monotonic clock; tests may use a
 * manual clock.
 *
 * <p>Extends {@link MonotonicClock}: {@link #nowNanos()} returns the same
 * reading as {@link #nanoTime()}. {@link #wallClockMillis()} stays TRACE-local
 * and is not part of the shared contract.
 */
public interface TraceClock extends MonotonicClock {
    long nanoTime();

    @Override
    default long nowNanos() {
        return nanoTime();
    }

    default long wallClockMillis() {
        return System.currentTimeMillis();
    }
}
