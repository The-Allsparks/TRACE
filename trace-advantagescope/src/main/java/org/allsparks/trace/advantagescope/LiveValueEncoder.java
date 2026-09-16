package org.allsparks.trace.advantagescope;

import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;

/**
 * Maps TRACE values onto the FTC Dashboard-compatible strings AdvantageScope
 * already understands. Pose x/y are inches; heading is radians. TRACE {@code .tlog}
 * Pose2d stays meters + headingRad.
 *
 * <p>Verified against AdvantageScope {@code FTCDashboardSource}: keys ending in
 * {@code " x"}, {@code " y"}, {@code " heading"} are grouped into a field pose.
 */
final class LiveValueEncoder {
    /** Matches AdvantageScope's inches-to-meters divisor for live FTC poses. */
    static final double INCHES_PER_METER = 39.37008;

    private LiveValueEncoder() {}

    static String scalar(TypedValue value) {
        switch (value.kind()) {
            case BOOLEAN:
                return value.asBoolean() ? "true" : "false";
            case LONG:
                return Long.toString(value.asLong());
            case DOUBLE:
                return Double.toString(value.asDouble());
            case STRING:
                return value.asString();
            default:
                return null;
        }
    }

    static double toInches(double coordinate, Units units) {
        if (Units.INCHES.equals(units)) {
            return coordinate;
        }
        if (Units.CENTIMETERS.equals(units)) {
            return coordinate / 2.54;
        }
        return coordinate * INCHES_PER_METER;
    }

    static Pose2d pose(TypedValue value) {
        return value.kind() == TypedValue.Kind.POSE2D ? value.asPose() : null;
    }
}
