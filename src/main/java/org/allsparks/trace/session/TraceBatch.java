package org.allsparks.trace.session;

import org.allsparks.trace.core.Units;

/**
 * Reusable packed list of named scalars for one {@link TraceCycle} bulk record.
 *
 * <p>Construct once (OpMode field). Each loop: {@link #clear()}, {@link #add(String,
 * double, Units)} for each channel, then {@link TraceCycle#recordOutputs(TraceBatch)}
 * or {@link TraceCycle#recordInputs(TraceBatch)}. Does not allocate after construction.
 *
 * <p>Extra {@link #add} past capacity is ignored (fail-open on the robot).
 *
 * <p>Optional {@code dsGroup} packs several channels into one Driver Station
 * line (add order) at {@link org.allsparks.trace.Trace#publishTelemetry()}. Null group means tlog only.
 */
public final class TraceBatch {
    private final String[] names;
    private final double[] values;
    private final Units[] units;
    private final String[] dsGroups;
    private int size;

    public TraceBatch(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0");
        }
        this.names = new String[capacity];
        this.values = new double[capacity];
        this.units = new Units[capacity];
        this.dsGroups = new String[capacity];
    }

    /**
     * Append one named scalar (tlog only). Returns {@code this} for chaining.
     * No-op when full.
     */
    public TraceBatch add(String name, double value, Units units) {
        return add(name, value, units, null);
    }

    /**
     * Append one named scalar and a Driver Station group.
     * {@code dsGroup} is the phone caption. Later adds with the same name
     * append columns, joined with {@code " / "}. Null/empty stays off the DS.
     */
    public TraceBatch add(String name, double value, Units units, String dsGroup) {
        if (size >= names.length) {
            return this;
        }
        names[size] = name;
        values[size] = value;
        this.units[size] = units;
        dsGroups[size] = dsGroup;
        size++;
        return this;
    }

    /** Drop every entry so this batch can be filled again this loop. */
    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return names.length;
    }

    String[] names() {
        return names;
    }

    double[] values() {
        return values;
    }

    Units[] units() {
        return units;
    }

    String[] dsGroups() {
        return dsGroups;
    }
}
