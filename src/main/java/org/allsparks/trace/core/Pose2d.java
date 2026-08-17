package org.allsparks.trace.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Field-relative 2D pose used by educational {@code Drive/Pose} records.
 * TRACE does not own localization; this is a typed payload only.
 */
public final class Pose2d {
    private final double x;
    private final double y;
    private final double headingRad;

    public Pose2d(double x, double y, double headingRad) {
        this.x = x;
        this.y = y;
        this.headingRad = headingRad;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double headingRad() {
        return headingRad;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Pose2d)) {
            return false;
        }
        Pose2d pose = (Pose2d) other;
        return Double.compare(pose.x, x) == 0
                && Double.compare(pose.y, y) == 0
                && Double.compare(pose.headingRad, headingRad) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, headingRad);
    }

    @Override
    public String toString() {
        return "Pose2d{x=" + x + ", y=" + y + ", headingRad=" + headingRad + "}";
    }

    public double[] toArray() {
        return new double[] {x, y, headingRad};
    }

    public static Pose2d fromArray(double[] values) {
        if (values == null || values.length != 3) {
            throw new IllegalArgumentException("Pose2d requires 3 values, got " + Arrays.toString(values));
        }
        return new Pose2d(values[0], values[1], values[2]);
    }
}
