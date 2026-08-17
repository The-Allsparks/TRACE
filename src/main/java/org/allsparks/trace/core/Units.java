package org.allsparks.trace.core;

/**
 * Unit metadata attached to a signal. Unknown or dimensionless values use
 * {@link #NONE}. Schema evolution must not silently change the meaning of a
 * unit already written to a log.
 */
public final class Units {
    public static final Units NONE = new Units("none");
    public static final Units VOLTS = new Units("V");
    public static final Units AMPERES = new Units("A");
    public static final Units WATTS = new Units("W");
    public static final Units SECONDS = new Units("s");
    public static final Units MILLISECONDS = new Units("ms");
    public static final Units NANOSECONDS = new Units("ns");
    public static final Units METERS = new Units("m");
    public static final Units INCHES = new Units("in");
    public static final Units RADIANS = new Units("rad");
    public static final Units DEGREES = new Units("deg");
    public static final Units METERS_PER_SECOND = new Units("m/s");
    public static final Units RADIANS_PER_SECOND = new Units("rad/s");
    public static final Units COUNTS = new Units("counts");
    public static final Units PERCENT = new Units("%");
    public static final Units CELSIUS = new Units("C");
    public static final Units DIMENSIONLESS = NONE;

    private final String symbol;

    private Units(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static Units of(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return NONE;
        }
        return new Units(symbol);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Units)) {
            return false;
        }
        return symbol.equals(((Units) other).symbol);
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }

    @Override
    public String toString() {
        return symbol;
    }
}
