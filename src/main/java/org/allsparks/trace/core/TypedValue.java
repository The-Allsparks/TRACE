package org.allsparks.trace.core;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Typed payload for a TRACE record. Avoids untyped maps as the canonical
 * representation.
 */
public final class TypedValue {
    public enum Kind {
        BOOLEAN((byte) 1),
        LONG((byte) 2),
        DOUBLE((byte) 3),
        STRING((byte) 4),
        POSE2D((byte) 5),
        DOUBLE_ARRAY((byte) 6),
        NONE((byte) 7);

        private final byte wireId;

        Kind(byte wireId) {
            this.wireId = wireId;
        }

        public byte wireId() {
            return wireId;
        }

        public static Kind fromWire(byte id) {
            for (Kind kind : values()) {
                if (kind.wireId == id) {
                    return kind;
                }
            }
            throw new IllegalArgumentException("Unknown value kind: " + id);
        }
    }

    private final Kind kind;
    private final boolean booleanValue;
    private final long longValue;
    private final double doubleValue;
    private final String stringValue;
    private final Pose2d poseValue;
    private final double[] arrayValue;

    private TypedValue(
            Kind kind,
            boolean booleanValue,
            long longValue,
            double doubleValue,
            String stringValue,
            Pose2d poseValue,
            double[] arrayValue) {
        this.kind = kind;
        this.booleanValue = booleanValue;
        this.longValue = longValue;
        this.doubleValue = doubleValue;
        this.stringValue = stringValue;
        this.poseValue = poseValue;
        this.arrayValue = arrayValue;
    }

    public static TypedValue ofBoolean(boolean value) {
        return new TypedValue(Kind.BOOLEAN, value, 0L, 0.0, null, null, null);
    }

    public static TypedValue ofLong(long value) {
        return new TypedValue(Kind.LONG, false, value, 0.0, null, null, null);
    }

    public static TypedValue ofInt(int value) {
        return ofLong(value);
    }

    public static TypedValue ofDouble(double value) {
        return new TypedValue(Kind.DOUBLE, false, 0L, value, null, null, null);
    }

    public static TypedValue ofString(String value) {
        return new TypedValue(Kind.STRING, false, 0L, 0.0, Objects.requireNonNull(value, "value"), null, null);
    }

    public static TypedValue ofPose(Pose2d value) {
        return new TypedValue(Kind.POSE2D, false, 0L, 0.0, null, Objects.requireNonNull(value, "value"), null);
    }

    public static TypedValue ofDoubles(double... values) {
        return new TypedValue(Kind.DOUBLE_ARRAY, false, 0L, 0.0, null, null, Arrays.copyOf(values, values.length));
    }

    public static TypedValue none() {
        return new TypedValue(Kind.NONE, false, 0L, 0.0, null, null, null);
    }

    public Kind kind() {
        return kind;
    }

    public boolean asBoolean() {
        require(Kind.BOOLEAN);
        return booleanValue;
    }

    public long asLong() {
        require(Kind.LONG);
        return longValue;
    }

    public double asDouble() {
        require(Kind.DOUBLE);
        return doubleValue;
    }

    public String asString() {
        require(Kind.STRING);
        return stringValue;
    }

    public Pose2d asPose() {
        require(Kind.POSE2D);
        return poseValue;
    }

    public double[] asDoubleArray() {
        require(Kind.DOUBLE_ARRAY);
        return Arrays.copyOf(arrayValue, arrayValue.length);
    }

    public boolean approximatelyEquals(TypedValue other, double epsilon) {
        if (other == null || kind != other.kind) {
            return false;
        }
        switch (kind) {
            case BOOLEAN:
                return booleanValue == other.booleanValue;
            case LONG:
                return longValue == other.longValue;
            case DOUBLE:
                return Math.abs(doubleValue - other.doubleValue) <= epsilon;
            case STRING:
                return Objects.equals(stringValue, other.stringValue);
            case POSE2D:
                return poseValue.equals(other.poseValue);
            case DOUBLE_ARRAY:
                return Arrays.equals(arrayValue, other.arrayValue);
            case NONE:
                return true;
            default:
                return false;
        }
    }

    public String render() {
        switch (kind) {
            case BOOLEAN:
                return Boolean.toString(booleanValue);
            case LONG:
                return Long.toString(longValue);
            case DOUBLE:
                return String.format(Locale.ROOT, "%.6g", doubleValue);
            case STRING:
                return stringValue;
            case POSE2D:
                return poseValue.toString();
            case DOUBLE_ARRAY:
                return Arrays.toString(arrayValue);
            case NONE:
                return "";
            default:
                return "";
        }
    }

    private void require(Kind expected) {
        if (kind != expected) {
            throw new IllegalStateException("Value kind is " + kind + ", expected " + expected);
        }
    }

    @Override
    public String toString() {
        return kind + "(" + render() + ")";
    }
}
