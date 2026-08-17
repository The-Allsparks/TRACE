package org.allsparks.trace.clock;

/**
 * Authoritative TRACE time source. Units are nanoseconds since an arbitrary
 * origin. Production code should use a monotonic clock; tests may use a
 * manual clock.
 */
public interface TraceClock {
    long nanoTime();

    default long wallClockMillis() {
        return System.currentTimeMillis();
    }
}
